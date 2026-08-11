<template>
  <div class="cf-grid" style="gap: 20px">
    <SectionCard title="知识库" desc="管理创作时可检索的资料，页面只展示人可读名称。">
      <div class="cf-toolbar">
        <el-space wrap>
          <el-select v-model="selectedKnowledgeBaseId" filterable placeholder="选择知识库" style="width: 300px" @change="loadDocuments">
            <el-option v-for="item in knowledgeBases" :key="item.knowledgeBaseId" :label="item.name || '未命名知识库'" :value="item.knowledgeBaseId" />
          </el-select>
          <el-button @click="loadKnowledgeBases">刷新知识库</el-button>
          <el-button type="primary" :icon="Plus" @click="showCreateKb = true">创建知识库</el-button>
        </el-space>
        <el-space wrap>
          <el-button :icon="Refresh" @click="loadDocuments">刷新文档</el-button>
          <el-button type="success" :icon="UploadFilled" @click="openDocPicker">上传文档</el-button>
          <el-button type="warning" :icon="Link" @click="showUrlDialog = true">创建 URL 文档</el-button>
        </el-space>
      </div>
      <input ref="docInput" type="file" accept=".md,.txt,.pdf,.doc,.docx" hidden @change="onPickDoc" />
    </SectionCard>

    <section class="knowledge-layout">
      <SectionCard title="文档列表" desc="点击文档查看处理状态、切片和日志。">
        <el-table :data="documents" size="small" stripe highlight-current-row @row-click="selectDocument">
          <el-table-column prop="originalFilename" label="文档名" min-width="190" />
          <el-table-column label="解析" width="100">
            <template #default="{ row }">{{ statusLabel(row.parseStatus) }}</template>
          </el-table-column>
          <el-table-column label="切片" width="100">
            <template #default="{ row }">{{ statusLabel(row.chunkStatus) }}</template>
          </el-table-column>
          <el-table-column label="向量化" width="110">
            <template #default="{ row }">{{ statusLabel(row.embeddingStatus) }}</template>
          </el-table-column>
          <el-table-column label="整体状态" width="120">
            <template #default="{ row }">{{ statusLabel(row.overallStatus) }}</template>
          </el-table-column>
        </el-table>
      </SectionCard>

      <SectionCard title="文档详情" desc="展示文档处理状态、切片内容和同步计划。">
        <el-empty v-if="!selectedDocument" description="请选择一个文档。" />
        <template v-else>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="文档名">{{ selectedDocument.originalFilename }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ selectedDocument.sourceType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="整体状态">{{ statusLabel(selectedDocument.overallStatus) }}</el-descriptions-item>
            <el-descriptions-item label="切片数量">{{ selectedDocument.chunkCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="字符数">{{ selectedDocument.charCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="词元估算">{{ selectedDocument.tokenCount ?? '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="cf-divider" />
          <el-space wrap>
            <el-button type="primary" @click="processDocumentNow">同步处理</el-button>
            <el-button @click="asyncProcessDocumentNow">异步处理</el-button>
            <el-button type="success" @click="fullProcessDocumentNow">完整处理</el-button>
            <el-button type="warning" @click="retryDocumentNow">重试</el-button>
            <el-button @click="queryStatusNow">查状态</el-button>
            <el-button @click="loadChunks">切片</el-button>
            <el-button @click="loadProcessLogs">日志</el-button>
          </el-space>

          <div class="cf-divider" />
          <el-tabs v-model="activeTab">
            <el-tab-pane label="状态" name="status">
              <InfoSummary :rows="documentStatusRowsView" />
            </el-tab-pane>
          <el-tab-pane label="切片" name="chunks">
              <el-table :data="chunks" size="small" stripe>
                <el-table-column prop="chunkIndex" label="序号" width="80" />
                <el-table-column prop="titlePath" label="标题路径" min-width="160" />
                <el-table-column prop="content" label="内容" min-width="300" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="处理日志" name="logs">
              <el-table :data="processLogs" size="small" stripe>
                <el-table-column prop="phase" label="阶段" width="150" />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">{{ statusLabel(row.status) }}</template>
                </el-table-column>
                <el-table-column prop="message" label="消息" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="URL 同步" name="url">
              <el-button type="primary" @click="showUrlScheduleDialog = true">编辑同步计划</el-button>
              <el-button @click="loadUrlSchedule">刷新计划</el-button>
              <el-button type="success" :disabled="!urlSchedule" @click="syncUrlScheduleNow">立即同步</el-button>
              <InfoSummary :rows="urlScheduleRowsView" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </SectionCard>
    </section>
  </div>

  <el-dialog v-model="showCreateKb" title="创建知识库" width="560px">
    <div class="cf-grid">
      <el-input v-model="createKbForm.name" placeholder="知识库名称" />
      <el-input v-model="createKbForm.description" placeholder="描述" />
      <el-input v-model="createKbForm.category" placeholder="分类" />
      <el-select v-model="createKbForm.defaultPlatform" placeholder="默认平台">
        <el-option v-for="item in PLATFORM_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </div>
    <template #footer>
      <el-button @click="showCreateKb = false">取消</el-button>
      <el-button type="primary" @click="createKnowledgeBaseNow">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showUrlDialog" title="创建 URL 文档" width="680px">
    <div class="cf-grid">
      <el-input v-model="urlForm.sourceUrl" placeholder="https://..." />
      <el-input v-model="urlForm.scheduleCron" placeholder="Cron，例如 0 0 * * *" />
      <el-switch v-model="urlForm.scheduleEnabled" active-text="启用定时同步" />
    </div>
    <template #footer>
      <el-button @click="showUrlDialog = false">取消</el-button>
      <el-button type="primary" @click="createUrlDocumentNow">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showUrlScheduleDialog" title="编辑 URL 同步计划" width="560px">
    <div class="cf-grid">
      <el-input v-model="scheduleForm.scheduleCron" placeholder="Cron 表达式" />
      <el-switch v-model="scheduleForm.enabled" active-text="启用同步" />
    </div>
    <template #footer>
      <el-button @click="showUrlScheduleDialog = false">取消</el-button>
      <el-button type="primary" @click="upsertUrlScheduleNow">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
// @ts-nocheck
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Link, Plus, Refresh, UploadFilled } from '@element-plus/icons-vue'
import SectionCard from '@/components/common/SectionCard.vue'
import InfoSummary from '@/components/common/InfoSummary.vue'
import {
  asyncProcessKnowledgeDocument,
  createKnowledgeBase,
  createKnowledgeUrlDocument,
  fullProcessKnowledgeDocument,
  listKnowledgeBases,
  listKnowledgeChunks,
  listKnowledgeDocumentProcessLogs,
  listKnowledgeDocuments,
  processKnowledgeDocument,
  queryKnowledgeDocumentProcessStatus,
  queryUrlSyncSchedule,
  retryKnowledgeDocumentProcess,
  syncUrlSchedule,
  upsertUrlSyncSchedule,
  uploadKnowledgeDocument
} from '@/api/knowledge'
import type { KnowledgeBaseResponse, KnowledgeChunkResponse, KnowledgeDocumentProcessLogResponse, KnowledgeDocumentProcessStatusResponse, KnowledgeDocumentResponse, KnowledgeUrlSyncScheduleResponse } from '@/types/domain'
import { useWorkspaceStore } from '@/stores/workspace'
import { DEMO } from '@/utils/demo'
import { PLATFORM_OPTIONS, statusLabel } from '@/utils/display'
import { documentStatusRows, urlScheduleRows } from '@/utils/presentation'

const store = useWorkspaceStore()
const docInput = ref<HTMLInputElement>()
const selectedKnowledgeBaseId = ref('')
const knowledgeBases = ref<KnowledgeBaseResponse[]>([])
const documents = ref<KnowledgeDocumentResponse[]>([])
const selectedDocument = ref<KnowledgeDocumentResponse>()
const processStatus = ref<KnowledgeDocumentProcessStatusResponse>()
const chunks = ref<KnowledgeChunkResponse[]>([])
const processLogs = ref<KnowledgeDocumentProcessLogResponse[]>([])
const urlSchedule = ref<KnowledgeUrlSyncScheduleResponse>()
const activeTab = ref('status')
const showCreateKb = ref(false)
const showUrlDialog = ref(false)
const showUrlScheduleDialog = ref(false)
const createKbForm = reactive({
  name: DEMO.knowledgeBaseName,
  description: '用于虎鲸科普创作的知识库',
  category: 'ANIMAL_SCIENCE',
  defaultPlatform: DEMO.platform
})
const urlForm = reactive({
  sourceUrl: '',
  scheduleCron: '0 0 * * *',
  scheduleEnabled: true
})
const scheduleForm = reactive({
  scheduleCron: '',
  enabled: true
})

const documentStatusRowsView = computed(() => documentStatusRows(processStatus.value, selectedDocument.value as unknown as Record<string, unknown>))
const urlScheduleRowsView = computed(() => urlScheduleRows(urlSchedule.value))

function openDocPicker() {
  docInput.value?.click()
}

async function loadKnowledgeBases() {
  knowledgeBases.value = await listKnowledgeBases(DEMO.userId).catch(() => [])
  store.setKnowledgeBases(knowledgeBases.value)
  if (!selectedKnowledgeBaseId.value || !knowledgeBases.value.some((item) => item.knowledgeBaseId === selectedKnowledgeBaseId.value)) {
    selectedKnowledgeBaseId.value = store.selectedKnowledgeBaseIds[0] || knowledgeBases.value[0]?.knowledgeBaseId || ''
  }
  await loadDocuments()
}

async function loadDocuments() {
  if (!selectedKnowledgeBaseId.value) {
    documents.value = []
    return
  }
  documents.value = await listKnowledgeDocuments(selectedKnowledgeBaseId.value).catch(() => [])
  store.setSelectedKnowledgeBaseIds([selectedKnowledgeBaseId.value])
}

async function selectDocument(row: KnowledgeDocumentResponse) {
  selectedDocument.value = row
  await queryStatusNow()
  await loadChunks()
  await loadProcessLogs()
  await loadUrlSchedule()
}

async function queryStatusNow() {
  if (!selectedDocument.value?.documentId) return
  processStatus.value = await queryKnowledgeDocumentProcessStatus(selectedDocument.value.documentId).catch(() => undefined)
}

async function loadChunks() {
  if (!selectedDocument.value?.documentId) return
  chunks.value = await listKnowledgeChunks(selectedDocument.value.documentId).catch(() => [])
}

async function loadProcessLogs() {
  if (!selectedDocument.value?.documentId) return
  processLogs.value = await listKnowledgeDocumentProcessLogs(selectedDocument.value.documentId).catch(() => [])
}

async function loadUrlSchedule() {
  if (!selectedDocument.value?.documentId) return
  const schedule = await queryUrlSyncSchedule(selectedDocument.value.documentId).catch(() => undefined)
  urlSchedule.value = schedule
  if (schedule) {
    scheduleForm.scheduleCron = (schedule as { scheduleCron?: string }).scheduleCron || ''
    scheduleForm.enabled = (schedule as { enabled?: boolean }).enabled ?? true
  }
}

async function uploadDocumentNow(file: File) {
  if (!selectedKnowledgeBaseId.value) {
    ElMessage.warning('请先选择知识库。')
    return
  }
  const response = await uploadKnowledgeDocument(selectedKnowledgeBaseId.value, file, DEMO.userId)
  await loadDocuments()
  selectedDocument.value = response
  ElMessage.success(`已上传：${response.originalFilename}`)
}

async function onPickDoc(event: Event) {
  const files = (event.target as HTMLInputElement).files
  if (!files?.length) return
  for (const file of Array.from(files)) {
    await uploadDocumentNow(file)
  }
}

async function createKnowledgeBaseNow() {
  const response = await createKnowledgeBase({ userId: DEMO.userId, ...createKbForm })
  showCreateKb.value = false
  await loadKnowledgeBases()
  selectedKnowledgeBaseId.value = response.knowledgeBaseId || selectedKnowledgeBaseId.value
  await loadDocuments()
}

async function createUrlDocumentNow() {
  if (!selectedKnowledgeBaseId.value) return
  const response = await createKnowledgeUrlDocument(selectedKnowledgeBaseId.value, {
    sourceUrl: urlForm.sourceUrl,
    userId: DEMO.userId,
    scheduleCron: urlForm.scheduleCron,
    scheduleEnabled: urlForm.scheduleEnabled
  })
  showUrlDialog.value = false
  await loadDocuments()
  selectedDocument.value = response
}

async function processDocumentNow() {
  if (!selectedDocument.value?.documentId) return
  await processKnowledgeDocument(selectedDocument.value.documentId)
  await queryStatusNow()
}

async function asyncProcessDocumentNow() {
  if (!selectedDocument.value?.documentId) return
  await asyncProcessKnowledgeDocument(selectedDocument.value.documentId)
  await queryStatusNow()
}

async function fullProcessDocumentNow() {
  if (!selectedDocument.value?.documentId) return
  await fullProcessKnowledgeDocument(selectedDocument.value.documentId)
  await queryStatusNow()
}

async function retryDocumentNow() {
  if (!selectedDocument.value?.documentId) return
  await retryKnowledgeDocumentProcess(selectedDocument.value.documentId)
  await queryStatusNow()
}

async function upsertUrlScheduleNow() {
  if (!selectedDocument.value?.documentId) return
  await upsertUrlSyncSchedule(selectedDocument.value.documentId, {
    scheduleCron: scheduleForm.scheduleCron,
    enabled: scheduleForm.enabled
  })
  showUrlScheduleDialog.value = false
  await loadUrlSchedule()
}

async function syncUrlScheduleNow() {
  const scheduleId = (urlSchedule.value as { scheduleId?: string } | undefined)?.scheduleId
  if (!scheduleId) return
  await syncUrlSchedule(scheduleId)
  await loadUrlSchedule()
}

onMounted(async () => {
  store.hydrateSelection()
  await loadKnowledgeBases()
})
</script>
