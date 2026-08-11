<template>
  <div class="cf-grid" style="gap: 20px">
    <SectionCard title="素材中心" desc="素材按当前项目和会话查询，上传时自动关联当前上下文。">
      <div class="cf-toolbar">
        <el-space wrap>
          <el-select v-model="store.selectedProjectId" placeholder="选择项目" filterable style="width: 260px" @change="onProjectChange">
            <el-option v-for="item in store.projects" :key="item.projectId" :label="item.name || '未命名项目'" :value="item.projectId" />
          </el-select>
          <el-select v-model="store.selectedConversationId" placeholder="选择会话" filterable style="width: 260px" @change="onConversationChange">
            <el-option v-for="item in store.conversations" :key="item.conversationId" :label="item.title || '未命名会话'" :value="item.conversationId" />
          </el-select>
          <el-select v-model="materialType" placeholder="素材类型" clearable style="width: 140px" @change="loadMaterials">
            <el-option label="图片" value="IMAGE" />
            <el-option label="视频" value="VIDEO" />
          </el-select>
        </el-space>
        <el-space wrap>
          <el-button :icon="Refresh" @click="loadMaterials">刷新</el-button>
          <el-button type="primary" :icon="UploadFilled" @click="openImagePicker">上传图片</el-button>
          <el-button type="success" :icon="VideoCamera" @click="openVideoPicker">上传视频</el-button>
        </el-space>
      </div>
      <input ref="imageInput" type="file" accept="image/*" multiple hidden @change="onPickImages" />
      <input ref="videoInput" type="file" accept="video/*" multiple hidden @change="onPickVideos" />
    </SectionCard>

    <section class="cf-grid cf-grid-2 materials-workbench">
      <SectionCard class="materials-list-card" title="素材列表" desc="选择素材后查看处理任务、理解结果和视频处理链路。">
        <el-table :data="filteredMaterials" size="small" stripe highlight-current-row @row-click="selectMaterial">
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ materialTypeLabel(row.materialType) }}</template>
          </el-table-column>
          <el-table-column prop="originalFilename" label="文件名" min-width="180" />
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <StatusPill :status="row.processStatus || row.status" :text="statusLabel(row.processStatus || row.status)" />
            </template>
          </el-table-column>
          <el-table-column prop="processPhase" label="阶段" width="150" />
          <el-table-column label="进度" width="110">
            <template #default="{ row }">{{ row.progressPercent ?? 0 }}%</template>
          </el-table-column>
        </el-table>
      </SectionCard>

      <SectionCard class="materials-detail-card" title="素材详情" desc="这里展示后端返回的真实素材处理信息。">
        <el-empty v-if="!selected" description="请选择一条素材。" />
        <template v-else>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="文件名">{{ selected.originalFilename }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ materialTypeLabel(selected.materialType) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusPill :status="selected.processStatus || selected.status" :text="statusLabel(selected.processStatus || selected.status)" />
            </el-descriptions-item>
            <el-descriptions-item label="进度">{{ selected.progressPercent ?? 0 }}%</el-descriptions-item>
            <el-descriptions-item label="是否可创作">{{ selected.canUseForCreation ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="处理阶段">{{ selected.processPhase || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="cf-divider" />
          <el-space wrap>
            <el-button v-if="selected.materialType === 'VIDEO'" type="primary" @click="readVideoMetadataNow">读取元数据</el-button>
            <el-button v-if="selected.materialType === 'VIDEO'" @click="extractFramesNow">抽取候选帧</el-button>
            <el-button v-if="selected.materialType === 'VIDEO'" @click="extractAudioNow">提取音频</el-button>
            <el-button v-if="selected.materialType === 'VIDEO'" @click="transcribeAudioNow">语音识别</el-button>
            <el-button v-if="selected.materialType === 'VIDEO'" type="success" @click="understandVideoNow">理解视频</el-button>
            <el-button @click="queryUnderstandingNow">查看理解结果</el-button>
            <el-button v-if="selected.processTaskId" @click="queryTaskNow">查看任务状态</el-button>
          </el-space>
          <el-alert v-if="operationMessage" :title="operationMessage" type="success" show-icon class="cf-section" />

          <div class="cf-divider" />
          <el-tabs v-model="activeTab">
            <el-tab-pane label="任务状态" name="task">
              <InfoSummary :rows="taskRowsView" />
            </el-tab-pane>
            <el-tab-pane label="理解结果" name="understanding">
              <InfoSummary :rows="understandingRowsView" :sections="understandingSectionsView" />
            </el-tab-pane>
            <el-tab-pane label="视频元数据" name="metadata">
              <InfoSummary :rows="metadataRowsView" />
            </el-tab-pane>
            <el-tab-pane label="音频/字幕" name="audio">
              <InfoSummary :rows="audioRowsView" :sections="subtitleSectionsView" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </SectionCard>
    </section>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, UploadFilled, VideoCamera } from '@element-plus/icons-vue'
import SectionCard from '@/components/common/SectionCard.vue'
import InfoSummary from '@/components/common/InfoSummary.vue'
import StatusPill from '@/components/common/StatusPill.vue'
import { queryMaterialUnderstanding, queryTaskStatus, uploadImage, uploadVideo } from '@/api/material'
import { extractVideoAudio, extractVideoFrames, listVideoSubtitles, readVideoMetadata, transcribeVideoAudio, understandVideo, queryVideoUnderstanding } from '@/api/video'
import { loadWorkspace, refreshMaterials } from '@/services/workspaceBootstrap'
import { useWorkspaceStore } from '@/stores/workspace'
import type { TaskStatusResponse } from '@/types/api'
import type { MaterialResponse, MaterialUnderstandingResponse, VideoAudioResponse, VideoMetadataResponse, VideoSubtitleSegmentResponse, VideoUnderstandingResponse } from '@/types/domain'
import { DEMO } from '@/utils/demo'
import { materialTypeLabel, statusLabel } from '@/utils/display'
import { audioRows, materialUnderstandingRows, materialUnderstandingSections, subtitleSections, taskRows, videoMetadataRows } from '@/utils/presentation'

const store = useWorkspaceStore()
const imageInput = ref<HTMLInputElement>()
const videoInput = ref<HTMLInputElement>()
const activeTab = ref('task')
const selected = ref<MaterialResponse>()
const taskStatus = ref<TaskStatusResponse>()
const understandingResult = ref<MaterialUnderstandingResponse | VideoUnderstandingResponse>()
const videoMetadata = ref<VideoMetadataResponse>()
const videoAudio = ref<VideoAudioResponse>()
const subtitles = ref<VideoSubtitleSegmentResponse[]>([])
const materialType = ref('')
const operationMessage = ref('')

const filteredMaterials = computed(() => {
  return materialType.value ? store.materials.filter((item) => item.materialType === materialType.value) : store.materials
})

const taskRowsView = computed(() => taskRows(taskStatus.value, selected.value))
const understandingRowsView = computed(() => materialUnderstandingRows(understandingResult.value))
const understandingSectionsView = computed(() => materialUnderstandingSections(understandingResult.value))
const metadataRowsView = computed(() => videoMetadataRows(videoMetadata.value))
const audioRowsView = computed(() => audioRows(videoAudio.value))
const subtitleSectionsView = computed(() => subtitleSections(videoAudio.value, subtitles.value))

watch(
  () => store.materials,
  (materials) => {
    if (!selected.value?.materialId) return
    selected.value = materials.find((item) => item.materialId === selected.value?.materialId)
  },
  { deep: true }
)

function openImagePicker() {
  imageInput.value?.click()
}

function openVideoPicker() {
  videoInput.value?.click()
}

async function onProjectChange() {
  store.persistSelection()
  await loadWorkspace(store)
}

async function onConversationChange() {
  store.persistSelection()
  await loadMaterials()
}

async function loadMaterials() {
  await refreshMaterials(store)
}

async function uploadFiles(files: FileList | null, kind: 'image' | 'video') {
  if (!files?.length) return
  if (!store.selectedProjectId || !store.selectedConversationId) {
    ElMessage.warning('请先选择项目和会话。')
    return
  }
  try {
    const batchId = `upload_batch_${crypto.randomUUID().replaceAll('-', '')}`
    for (const file of Array.from(files)) {
      const payload = {
        file,
        userId: DEMO.userId,
        projectId: store.selectedProjectId,
        conversationId: store.selectedConversationId,
        runId: store.currentRunId,
        uploadBatchId: batchId
      }
      if (kind === 'image') {
        await uploadImage(payload)
      } else {
        await uploadVideo(payload)
      }
    }
    ElMessage.success('素材已上传。')
    await loadMaterials()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  }
}

async function onPickImages(event: Event) {
  await uploadFiles((event.target as HTMLInputElement).files, 'image')
}

async function onPickVideos(event: Event) {
  await uploadFiles((event.target as HTMLInputElement).files, 'video')
}

async function selectMaterial(row: MaterialResponse) {
  selected.value = row
  taskStatus.value = undefined
  understandingResult.value = undefined
  videoMetadata.value = undefined
  videoAudio.value = undefined
  subtitles.value = []
  operationMessage.value = ''
  await queryTaskNow().catch(() => undefined)
  await queryUnderstandingNow().catch(() => undefined)
}

async function queryTaskNow() {
  if (!selected.value?.processTaskId) return
  taskStatus.value = await queryTaskStatus(selected.value.processTaskId)
}

async function queryUnderstandingNow() {
  if (!selected.value?.materialId) return
  understandingResult.value = selected.value.materialType === 'VIDEO'
    ? await queryVideoUnderstanding(selected.value.materialId)
    : await queryMaterialUnderstanding(selected.value.materialId)
}

async function readVideoMetadataNow() {
  if (!selected.value?.materialId) return
  videoMetadata.value = await readVideoMetadata(selected.value.materialId)
}

async function extractFramesNow() {
  if (!selected.value?.materialId) return
  const frames = await extractVideoFrames(selected.value.materialId)
  operationMessage.value = `已抽取候选帧 ${frames.length} 张，可继续执行视频理解。`
}

async function extractAudioNow() {
  if (!selected.value?.materialId) return
  videoAudio.value = await extractVideoAudio(selected.value.materialId)
}

async function transcribeAudioNow() {
  if (!selected.value?.materialId) return
  videoAudio.value = await transcribeVideoAudio(selected.value.materialId)
  subtitles.value = await listVideoSubtitles(selected.value.materialId).catch(() => [])
}

async function understandVideoNow() {
  if (!selected.value?.materialId) return
  understandingResult.value = await understandVideo(selected.value.materialId, store.currentRunId, '请结合画面、音频和字幕理解这个视频')
}

onMounted(async () => {
  await loadWorkspace(store)
})
</script>
