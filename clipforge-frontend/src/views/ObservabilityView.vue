<template>
  <div class="cf-grid" style="gap: 20px">
    <SectionCard title="观测面板" desc="查看最近一次创作运行的 Trace、模型调用、质量复盘和用量统计。">
      <div class="cf-toolbar">
        <el-space wrap>
          <el-select v-model="selectedRunId" placeholder="选择最近运行" style="width: 260px">
            <el-option v-for="(runId, index) in store.runIds" :key="runId" :label="runLabel(runId, index)" :value="runId" />
          </el-select>
          <el-button type="primary" @click="loadAll">查询</el-button>
          <el-button @click="useCurrentRun">使用最近一次运行</el-button>
        </el-space>
      </div>
    </SectionCard>

    <section class="cf-grid cf-grid-3">
      <SectionCard title="执行概览">
        <div class="cf-list">
          <div class="cf-list-item"><div class="cf-muted">输入 Token</div><div>{{ quality?.totalPromptTokens ?? modelCalls?.totalPromptTokens ?? '-' }}</div></div>
          <div class="cf-list-item"><div class="cf-muted">输出 Token</div><div>{{ quality?.totalCompletionTokens ?? modelCalls?.totalCompletionTokens ?? '-' }}</div></div>
          <div class="cf-list-item"><div class="cf-muted">总 Token</div><div>{{ quality?.totalTokens ?? modelCalls?.totalTokens ?? '-' }}</div></div>
          <div class="cf-list-item"><div class="cf-muted">最终评分</div><div>{{ quality?.finalScore ?? '-' }}</div></div>
        </div>
      </SectionCard>

      <SectionCard title="成本与版本">
        <div class="cf-list">
          <div class="cf-list-item"><div class="cf-muted">币种</div><div>{{ quality?.currency ?? '-' }}</div></div>
          <div class="cf-list-item"><div class="cf-muted">估算成本</div><div>{{ quality?.estimatedTotalCost ?? '-' }}</div></div>
          <div class="cf-list-item"><div class="cf-muted">最终版本</div><div>{{ quality?.finalVersionId ? '已生成' : '-' }}</div></div>
        </div>
      </SectionCard>

      <SectionCard title="模型用量">
        <div ref="chartRef" style="width: 100%; height: 220px" />
      </SectionCard>
    </section>

    <section class="cf-grid cf-grid-2">
      <SectionCard title="Trace 事件">
        <el-empty v-if="!(trace?.traceEvents || []).length" description="暂无 Trace" />
        <div v-else class="cf-list">
          <div v-for="(item, index) in trace?.traceEvents || []" :key="index" class="cf-list-item">
            <strong>{{ item.eventType }}</strong>
            <div class="cf-subtitle">{{ item.message }}</div>
            <div class="cf-muted">{{ item.detail }}</div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="模型调用明细">
        <el-table :data="modelCalls?.records || []" size="small" stripe>
          <el-table-column prop="purpose" label="用途" width="160" />
          <el-table-column prop="provider" label="供应商" width="110" />
          <el-table-column prop="modelName" label="模型" />
          <el-table-column prop="totalTokens" label="Token" width="100" />
          <el-table-column prop="latencyMs" label="耗时" width="100" />
        </el-table>
      </SectionCard>
    </section>

    <SectionCard title="质量复盘详情">
      <InfoSummary :rows="qualityRows" :sections="qualitySections" />
    </SectionCard>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import SectionCard from '@/components/common/SectionCard.vue'
import InfoSummary from '@/components/common/InfoSummary.vue'
import { useWorkspaceStore } from '@/stores/workspace'
import { queryModelCalls, queryQualityReport, queryTrace } from '@/api/observability'
import type { QueryModelCallRecordsResponse, QueryQualityReportResponse, QueryTraceResponse } from '@/types/domain'

const store = useWorkspaceStore()
const selectedRunId = ref('')
const trace = ref<QueryTraceResponse>()
const modelCalls = ref<QueryModelCallRecordsResponse>()
const quality = ref<QueryQualityReportResponse>()
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined

const qualityRows = computed(() => [
  { label: '最终评分', value: quality.value?.finalScore ?? '-' },
  { label: '是否触发评估', value: quality.value?.evaluationTriggered },
  { label: '是否触发修正', value: quality.value?.revisionTriggered },
  { label: '修正次数', value: quality.value?.revisionCount ?? 0 },
  { label: '总 Token', value: quality.value?.totalTokens ?? modelCalls.value?.totalTokens ?? '-' },
  { label: '估算成本', value: quality.value?.estimatedTotalCost ?? '-' }
])

const qualitySections = computed(() => [
  {
    title: '评估结论',
    items: (quality.value?.evaluations || []).map((item) => {
      const score = item.score == null ? '-' : item.score
      return `评分 ${score}：${item.summary || '暂无摘要'}`
    })
  },
  {
    title: '修正记录',
    items: (quality.value?.revisions || []).map((item) => item.reason || '已生成一次修正')
  },
  {
    title: '模型调用概览',
    items: (modelCalls.value?.records || []).map((item) => `${item.purpose || '模型调用'}：${item.modelName || '-'}，Token ${item.totalTokens ?? '-'}`)
  }
])

function runLabel(runId: string, index: number) {
  return runId === store.currentRunId || index === 0 ? '最近一次运行' : `历史运行 ${index + 1}`
}

function useCurrentRun() {
  selectedRunId.value = store.currentRunId || store.runIds[0] || ''
}

async function loadAll() {
  if (!selectedRunId.value) return
  trace.value = await queryTrace(selectedRunId.value).catch(() => undefined)
  modelCalls.value = await queryModelCalls(selectedRunId.value).catch(() => undefined)
  quality.value = await queryQualityReport(selectedRunId.value).catch(() => undefined)
  await nextTick()
  renderChart()
}

function renderChart() {
  if (!chartRef.value) return
  chart ||= echarts.init(chartRef.value)
  const map = quality.value?.tokensByPurpose || {}
  const names = Object.keys(map)
  const values = names.map((key) => map[key] || 0)
  chart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: names, axisLabel: { color: '#c7d8ee' } },
    yAxis: { type: 'value', axisLabel: { color: '#c7d8ee' } },
    series: [{ type: 'bar', data: values, itemStyle: { color: '#6ae4ff' } }]
  })
}

watch(() => quality.value, () => void nextTick().then(renderChart))

onMounted(() => {
  store.hydrateSelection()
  useCurrentRun()
  void loadAll()
})

onBeforeUnmount(() => {
  chart?.dispose()
})
</script>
