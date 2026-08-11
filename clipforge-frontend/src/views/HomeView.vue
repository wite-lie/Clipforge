<template>
  <div class="cf-grid" style="gap: 20px">
    <section class="cf-hero">
      <div class="cf-hero-content">
        <div class="cf-hero-copy">
          <div class="cf-eyebrow">
            <el-icon><TrendCharts /></el-icon>
            虎鲸科普联调演示
          </div>
          <h1>从素材、知识库到创作方案，一次跑通短视频 Agent 流程</h1>
          <p>
            当前页面会读取后端数据库中的项目、会话、知识库、素材和最近一次创作结果。
            如果数据库为空，可以先初始化虎鲸演示数据，再进入项目会话页面发起真实创作。
          </p>

          <div class="cf-hero-actions">
            <el-button type="primary" size="large" :icon="UploadFilled" :loading="initializing" @click="initializeDemo">
              初始化虎鲸演示数据
            </el-button>
            <el-button size="large" :icon="VideoPlay" :loading="creating" @click="createDemoPlan">
              生成虎鲸创作方案
            </el-button>
            <el-button size="large" :icon="Compass" @click="$router.push('/studio')">
              进入项目会话
            </el-button>
          </div>

          <div class="cf-section cf-grid cf-grid-4">
            <div class="cf-hero-stat">
              <div class="cf-hero-stat-value">{{ store.projects.length }}</div>
              <div class="cf-hero-stat-label">项目</div>
            </div>
            <div class="cf-hero-stat">
              <div class="cf-hero-stat-value">{{ store.conversations.length }}</div>
              <div class="cf-hero-stat-label">会话</div>
            </div>
            <div class="cf-hero-stat">
              <div class="cf-hero-stat-value">{{ store.knowledgeBases.length }}</div>
              <div class="cf-hero-stat-label">知识库</div>
            </div>
            <div class="cf-hero-stat">
              <div class="cf-hero-stat-value">{{ store.materials.length }}</div>
              <div class="cf-hero-stat-label">素材</div>
            </div>
          </div>
        </div>

        <div class="cf-hero-aside">
          <el-card class="cf-card" shadow="never">
            <template #header>
              <div class="cf-panel-header compact">
                <div>
                  <h3 class="cf-title">当前工作区</h3>
                  <p class="cf-subtitle">用于本次联调和演示的项目上下文</p>
                </div>
              </div>
            </template>
            <div class="cf-list">
              <div class="cf-list-item">
                <div class="cf-muted">项目</div>
                <strong>{{ activeProjectName }}</strong>
              </div>
              <div class="cf-list-item">
                <div class="cf-muted">会话</div>
                <strong>{{ activeConversationName }}</strong>
              </div>
              <div class="cf-list-item">
                <div class="cf-muted">平台</div>
                <strong>{{ platformLabel(form.platform) }}</strong>
              </div>
              <div class="cf-list-item">
                <div class="cf-muted">创作状态</div>
                <strong>{{ store.currentRunId ? '已有最近一次结果' : '尚未生成' }}</strong>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </section>

    <section class="cf-grid cf-grid-2">
      <SectionCard title="演示输入" desc="这些输入会发送给后端创作接口，用户编号由前端工作上下文自动携带。">
        <div class="cf-form-inline">
          <el-form-item label="目标平台">
            <el-select v-model="form.platform" style="width: 100%">
              <el-option v-for="item in PLATFORM_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识库">
            <el-select v-model="store.selectedKnowledgeBaseIds" multiple collapse-tags style="width: 100%" @change="store.persistSelection()">
              <el-option
                v-for="item in store.knowledgeBases"
                :key="item.knowledgeBaseId"
                :label="item.name || '未命名知识库'"
                :value="item.knowledgeBaseId"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="创作目标">
          <el-input v-model="form.goal" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="风格偏好">
          <el-input v-model="form.stylePreference" type="textarea" :rows="3" />
        </el-form-item>

        <el-space wrap>
          <el-button :icon="Refresh" @click="reload">刷新后端数据</el-button>
          <el-button type="primary" :icon="Position" :loading="creating" @click="createDemoPlan">生成创作方案</el-button>
        </el-space>

        <el-alert v-if="store.lastError" :title="store.lastError" type="error" show-icon class="mt-4" />
      </SectionCard>

      <SectionCard title="后端素材候选" desc="这里展示数据库中当前会话下已经上传的素材。">
        <el-empty v-if="!store.materials.length" description="数据库中还没有素材，请先初始化演示数据。" />
        <div v-else class="cf-grid cf-grid-2">
          <el-card v-for="item in store.materials" :key="item.materialId" class="cf-card" shadow="never">
            <div class="material-head">
              <strong>{{ item.originalFilename || '未命名素材' }}</strong>
              <StatusPill :status="item.processStatus || item.status" :text="statusLabel(item.processStatus || item.status)" />
            </div>
            <div class="cf-subtitle">{{ materialTypeLabel(item.materialType) }} · {{ item.processPhase || '等待后端处理' }}</div>
            <el-progress :percentage="item.progressPercent ?? 0" :stroke-width="8" style="margin-top: 12px" />
          </el-card>
        </div>
      </SectionCard>
    </section>

    <section class="cf-grid cf-grid-2">
      <SectionCard title="最近创作方案" desc="生成后会展示后端返回的结构化方案。">
        <el-empty v-if="!store.currentPlan" description="尚未生成创作方案。" />
        <template v-else>
          <CreationPlanView :plan="store.currentPlan" />
        </template>
      </SectionCard>

      <SectionCard title="联调步骤" desc="适合空数据库重新走一遍流程。">
        <el-timeline>
          <el-timeline-item timestamp="第一步">点击“初始化虎鲸演示数据”，创建默认项目、会话、知识库并上传素材。</el-timeline-item>
          <el-timeline-item timestamp="第二步">等待后端异步处理素材和知识文档，必要时到素材中心和知识库页面查看状态。</el-timeline-item>
          <el-timeline-item timestamp="第三步">点击“生成虎鲸创作方案”，后端读取素材摘要、知识库证据和会话上下文。</el-timeline-item>
          <el-timeline-item timestamp="第四步">到观测面板查看最近一次运行的 Trace、模型调用、成本和质量复盘。</el-timeline-item>
        </el-timeline>
      </SectionCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Compass, Position, Refresh, TrendCharts, UploadFilled, VideoPlay } from '@element-plus/icons-vue'
