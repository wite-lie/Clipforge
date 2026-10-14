<template>
  <div class="studio-layout">
    <aside class="project-panel cf-panel">
      <div class="cf-panel-header">
        <div>
          <h3 class="cf-title">项目会话</h3>
          <p class="cf-subtitle">先选项目，再进入对应会话</p>
        </div>
      </div>

      <div class="project-actions">
        <el-button type="primary" :icon="Plus" @click="showProjectDialog = true">新建项目</el-button>
      </div>

      <el-scrollbar class="project-scroll">
        <div v-if="!store.projects.length" class="cf-empty">暂无项目，请先在首页创建演示项目或手动新建。</div>
        <div v-for="project in store.projects" :key="project.projectId" class="project-group">
          <button
            class="project-title"
            :class="{ active: project.projectId === store.selectedProjectId }"
            @click="toggleProject(project.projectId)"
          >
            <el-icon>
              <FolderOpened v-if="isExpanded(project.projectId)" />
              <Folder v-else />
            </el-icon>
            <span>{{ project.name || '未命名项目' }}</span>
            <el-icon class="project-arrow">
              <ArrowDown v-if="isExpanded(project.projectId)" />
              <ArrowRight v-else />
            </el-icon>
          </button>

          <div v-if="isExpanded(project.projectId)" class="conversation-list">
            <div v-for="conversation in conversationsOf(project.projectId)" :key="conversation.conversationId" class="conversation-row">
              <button
                class="conversation-item"
                :class="{ active: conversation.conversationId === store.selectedConversationId }"
                @click="selectConversation(project.projectId, conversation.conversationId)"
              >
                {{ conversation.title || '未命名会话' }}
              </button>
              <el-button
                link
                class="conversation-edit"
                :icon="Edit"
                title="重命名会话"
                @click.stop="openRenameConversation(conversation)"
              />
              <el-button
                link
                type="danger"
                class="conversation-delete"
                :icon="Delete"
                @click.stop="removeConversation(conversation.conversationId)"
              />
            </div>
            <button class="conversation-add" @click="createConversationFor(project.projectId)">+ 添加会话</button>
          </div>
        </div>
      </el-scrollbar>
    </aside>

    <main class="chat-panel cf-panel">
      <div class="chat-header">
        <div>
          <h2>{{ activeConversationName }}</h2>
          <p>{{ activeProjectName }} · {{ platformLabel(form.platform) }}</p>
        </div>
        <el-space wrap>
          <el-radio-group v-model="form.outputType" size="small">
            <el-radio-button label="VIDEO_PLAN">短视频方案</el-radio-button>
            <el-radio-button label="ARTICLE_PLAN">图文文章</el-radio-button>
          </el-radio-group>
          <el-select v-model="form.platform" style="width: 130px">
            <el-option v-for="item in PLATFORM_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
        </el-space>
      </div>

      <div class="context-row">
        <div class="context-field">
          <div class="context-field-header">
            <span>知识库</span>
            <el-switch v-model="autoKnowledgeSelection" size="small" active-text="自动选择" />
          </div>
          <el-select
            v-model="store.selectedKnowledgeBaseIds"
            multiple
            collapse-tags
            :disabled="autoKnowledgeSelection"
            placeholder="选择知识库"
            @change="store.persistSelection()"
          >
            <el-option v-for="item in store.knowledgeBases" :key="item.knowledgeBaseId" :label="item.name || '未命名知识库'" :value="item.knowledgeBaseId">
              <div class="select-option">
                <span>{{ item.name || '未命名知识库' }}</span>
                <small>{{ item.defaultPlatform ? platformLabel(item.defaultPlatform) : '通用' }}</small>
              </div>
            </el-option>
          </el-select>
        </div>
        <div class="context-field">
          <div class="context-field-header">
            <span>素材</span>
            <small>{{ selectedMaterialSummary }}</small>
          </div>
          <el-select v-model="store.selectedMaterialIds" multiple collapse-tags placeholder="选择素材" @change="store.persistSelection()">
            <el-option
              v-for="item in store.materials"
              :key="item.materialId"
              :label="item.originalFilename || '未命名素材'"
              :value="item.materialId"
              :disabled="materialDisabled(item)"
            >
              <div class="select-option">
                <span>{{ item.originalFilename || '未命名素材' }}</span>
                <small>{{ materialOptionMeta(item) }}</small>
              </div>
            </el-option>
          </el-select>
        </div>
      </div>

      <div class="style-row">
        <div class="style-label">风格偏好</div>
        <el-input v-model="form.stylePreference" placeholder="例如真实科普、节奏紧凑、适合亲子家庭" />
      </div>

      <el-scrollbar ref="messageScrollbar" class="message-scroll">
        <div v-if="!messages.length" class="cf-empty">还没有对话消息，可以直接发送需求。</div>
        <div v-for="message in messages" :key="message.messageId || `${message.role}-${message.content}`" class="message" :class="message.role">
          <div class="message-role">{{ message.role === 'assistant' ? 'ClipForge Agent' : '我' }}</div>
          <div class="message-bubble">{{ message.content }}</div>
        </div>
      </el-scrollbar>

      <div class="composer">
        <el-input v-model="input" type="textarea" :rows="4" placeholder="输入创作需求。" />
        <div class="composer-actions">
          <el-tag v-if="blockingMaterialCount" type="warning" effect="dark">有 {{ blockingMaterialCount }} 个素材仍在处理</el-tag>
          <el-button type="primary" :icon="Promotion" :loading="sending" @click="sendMessage">发送</el-button>
        </div>
      </div>
    </main>

    <aside class="result-panel cf-panel">
      <div class="cf-panel-header">
        <div>
          <h3 class="cf-title">最近结果</h3>
          <p class="cf-subtitle">{{ resultSubtitle }}</p>
        </div>
        <el-button :icon="Download" :loading="exporting" :disabled="!store.currentRunId" @click="exportCurrentMarkdown">导出 MD</el-button>
      </div>
      <el-scrollbar class="result-scroll">
        <div class="result-body">
          <CreationPlanView :plan="store.currentPlan" :output-type="store.currentOutputType" :output="store.currentOutput" />
        </div>
      </el-scrollbar>
    </aside>
  </div>

  <el-dialog v-model="showProjectDialog" title="新建项目" width="520px">
    <div class="cf-grid">
      <el-input v-model="projectForm.name" placeholder="项目名称" />
      <el-input v-model="projectForm.description" type="textarea" :rows="3" placeholder="项目描述" />
    </div>
    <template #footer>
      <el-button @click="showProjectDialog = false">取消</el-button>
      <el-button type="primary" @click="createProjectNow">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showRenameDialog" title="重命名会话" width="420px">
    <el-input
      v-model="renameTitle"
      maxlength="80"
      show-word-limit
      placeholder="请输入会话名称"
      @keyup.enter="renameConversation"
    />
    <template #footer>
      <el-button @click="showRenameDialog = false">取消</el-button>
      <el-button type="primary" :loading="renaming" @click="renameConversation">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
