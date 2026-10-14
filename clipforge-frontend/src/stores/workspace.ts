import { defineStore } from 'pinia'
import { DEMO } from '@/utils/demo'
import type { ConversationResponse, KnowledgeBaseResponse, MaterialResponse, ProjectResponse } from '@/types/domain'
import type {
  CreationOutputType,
  CreationOutputView,
  CreationPlanResult,
  ModelCallRecordView,
  TaskStatusResponse,
  TraceEvent
} from '@/types/api'
import type { QueryQualityReportResponse } from '@/types/domain'

const STORAGE_KEY = 'clipforge-workspace-state'

interface WorkspaceSelection {
  projectId?: string
  conversationId?: string
  knowledgeBaseIds: string[]
  materialIds: string[]
  runIds: string[]
}

interface WorkspaceState {
  apiBaseUrl: string
  demoMode: boolean
  projects: ProjectResponse[]
  conversations: ConversationResponse[]
  knowledgeBases: KnowledgeBaseResponse[]
  materials: MaterialResponse[]
  selectedProjectId?: string
  selectedConversationId?: string
  selectedKnowledgeBaseIds: string[]
  selectedMaterialIds: string[]
  currentRunId?: string
  runIds: string[]
  currentOutputType?: CreationOutputType
  currentPlan?: CreationPlanResult
  currentOutput?: CreationOutputView
  traceEvents: TraceEvent[]
  modelCallRecords: ModelCallRecordView[]
  qualityReport?: QueryQualityReportResponse
  lastTaskStatus?: TaskStatusResponse
  lastError?: string
}

function readStorage(): WorkspaceSelection | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as WorkspaceSelection
  } catch {
    return null
  }
}

function writeStorage(state: WorkspaceSelection) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

function isUserVisibleMaterial(material?: MaterialResponse) {
  if (!material) return false
  if (material.materialId?.startsWith('material_frame_')) return false
  if (material.originalFilename?.startsWith('视频帧')) return false
  return true
}

export const useWorkspaceStore = defineStore('workspace', {
  state: (): WorkspaceState => ({
    apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '',
    demoMode: true,
    projects: [],
    conversations: [],
    knowledgeBases: [],
    materials: [],
    selectedKnowledgeBaseIds: [],
    selectedMaterialIds: [],
    runIds: [],
    traceEvents: [],
    modelCallRecords: []
  }),
  actions: {
    hydrateSelection() {
      const saved = readStorage()
      if (!saved) return
      this.selectedProjectId = saved.projectId
      this.selectedConversationId = saved.conversationId
      this.selectedKnowledgeBaseIds = saved.knowledgeBaseIds || []
      this.selectedMaterialIds = saved.materialIds || []
      this.runIds = saved.runIds || []
      this.currentRunId = this.runIds[0]
    },
    persistSelection() {
      writeStorage({
        projectId: this.selectedProjectId,
        conversationId: this.selectedConversationId,
        knowledgeBaseIds: this.selectedKnowledgeBaseIds,
        materialIds: this.selectedMaterialIds,
        runIds: this.runIds
      })
    },
    setProjects(projects: ProjectResponse[]) {
      this.projects = projects
    },
    setConversations(conversations: ConversationResponse[]) {
      this.conversations = conversations
    },
    setKnowledgeBases(knowledgeBases: KnowledgeBaseResponse[]) {
      this.knowledgeBases = knowledgeBases
    },
    setMaterials(materials: MaterialResponse[]) {
      this.materials = (materials || []).filter((item) => isUserVisibleMaterial(item))
    },
    setActiveProject(projectId?: string) {
      this.selectedProjectId = projectId
      this.persistSelection()
    },
    setActiveConversation(conversationId?: string) {
      this.selectedConversationId = conversationId
      this.persistSelection()
    },
    setSelectedKnowledgeBaseIds(ids: string[]) {
      this.selectedKnowledgeBaseIds = ids
      this.persistSelection()
    },
    setSelectedMaterialIds(ids: string[]) {
      this.selectedMaterialIds = ids
      this.persistSelection()
    },
    setCurrentRun(runId?: string) {
      this.currentRunId = runId
      if (runId && !this.runIds.includes(runId)) {
        this.runIds = [runId, ...this.runIds].slice(0, 20)
        this.persistSelection()
      }
    },
    setCurrentPlan(plan?: CreationPlanResult) {
      this.currentPlan = plan
    },
    setCurrentOutput(output?: CreationOutputView, outputType?: CreationOutputType) {
      this.currentOutput = output
      this.currentOutputType = outputType || output?.outputType
    },
    setTraceEvents(events: TraceEvent[]) {
      this.traceEvents = events
    },
    setModelCallRecords(records: ModelCallRecordView[]) {
      this.modelCallRecords = records
    },
    setQualityReport(report?: QueryQualityReportResponse) {
      this.qualityReport = report
    },
    setLastTaskStatus(status?: TaskStatusResponse) {
      this.lastTaskStatus = status
    },
    setLastError(error?: string) {
      this.lastError = error
    },
    seedDemoDefaults() {
      const demoKnowledge = this.knowledgeBases.find((item) => item.name === DEMO.knowledgeBaseName) || this.knowledgeBases[0]
      this.selectedKnowledgeBaseIds = demoKnowledge?.knowledgeBaseId ? [demoKnowledge.knowledgeBaseId] : []
      this.persistSelection()
    }
  }
})
