import { DEMO, DEFAULT_PROJECT_DESCRIPTION } from '@/utils/demo'
import { fetchPublicFileAsFile } from '@/utils/files'
import { createKnowledgeBase, asyncProcessKnowledgeDocument, listKnowledgeBases, listKnowledgeDocuments, uploadKnowledgeDocument } from '@/api/knowledge'
import { listMaterials, uploadImage, uploadVideo } from '@/api/material'
import { createConversation, createProject, listProjectConversations, listProjects } from '@/api/project'
import { startMaterialTaskPolling } from '@/services/materialTaskPolling'
import type { KnowledgeBaseResponse, MaterialResponse } from '@/types/domain'
import type { useWorkspaceStore } from '@/stores/workspace'

type WorkspaceStore = ReturnType<typeof useWorkspaceStore>

export interface WorkspaceLoadResult {
  projectCreated: boolean
  conversationCreated: boolean
  knowledgeBaseCreated: boolean
  knowledgeDocumentUploaded: boolean
  materialUploadCount: number
}

export async function loadWorkspace(store: WorkspaceStore) {
  store.hydrateSelection()

  const projects = await listProjects(DEMO.userId).catch(() => [])
  store.setProjects(projects)

  const selectedProject =
    projects.find((item) => item.projectId === store.selectedProjectId) ||
    projects.find((item) => item.name === DEMO.projectName) ||
    projects[0]

  store.setActiveProject(selectedProject?.projectId)

  if (selectedProject?.projectId) {
    const conversations = await listProjectConversations(selectedProject.projectId).catch(() => [])
    store.setConversations(conversations)
    const selectedConversation =
      conversations.find((item) => item.conversationId === store.selectedConversationId) ||
      conversations.find((item) => item.title === DEMO.conversationTitle) ||
      conversations[0]
    store.setActiveConversation(selectedConversation?.conversationId)
  } else {
    store.setConversations([])
    store.setActiveConversation(undefined)
  }

  const knowledgeBases = await listKnowledgeBases(DEMO.userId).catch(() => [])
  store.setKnowledgeBases(knowledgeBases)
  syncKnowledgeSelection(store, knowledgeBases)

  await refreshMaterials(store)
}

export async function ensureDemoWorkspace(store: WorkspaceStore): Promise<WorkspaceLoadResult> {
  const result: WorkspaceLoadResult = {
    projectCreated: false,
    conversationCreated: false,
    knowledgeBaseCreated: false,
    knowledgeDocumentUploaded: false,
    materialUploadCount: 0
  }

  const projects = await listProjects(DEMO.userId).catch(() => [])
  let project = projects.find((item) => item.name === DEMO.projectName)
  if (!project) {
    project = await createProject({
      userId: DEMO.userId,
      name: DEMO.projectName,
      description: DEFAULT_PROJECT_DESCRIPTION
    })
    result.projectCreated = true
  }
  store.setActiveProject(project.projectId)

  const conversations = project.projectId ? await listProjectConversations(project.projectId).catch(() => []) : []
  let conversation = conversations.find((item) => item.title === DEMO.conversationTitle)
  if (!conversation && project.projectId) {
    conversation = await createConversation(project.projectId, {
      userId: DEMO.userId,
      title: DEMO.conversationTitle
    })
    result.conversationCreated = true
  }
  store.setActiveConversation(conversation?.conversationId)

  const knowledgeBases = await listKnowledgeBases(DEMO.userId).catch(() => [])
  let knowledgeBase = knowledgeBases.find((item) => item.name === DEMO.knowledgeBaseName)
  if (!knowledgeBase) {
    knowledgeBase = await createKnowledgeBase({
      userId: DEMO.userId,
      name: DEMO.knowledgeBaseName,
      description: '虎鲸科普创作使用的检索增强生成知识库。',
      category: 'ANIMAL_SCIENCE',
      defaultPlatform: DEMO.platform,
      chunkStrategy: 'RECURSIVE'
    })
    result.knowledgeBaseCreated = true
  }

  if (knowledgeBase?.knowledgeBaseId) {
    store.setSelectedKnowledgeBaseIds([knowledgeBase.knowledgeBaseId])
    const documents = await listKnowledgeDocuments(knowledgeBase.knowledgeBaseId).catch(() => [])
    const hasDemoDocument = documents.some((item) => item.originalFilename === DEMO.knowledgeDocumentName)
    if (!hasDemoDocument) {
      const file = await fetchPublicFileAsFile(DEMO.demoKnowledgeFile.url, DEMO.demoKnowledgeFile.name)
      const document = await uploadKnowledgeDocument(knowledgeBase.knowledgeBaseId, file, DEMO.userId)
      result.knowledgeDocumentUploaded = true
      if (document.documentId) {
        await asyncProcessKnowledgeDocument(document.documentId).catch(() => undefined)
      }
    }
  }

  const existingMaterials = await listMaterials({
    userId: DEMO.userId,
    projectId: project.projectId,
    conversationId: conversation?.conversationId
  }).catch(() => [])

  const existingNames = new Set(existingMaterials.map((item) => item.originalFilename))
  const batchId = `upload_batch_${crypto.randomUUID().replaceAll('-', '')}`
  const uploaded: MaterialResponse[] = []

  for (const image of DEMO.demoImageFiles) {
    if (existingNames.has(image.name)) continue
    const file = await fetchPublicFileAsFile(image.url, image.name)
    uploaded.push(await uploadImage({
      file,
      userId: DEMO.userId,
      projectId: project.projectId,
      conversationId: conversation?.conversationId,
      uploadBatchId: batchId
    }))
  }

  if (!existingNames.has(DEMO.demoVideoFile.name)) {
    const file = await fetchPublicFileAsFile(DEMO.demoVideoFile.url, DEMO.demoVideoFile.name)
    uploaded.push(await uploadVideo({
      file,
      userId: DEMO.userId,
      projectId: project.projectId,
      conversationId: conversation?.conversationId,
      uploadBatchId: batchId
    }))
  }

  result.materialUploadCount = uploaded.length
  await loadWorkspace(store)
  store.setSelectedMaterialIds(store.materials.map((item) => item.materialId || '').filter(Boolean))
  return result
}

export async function refreshMaterials(store: WorkspaceStore) {
  const materials = await listMaterials({
    userId: DEMO.userId,
    projectId: store.selectedProjectId,
    conversationId: store.selectedConversationId
  }).catch(() => [])
  store.setMaterials(materials)
  const stillAvailable = store.selectedMaterialIds.filter((id) => materials.some((item) => item.materialId === id))
  store.setSelectedMaterialIds(stillAvailable.length ? stillAvailable : materials.map((item) => item.materialId || '').filter(Boolean))
  startMaterialTaskPolling(store, {
    onSettled: () => refreshMaterials(store)
  })
}

function syncKnowledgeSelection(store: WorkspaceStore, knowledgeBases: KnowledgeBaseResponse[]) {
  const selected = store.selectedKnowledgeBaseIds.filter((id) => knowledgeBases.some((item) => item.knowledgeBaseId === id))
  if (selected.length) {
    store.setSelectedKnowledgeBaseIds(selected)
    return
  }
  const demoKnowledge = knowledgeBases.find((item) => item.name === DEMO.knowledgeBaseName) || knowledgeBases[0]
  store.setSelectedKnowledgeBaseIds(demoKnowledge?.knowledgeBaseId ? [demoKnowledge.knowledgeBaseId] : [])
}
