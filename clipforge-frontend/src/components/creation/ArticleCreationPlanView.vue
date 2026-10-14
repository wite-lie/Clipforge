<template>
  <el-empty v-if="!article" description="暂无图文方案" />
  <div v-else class="article-view">
    <InfoSummary :rows="summaryRows" />

    <div class="article-block" v-if="article.titleCandidates?.length">
      <div class="article-title">标题候选</div>
      <ol>
        <li v-for="item in article.titleCandidates" :key="item">{{ item }}</li>
      </ol>
    </div>

    <div class="article-block" v-if="article.summary">
      <div class="article-title">摘要</div>
      <p>{{ article.summary }}</p>
    </div>

    <div class="article-block" v-if="article.lead">
      <div class="article-title">导语</div>
      <p>{{ article.lead }}</p>
    </div>

    <div class="article-block" v-if="article.sections?.length">
      <div class="article-title">正文结构</div>
      <section v-for="(section, index) in article.sections" :key="`${section.sectionTitle}-${index}`" class="article-section">
        <h4>{{ section.sectionTitle || `第 ${index + 1} 段` }}</h4>
        <p>{{ section.body }}</p>
        <div v-if="section.keyPoints?.length" class="mini-list">
          <span v-for="point in section.keyPoints" :key="point">{{ point }}</span>
        </div>
        <div v-if="section.suggestedMaterialIds?.length" class="material-hints">
          建议素材：{{ section.suggestedMaterialIds.join('、') }}
        </div>
        <div v-if="section.imageSlots?.length" class="image-slot-list">
          <div v-for="slot in section.imageSlots" :key="slot.slotId || slot.placement" class="image-slot">
            <div class="image-slot-head">
              <span>{{ slot.placement || '配图位置' }}</span>
              <small>{{ sourceTypeLabel(slot.sourceType) }} · {{ slot.status || 'SUGGESTED' }}</small>
            </div>
            <p>{{ slot.reason || slot.imagePrompt || '暂无配图说明' }}</p>
            <div v-if="slot.recommendedMaterialIds?.length" class="material-hints">推荐素材：{{ slot.recommendedMaterialIds.join('、') }}</div>
            <div v-if="slot.boundMaterialId" class="material-hints">已绑定：{{ slot.boundMaterialId }}</div>
          </div>
        </div>
        <div v-if="section.sourceEvidence?.length" class="evidence-list">
          <div v-for="(evidence, evidenceIndex) in section.sourceEvidence" :key="evidenceIndex">
            {{ evidence.sourceType || '依据' }}：{{ evidence.quote || evidence.sourceId || '暂无内容' }}
          </div>
        </div>
      </section>
    </div>

    <div class="article-block" v-if="article.imageSlots?.length">
      <div class="article-title">配图规划</div>
      <div class="image-slot-list">
        <div v-for="slot in article.imageSlots" :key="slot.slotId || `${slot.sectionTitle}-${slot.placement}`" class="image-slot">
          <div class="image-slot-head">
            <span>{{ slot.placement || '配图位置' }}</span>
            <small>{{ sourceTypeLabel(slot.sourceType) }} · {{ slot.status || 'SUGGESTED' }}</small>
          </div>
          <p>{{ slot.reason || slot.imagePrompt || '暂无配图说明' }}</p>
          <div v-if="slot.sectionTitle" class="material-hints">对应位置：{{ slot.sectionTitle }}</div>
          <div v-if="slot.recommendedMaterialIds?.length" class="material-hints">推荐素材：{{ slot.recommendedMaterialIds.join('、') }}</div>
          <div v-if="slot.boundMaterialId" class="material-hints">已绑定：{{ slot.boundMaterialId }}</div>
          <div v-if="slot.imagePrompt" class="material-hints">配图提示：{{ slot.imagePrompt }}</div>
        </div>
      </div>
    </div>

    <div class="article-block" v-if="article.conclusion">
      <div class="article-title">结尾</div>
      <p>{{ article.conclusion }}</p>
    </div>

    <div class="article-block" v-if="article.interactionPrompt">
      <div class="article-title">互动引导</div>
      <p>{{ article.interactionPrompt }}</p>
    </div>

    <div class="article-block" v-if="article.publishCopy">
      <div class="article-title">发布文案</div>
      <p>{{ article.publishCopy }}</p>
    </div>

    <div class="article-block" v-if="article.hashtags?.length">
      <div class="article-title">话题标签</div>
      <div class="tag-list">
        <span v-for="item in article.hashtags" :key="item">{{ item }}</span>
      </div>
    </div>

    <div class="article-block" v-if="article.riskWarnings?.length">
      <div class="article-title">风险提示</div>
      <ul>
        <li v-for="item in article.riskWarnings" :key="item">{{ item }}</li>
      </ul>
    </div>

    <div class="article-block" v-if="article.improvementSuggestions?.length">
      <div class="article-title">改进建议</div>
      <ul>
        <li v-for="item in article.improvementSuggestions" :key="item">{{ item }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import InfoSummary from '@/components/common/InfoSummary.vue'
import type { ArticleCreationPlan } from '@/types/api'

const props = defineProps<{
  article?: ArticleCreationPlan
}>()

const summaryRows = computed(() => [
  { label: '主题', value: props.article?.theme },
  { label: '目标受众', value: props.article?.targetAudience },
  { label: '方案评分', value: props.article?.score ?? '-' }
])

function sourceTypeLabel(value?: string) {
  const map: Record<string, string> = {
    EXISTING_MATERIAL: '已有素材',
    USER_UPLOAD: '用户补图',
    MODEL_GENERATED: '模型生成',
    WEB_SEARCH: '网络搜图',
    NONE: '无需配图'
  }
  return value ? map[value] || value : '未指定'
}
</script>

<style scoped>
.article-view {
  display: grid;
  gap: 14px;
}

.article-block {
  padding: 14px;
  border-radius: 8px;
  background: #fbfdfb;
  border: 1px solid var(--cf-border);
}

.article-title {
  margin-bottom: 8px;
  color: var(--cf-ink);
  font-weight: 750;
}

.article-block p,
.article-block ol,
.article-block ul {
  margin: 0;
  color: var(--cf-text);
  line-height: 1.85;
}

.article-block ol,
.article-block ul {
  padding-left: 20px;
}

.article-block li + li {
  margin-top: 6px;
}

.article-section {
  padding: 12px 0;
  border-top: 1px solid var(--cf-border);
}

.article-section:first-of-type {
  border-top: 0;
  padding-top: 0;
}

.article-section h4 {
  margin: 0 0 8px;
  color: var(--cf-ink);
  font-size: 15px;
}

.mini-list,
.tag-list,
.image-slot-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.mini-list span,
.tag-list span {
  padding: 5px 9px;
  border-radius: 999px;
  color: var(--cf-green-deep);
  background: var(--cf-green-pale);
  border: 1px solid #cfe4d5;
}

.material-hints,
.evidence-list {
  margin-top: 10px;
  color: var(--cf-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.image-slot-list {
  display: grid;
}

.image-slot {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #d8e8dc;
  background: #f3f8f4;
}

.image-slot p {
  margin-top: 8px;
}

.image-slot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--cf-ink);
  font-weight: 700;
}

.image-slot-head small {
  color: var(--cf-green);
  font-weight: 500;
}
</style>