import SectionCard from '@/components/common/SectionCard.vue'
import StatusPill from '@/components/common/StatusPill.vue'
import CreationPlanView from '@/components/creation/CreationPlanView.vue'
import { createPlan, queryModelCalls, queryQualityReport, queryTrace } from '@/api/creation'
import { ensureDemoWorkspace, loadWorkspace } from '@/services/workspaceBootstrap'
import { useWorkspaceStore } from '@/stores/workspace'
import { DEMO } from '@/utils/demo'
import { materialTypeLabel, PLATFORM_OPTIONS, platformLabel, statusLabel } from '@/utils/display'

const store = useWorkspaceStore()
const initializing = ref(false)
const creating = ref(false)

const form = reactive({
  platform: DEMO.platform,
  goal: DEMO.goal,
  stylePreference: DEMO.stylePreference
})

const activeProjectName = computed(() => {
  return store.projects.find((item) => item.projectId === store.selectedProjectId)?.name || '尚未创建项目'
})

const activeConversationName = computed(() => {
  return store.conversations.find((item) => item.conversationId === store.selectedConversationId)?.title || '尚未创建会话'
})

async function reload() {
  try {
    await loadWorkspace(store)
  } catch (error) {
    store.setLastError(error instanceof Error ? error.message : '刷新后端数据失败')
  }
}

async function initializeDemo() {
  initializing.value = true
  try {
    store.setLastError(undefined)
    const result = await ensureDemoWorkspace(store)
    ElMessage.success(`初始化完成：新增素材 ${result.materialUploadCount} 个`)
  } catch (error) {
    store.setLastError(error instanceof Error ? error.message : '初始化演示数据失败')
    ElMessage.error('初始化演示数据失败')
  } finally {
    initializing.value = false
  }
}

async function createDemoPlan() {
  creating.value = true
  try {
    store.setLastError(undefined)
    if (!store.selectedProjectId || !store.selectedConversationId || !store.materials.length) {
      await ensureDemoWorkspace(store)
    }

    const response = await createPlan({
      userId: DEMO.userId,
      projectId: store.selectedProjectId,
      conversationId: store.selectedConversationId,
      goal: form.goal,
      platform: form.platform,
      stylePreference: form.stylePreference,
      materialIds: store.selectedMaterialIds,
      knowledgeBaseIds: store.selectedKnowledgeBaseIds
    })

    store.setCurrentRun(response.runId)
    store.setCurrentPlan(response.result)
    store.setTraceEvents(response.traceEvents || [])

    if (response.runId) {
      const [quality, traces, calls] = await Promise.allSettled([
        queryQualityReport(response.runId),
        queryTrace(response.runId),
        queryModelCalls(response.runId)
      ])
      if (quality.status === 'fulfilled') store.setQualityReport(quality.value)
      if (traces.status === 'fulfilled') store.setTraceEvents(traces.value.traceEvents || [])
      if (calls.status === 'fulfilled') store.setModelCallRecords(calls.value.records || [])
    }

    ElMessage.success('创作方案已生成')
  } catch (error) {
    store.setLastError(error instanceof Error ? error.message : '生成创作方案失败')
    ElMessage.error('生成创作方案失败')
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  void reload()
})
</script>

<style scoped>
.compact {
  padding: 0;
  border: 0;
}

.material-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
</style>
