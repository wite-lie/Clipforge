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
          <el-select v-model="form.platform" style="width: 130px">
            <el-option v-for="item in PLATFORM_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
        </el-space>
      </div>

      <div class="context-row">
        <el-select v-model="store.selectedKnowledgeBaseIds" multiple collapse-tags placeholder="选择知识库" @change="store.persistSelection()">
          <el-option v-for="item in store.knowledgeBases" :key="item.knowledgeBaseId" :label="item.name || '未命名知识库'" :value="item.knowledgeBaseId" />
        </el-select>
        <el-select v-model="store.selectedMaterialIds" multiple collapse-tags placeholder="选择素材" @change="store.persistSelection()">
          <el-option v-for="item in store.materials" :key="item.materialId" :label="item.originalFilename || '未命名素材'" :value="item.materialId" />
        </el-select>
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
          <el-button type="primary" :icon="Promotion" :loading="sending" @click="sendMessage">发送</el-button>
        </div>
      </div>
    </main>

    <aside class="result-panel cf-panel">
      <div class="cf-panel-header">
        <div>
          <h3 class="cf-title">最近结果</h3>
          <p class="cf-subtitle">展示最近一次创作方案</p>
        </div>
      </div>
      <el-scrollbar class="result-scroll">
        <div class="result-body">
          <CreationPlanView :plan="store.currentPlan" />
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
</template>

<script setup lang="ts">
// @ts-nocheck
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type ScrollbarInstance } from 'element-plus'
import { ArrowDown, ArrowRight, Delete, Folder, FolderOpened, Plus, Promotion, Refresh } from '@element-plus/icons-vue'
import CreationPlanView from '@/components/creation/CreationPlanView.vue'
import { queryModelCalls, queryQualityReport, queryTrace } from '@/api/creation'
import { createConversation, createProject, deleteConversation, listConversationMessages, listProjectConversations, routeConversation } from '@/api/project'
import { loadWorkspace, refreshMaterials } from '@/services/workspaceBootstrap'
import { useWorkspaceStore } from '@/stores/workspace'
import type { ConversationMessageResponse, ConversationResponse } from '@/types/domain'
import { DEMO } from '@/utils/demo'
import { PLATFORM_OPTIONS, platformLabel } from '@/utils/display'

const store = useWorkspaceStore()
const messages = ref<ConversationMessageResponse[]>([])
const input = ref('')
const sending = ref(false)
const showProjectDialog = ref(false)
const expandedProjectIds = ref<string[]>([])
const conversationMap = ref<Record<string, ConversationResponse[]>>({})
const messageScrollbar = ref<ScrollbarInstance>()
const form = ref({
  platform: DEMO.platform,
  stylePreference: DEMO.stylePreference
})
const projectForm = reactive({
  name: '',
  description: ''
})

const activeProjectName = computed(() => store.projects.find((item) => item.projectId === store.selectedProjectId)?.name || '尚未选择项目')
const activeConversationName = computed(() => store.conversations.find((item) => item.conversationId === store.selectedConversationId)?.title || '尚未选择会话')

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
      materialIds: store.selectedMaterialIds,
      knowledgeBaseIds: store.selectedKnowledgeBaseIds,
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

onMounted(async () => {
  await reloadAll()
})
</script>

<style scoped>
.studio-layout {
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr) 380px;
  gap: 16px;
  height: calc(100vh - 130px);
  min-height: 620px;
}

.project-panel,
.chat-panel,
.result-panel {
  min-height: 0;
  overflow: hidden;
}

.project-panel {
  display: grid;
  grid-template-rows: auto auto 1fr;
}

.project-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  padding: 14px;
  border-bottom: 1px solid var(--cf-border);
}

.project-scroll,
.result-scroll {
  min-height: 0;
}

.project-group {
  padding: 0 12px 10px;
}

.project-title,
.conversation-item,
.conversation-add {
  width: 100%;
  border: 0;
  color: #eaf3ff;
  text-align: left;
  cursor: pointer;
}

.project-title {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(8, 15, 28, 0.76);
  font-size: 15px;
  font-weight: 700;
}

.project-title.active {
  background: rgba(106, 228, 255, 0.18);
  outline: 1px solid rgba(106, 228, 255, 0.32);
  color: #ffffff;
}

.project-arrow {
  color: #9fb8d8;
}

.conversation-list {
  display: grid;
  gap: 6px;
  margin: 8px 0 6px 16px;
}

.conversation-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px;
  align-items: center;
}

.conversation-item,
.conversation-add {
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(15, 27, 48, 0.78);
}

.conversation-item.active {
  background: rgba(105, 224, 179, 0.16);
  color: #ffffff;
  outline: 1px solid rgba(105, 224, 179, 0.3);
}

.conversation-delete {
  flex: 0 0 auto;
}

.conversation-add {
  color: #9be7ff;
  border: 1px dashed rgba(106, 228, 255, 0.28);
}

.chat-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
}

.chat-header,
.context-row,
.composer {
  padding: 16px 18px;
  border-bottom: 1px solid var(--cf-border);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.chat-header h2 {
  margin: 0;
  font-size: 24px;
}

.chat-header p {
  margin: 6px 0 0;
  color: var(--cf-text-soft);
}

.context-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.message-scroll {
  min-height: 0;
  padding: 18px;
}

.message {
  margin-bottom: 16px;
}

.message.assistant {
  padding-right: 60px;
}

.message.user {
  padding-left: 60px;
}

.message-role {
  margin-bottom: 6px;
  color: #c7d8ee;
  font-size: 13px;
}

.message-bubble {
  padding: 14px 16px;
  border-radius: 10px;
  background: rgba(8, 15, 28, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.18);
  line-height: 1.8;
  white-space: pre-wrap;
}

.message.user .message-bubble {
  background: rgba(106, 228, 255, 0.12);
}

.composer {
  border-top: 1px solid var(--cf-border);
  border-bottom: 0;
  background: rgba(5, 11, 20, 0.58);
}

.composer-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.result-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.result-body {
  padding: 16px;
}
</style>