// @ts-nocheck
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type ScrollbarInstance } from 'element-plus'
import { ArrowDown, ArrowRight, Delete, Download, Edit, Folder, FolderOpened, Plus, Promotion, Refresh } from '@element-plus/icons-vue'
import CreationPlanView from '@/components/creation/CreationPlanView.vue'
import { downloadMarkdownUrl, exportMarkdown, queryModelCalls, queryQualityReport, queryTrace } from '@/api/creation'
import { createConversation, createProject, deleteConversation, listConversationMessages, listProjectConversations, routeConversation, updateConversation } from '@/api/project'
import { loadWorkspace, refreshMaterials } from '@/services/workspaceBootstrap'
import { useWorkspaceStore } from '@/stores/workspace'
import type { ConversationMessageResponse, ConversationResponse, MaterialResponse } from '@/types/domain'
import type { CreationOutputType } from '@/types/api'
import { DEMO } from '@/utils/demo'
import { PLATFORM_OPTIONS, materialTypeLabel, platformLabel, statusLabel } from '@/utils/display'

const store = useWorkspaceStore()
const messages = ref<ConversationMessageResponse[]>([])
const input = ref('')
const sending = ref(false)
const exporting = ref(false)
const showProjectDialog = ref(false)
const showRenameDialog = ref(false)
const renameTitle = ref('')
const renaming = ref(false)
const renameConversationId = ref('')
const expandedProjectIds = ref<string[]>([])
const conversationMap = ref<Record<string, ConversationResponse[]>>({})
const messageScrollbar = ref<ScrollbarInstance>()
const autoKnowledgeSelection = ref(false)
const form = ref({
  platform: DEMO.platform,
  stylePreference: DEMO.stylePreference,
  outputType: 'VIDEO_PLAN' as CreationOutputType
})
const projectForm = reactive({
  name: '',
  description: ''
})

