<template>
  <div class="info-summary">
    <div v-if="rows.length" class="info-grid">
      <div v-for="row in rows" :key="row.label" class="info-cell">
        <div class="info-label">{{ row.label }}</div>
        <div class="info-value">
          <slot :name="`value-${row.label}`" :row="row">
            {{ displayValue(row.value) }}
          </slot>
        </div>
      </div>
    </div>

    <div v-for="section in visibleSections" :key="section.title" class="info-section">
      <div class="info-section-title">{{ section.title }}</div>
      <p v-if="section.text" class="info-text">{{ section.text }}</p>
      <ol v-if="section.items?.length" class="info-list">
        <li v-for="(item, index) in section.items" :key="index">{{ item }}</li>
      </ol>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface InfoRow {
  label: string
  value?: string | number | boolean | null
}

export interface InfoSection {
  title: string
  text?: string
  items?: Array<string | number>
}

const props = defineProps<{
  rows?: InfoRow[]
  sections?: InfoSection[]
}>()

const rows = computed(() => props.rows || [])
const visibleSections = computed(() => (props.sections || []).filter((item) => item.text || item.items?.length))

function displayValue(value: InfoRow['value']) {
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'boolean') return value ? '是' : '否'
  return String(value)
}
</script>

<style scoped>
.info-summary {
  display: grid;
  gap: 14px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-cell {
  padding: 12px;
  border-radius: 8px;
  background: rgba(8, 15, 28, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.info-label {
  margin-bottom: 6px;
  color: #9fb8d8;
  font-size: 12px;
}

.info-value {
  color: #f4f8ff;
  font-weight: 650;
  line-height: 1.6;
  word-break: break-word;
}

.info-section {
  padding: 14px;
  border-radius: 8px;
  background: rgba(8, 15, 28, 0.64);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.info-section-title {
  margin-bottom: 8px;
  color: #f6f9ff;
  font-size: 15px;
  font-weight: 700;
}

.info-text,
.info-list {
  margin: 0;
  color: #dbeafe;
  line-height: 1.8;
}

.info-list {
  padding-left: 20px;
}

.info-list li + li {
  margin-top: 6px;
}
</style>
