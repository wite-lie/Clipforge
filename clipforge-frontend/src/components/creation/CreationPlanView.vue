<template>
  <el-empty v-if="!plan" description="暂无创作方案" />
  <div v-else class="plan-view">
    <InfoSummary :rows="summaryRows" />

    <div class="plan-block" v-if="plan.hook">
      <div class="plan-title">开头钩子</div>
      <p>{{ plan.hook }}</p>
    </div>

    <div class="plan-block" v-if="plan.storyboard?.length">
      <div class="plan-title">分镜脚本</div>
      <ol>
        <li v-for="(item, index) in plan.storyboard" :key="index">{{ item }}</li>
      </ol>
    </div>

    <div class="plan-block" v-if="plan.voiceoverScript">
      <div class="plan-title">旁白脚本</div>
      <p>{{ plan.voiceoverScript }}</p>
    </div>

    <div class="plan-block" v-if="plan.subtitleSuggestions?.length">
      <div class="plan-title">字幕建议</div>
      <div class="tag-list">
        <span v-for="item in plan.subtitleSuggestions" :key="item">{{ item }}</span>
      </div>
    </div>

    <div class="plan-block" v-if="plan.titleCandidates?.length">
      <div class="plan-title">标题候选</div>
      <ol>
        <li v-for="item in plan.titleCandidates" :key="item">{{ item }}</li>
      </ol>
    </div>

    <div class="plan-block" v-if="plan.publishCopy">
      <div class="plan-title">发布文案</div>
      <p>{{ plan.publishCopy }}</p>
    </div>

    <div class="plan-block" v-if="plan.hashtags?.length">
      <div class="plan-title">话题标签</div>
      <div class="tag-list">
        <span v-for="item in plan.hashtags" :key="item">{{ item }}</span>
      </div>
    </div>

    <div class="plan-block" v-if="plan.improvementSuggestions?.length">
      <div class="plan-title">改进建议</div>
      <ol>
        <li v-for="item in plan.improvementSuggestions" :key="item">{{ item }}</li>
      </ol>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import InfoSummary from '@/components/common/InfoSummary.vue'
import type { CreationPlanResult } from '@/types/api'

const props = defineProps<{
  plan?: CreationPlanResult
}>()

const summaryRows = computed(() => [
  { label: '主题', value: props.plan?.theme },
  { label: '目标受众', value: props.plan?.targetAudience },
  { label: '方案评分', value: props.plan?.score ?? '-' }
])
</script>

<style scoped>
.plan-view {
  display: grid;
  gap: 14px;
}

.plan-block {
  padding: 14px;
  border-radius: 8px;
  background: rgba(8, 15, 28, 0.68);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.plan-title {
  margin-bottom: 8px;
  color: #f6f9ff;
  font-weight: 750;
}

.plan-block p,
.plan-block ol {
  margin: 0;
  color: #dbeafe;
  line-height: 1.85;
}

.plan-block ol {
  padding-left: 20px;
}

.plan-block li + li {
  margin-top: 6px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-list span {
  padding: 5px 9px;
  border-radius: 999px;
  color: #c7fff0;
  background: rgba(105, 224, 179, 0.12);
  border: 1px solid rgba(105, 224, 179, 0.24);
}
</style>