const activeProjectName = computed(() => store.projects.find((item) => item.projectId === store.selectedProjectId)?.name || '尚未选择项目')
const activeConversationName = computed(() => store.conversations.find((item) => item.conversationId === store.selectedConversationId)?.title || '尚未选择会话')
const selectedMaterials = computed(() => store.materials.filter((item) => item.materialId && store.selectedMaterialIds.includes(item.materialId)))
const blockingMaterialCount = computed(() => selectedMaterials.value.filter((item) => materialDisabled(item)).length)
const selectedProcessTaskIds = computed(() => selectedMaterials.value.map((item) => item.processTaskId || '').filter(Boolean))
const selectedMaterialSummary = computed(() => {
  if (!store.selectedMaterialIds.length) return '未选择'
  const ready = selectedMaterials.value.filter((item) => !materialDisabled(item)).length
  return `${ready}/${store.selectedMaterialIds.length} 可用`
})
const resultSubtitle = computed(() => {
  if (!store.currentRunId) return '展示最近一次创作方案'
  return outputTypeLabel(store.currentOutputType || form.value.outputType)
})

function isExpanded(projectId?: string) {
  return !!projectId && expandedProjectIds.value.includes(projectId)
}

function conversationsOf(projectId?: string) {
  return projectId ? conversationMap.value[projectId] || [] : []
}

async function reloadAll() {
  await loadWorkspace(store)
  await loadConversationMap()
  expandActiveProject()
  await loadMessages()
}

function materialDisabled(material?: MaterialResponse) {
  if (!material) return true
  if (material.canUseForCreation === false) return true
  const status = material.processStatus || material.status
  return ['PENDING', 'PROCESSING', 'RUNNING', 'FAILED'].includes(status)
}

function materialOptionMeta(material: MaterialResponse) {
  return `${materialTypeLabel(material.materialType)} · ${statusLabel(material.processStatus || material.status)}`
}

function outputTypeLabel(outputType?: CreationOutputType) {
  return outputType === 'ARTICLE_PLAN' ? '图文文章' : '短视频方案'
}

async function loadConversationMap() {
  const entries = await Promise.all(
    store.projects.map(async (project) => {
      const conversations = project.projectId ? await listProjectConversations(project.projectId).catch(() => []) : []
      return [project.projectId || '', conversations] as const
    })
  )
  conversationMap.value = Object.fromEntries(entries.filter(([projectId]) => projectId))
}

function expandActiveProject() {
  if (store.selectedProjectId && !expandedProjectIds.value.includes(store.selectedProjectId)) {
    expandedProjectIds.value = [...expandedProjectIds.value, store.selectedProjectId]
  }
}

async function toggleProject(projectId?: string) {
  if (!projectId) return
  if (isExpanded(projectId)) {
    expandedProjectIds.value = expandedProjectIds.value.filter((id) => id !== projectId)
    return
  }
  expandedProjectIds.value = [...expandedProjectIds.value, projectId]
  await selectProject(projectId)
}

async function selectProject(projectId?: string) {
  if (!projectId) return
  store.setActiveProject(projectId)
  const conversations = conversationsOf(projectId)
  store.setConversations(conversations)
  store.setActiveConversation(conversations[0]?.conversationId)
  await refreshMaterials(store)
  await loadMessages()
}

async function selectConversation(projectId?: string, conversationId?: string) {
  if (!projectId || !conversationId) return
  store.setActiveProject(projectId)
  store.setConversations(conversationsOf(projectId))
  store.setActiveConversation(conversationId)
  expandActiveProject()
  await refreshMaterials(store)
  await loadMessages()
}

async function createProjectNow() {
  if (!projectForm.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  await createProject({
    userId: DEMO.userId,
    name: projectForm.name.trim(),
    description: projectForm.description.trim()
  })
  showProjectDialog.value = false
  projectForm.name = ''
  projectForm.description = ''
  await loadWorkspace(store)
  await loadConversationMap()
  expandActiveProject()
  ElMessage.success('项目已创建')
}

async function createConversationFor(projectId?: string) {
  if (!projectId) return
  const conversation = await createConversation(projectId, {
    userId: DEMO.userId,
    title: `新的创作会话 ${new Date().toLocaleTimeString()}`
  })
  const conversations = await listProjectConversations(projectId).catch(() => [])
  conversationMap.value = { ...conversationMap.value, [projectId]: conversations }
  store.setActiveProject(projectId)
  store.setConversations(conversations)
  store.setActiveConversation(conversation.conversationId)
  expandActiveProject()
  await refreshMaterials(store)
  await loadMessages()
}

function openRenameConversation(conversation?: ConversationResponse) {
  if (!conversation?.conversationId) return
  renameConversationId.value = conversation.conversationId
  renameTitle.value = conversation.title || ''
  showRenameDialog.value = true
}

async function renameConversation() {
  const title = renameTitle.value.trim()
  if (!renameConversationId.value || !title) {
    ElMessage.warning('请输入会话名称')
    return
  }
  renaming.value = true
  try {
    await updateConversation(renameConversationId.value, { title })
    await reloadAll()
    showRenameDialog.value = false
    ElMessage.success('会话名称已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '会话重命名失败')
  } finally {
    renaming.value = false
  }
}

async function removeConversation(conversationId?: string) {
  if (!conversationId) return
  await ElMessageBox.confirm('确认删除这个会话吗？删除后将从列表中隐藏。', '删除会话', { type: 'warning' })
  await deleteConversation(conversationId)
  await reloadAll()
  ElMessage.success('会话已删除')
}

async function loadMessages() {
  if (!store.selectedConversationId) {
    messages.value = []
    return
  }
  const response = await listConversationMessages(store.selectedConversationId).catch(() => ({ messages: [] }))
  messages.value = response.messages || []
  await nextTick()
  messageScrollbar.value?.setScrollTop(999999)
}

async function sendMessage() {
  if (!input.value.trim()) return
  if (!store.selectedProjectId || !store.selectedConversationId) {
    ElMessage.warning('请先选择项目和会话')
    return
  }
  if (blockingMaterialCount.value > 0) {
    ElMessage.warning('请等待已选素材处理完成，或先取消未完成素材')
    return
  }

  sending.value = true
  try {
    const userText = input.value.trim()
    messages.value.push({
      role: 'user',
      content: userText,
      conversationId: store.selectedConversationId
    })
    input.value = ''
    await nextTick()
    messageScrollbar.value?.setScrollTop(999999)

    const response = await routeConversation({
      userId: DEMO.userId,
      projectId: store.selectedProjectId,
      conversationId: store.selectedConversationId,
      message: userText,
      platform: form.value.platform,
      stylePreference: form.value.stylePreference,
      outputType: form.value.outputType,
      materialIds: store.selectedMaterialIds,
      processTaskIds: selectedProcessTaskIds.value,
      knowledgeBaseIds: autoKnowledgeSelection.value ? [] : store.selectedKnowledgeBaseIds,
      recentMessages: messages.value.slice(-6).map((item) => `${item.role}: ${item.content}`)
    })

    const creation = response.creation
    const answerText = response.answerText || creation?.result?.hook || '已完成处理。'
    messages.value.push({
      role: 'assistant',
      content: answerText,
      conversationId: store.selectedConversationId,
      runId: creation?.runId
    })

    if (creation) {
      store.setCurrentRun(creation.runId)
      store.setCurrentPlan(creation.result)
      store.setCurrentOutput(creation.output, creation.outputType)
      store.setTraceEvents(creation.traceEvents || [])
      if (creation.runId) {
        const [quality, traces, calls] = await Promise.allSettled([
          queryQualityReport(creation.runId),
          queryTrace(creation.runId),
          queryModelCalls(creation.runId)
        ])
        if (quality.status === 'fulfilled') store.setQualityReport(quality.value)
        if (traces.status === 'fulfilled') store.setTraceEvents(traces.value.traceEvents || [])
        if (calls.status === 'fulfilled') store.setModelCallRecords(calls.value.records || [])
      }
    }

    await loadMessages()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败')
  } finally {
    sending.value = false
  }
}

async function exportCurrentMarkdown() {
  if (!store.currentRunId) {
    ElMessage.warning('暂无可导出的创作结果')
    return
  }
  exporting.value = true
  try {
    const exported = await exportMarkdown(store.currentRunId)
    if (!exported.exportId) {
      ElMessage.warning('导出记录缺少编号')
      return
    }
    window.open(downloadMarkdownUrl(exported.exportId), '_blank')
    ElMessage.success('Markdown 已生成')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  await reloadAll()
})
</script>

<style scoped>
.studio-layout{display:grid;grid-template-columns:270px minmax(0,1fr) 330px;gap:18px;height:calc(100vh - 116px);min-height:650px}.project-panel,.chat-panel,.result-panel{min-height:0;overflow:hidden}.project-panel{grid-column:1;grid-row:1;display:grid;grid-template-rows:auto auto 1fr}.result-panel{grid-column:2;grid-row:1;display:grid;grid-template-rows:auto minmax(0,1fr)}.chat-panel{grid-column:3;grid-row:1;display:grid;grid-template-rows:auto auto auto minmax(0,1fr) auto;background:#fbfdfb}.project-actions{display:grid;gap:8px;padding:14px;border-bottom:1px solid var(--cf-border)}.project-scroll,.result-scroll,.message-scroll{min-height:0}.project-group{padding:0 12px 10px}.project-title,.conversation-item,.conversation-add{width:100%;border:0;color:var(--cf-text);text-align:left;cursor:pointer}.project-title{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:10px;padding:11px 10px;border-radius:8px;background:transparent;font-size:14px;font-weight:700}.project-title:hover,.project-title.active{background:var(--cf-green-pale);color:var(--cf-green-deep)}.project-arrow{color:var(--cf-text-faint)}.conversation-list{display:grid;gap:5px;margin:8px 0 6px 15px}.conversation-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:6px;align-items:center}.conversation-item,.conversation-add{padding:9px 10px;border-radius:7px;background:#f8faf8;font-size:12px}.conversation-item:hover,.conversation-item.active{background:#edf5ef;color:var(--cf-green-deep);outline:1px solid #d6e7da}.conversation-delete{flex:0 0 auto}.conversation-add{color:var(--cf-green-deep);border:1px dashed var(--cf-border-strong)}.chat-header,.context-row,.style-row,.composer{padding:14px 16px;border-bottom:1px solid var(--cf-border)}.chat-header{display:block}.chat-header h2{margin:0;color:var(--cf-ink);font-size:18px}.chat-header p{margin:5px 0 12px;color:var(--cf-text-muted);font-size:12px}.context-row{display:grid;gap:12px}.context-field{display:grid;gap:7px;min-width:0}.context-field-header{display:flex;align-items:center;justify-content:space-between;gap:10px;color:var(--cf-text);font-size:12px;font-weight:700}.context-field-header small{color:var(--cf-text-muted)}.style-row{padding-top:0}.select-option{display:flex;align-items:center;justify-content:space-between;gap:14px}.select-option small{color:var(--cf-text-muted)}.message-scroll{padding:16px 14px}.message{margin-bottom:15px}.message.assistant{padding-right:16px}.message.user{padding-left:16px}.message-role{margin-bottom:5px;color:var(--cf-text-faint);font-size:11px}.message-bubble{padding:11px 12px;border:1px solid var(--cf-border);border-radius:9px;background:#fff;line-height:1.7;white-space:pre-wrap;font-size:12px}.message.user .message-bubble{background:var(--cf-green-pale);border-color:#d6e8da}.composer{border-top:1px solid var(--cf-border);border-bottom:0;background:#fff}.composer-actions{display:flex;justify-content:flex-end;gap:8px;align-items:center;margin-top:9px}.result-body{padding:20px}.result-scroll :deep(.el-scrollbar__view){min-height:100%}.result-panel :deep(.cf-panel-header){padding:18px 22px}.result-panel :deep(.cf-title){font-size:18px}
@media(max-width:1250px){.studio-layout{grid-template-columns:235px minmax(0,1fr) 290px;gap:12px}.result-body{padding:14px}}
@media(max-width:900px){
  .studio-layout{display:flex;flex-direction:column;height:auto;min-height:0}
  .project-panel{order:1;min-height:220px}
  .result-panel{order:2;min-height:520px}
  .chat-panel{order:3;min-height:650px}
  .project-scroll{max-height:160px}
  .result-scroll{min-height:420px}
  .chat-panel{overflow:visible}
}

.conversation-row{grid-template-columns:minmax(0,1fr) auto auto}
.conversation-edit,.conversation-delete{padding:4px;color:var(--cf-text-faint)}
.conversation-edit:hover{color:var(--cf-green-deep)}
.style-label{margin-bottom:7px;color:var(--cf-text);font-size:12px;font-weight:700}
</style>
