<template>
  <span class="status-pill" :class="variantClass">{{ text }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text?: string
  status?: string
}>()

const variantClass = computed(() => {
  const value = (props.status || props.text || '').toUpperCase()
  if (['SUCCEEDED', 'SUCCESS', 'READY_FOR_CREATION', 'ACTIVE'].includes(value)) return 'is-success'
  if (['FAILED', 'ERROR', 'DISABLED', 'ARCHIVED'].includes(value)) return 'is-danger'
  if (['PROCESSING', 'PENDING', 'UPLOADED', 'WAITING'].includes(value)) return 'is-warn'
  return 'is-neutral'
})

const text = computed(() => props.text || props.status || '-')
</script>

<style scoped>
.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  border: 1px solid transparent;
}
.is-success { color: #61e2b5; background: rgba(97, 226, 181, 0.12); border-color: rgba(97, 226, 181, 0.2); }
.is-danger { color: #ff8d8d; background: rgba(255, 141, 141, 0.12); border-color: rgba(255, 141, 141, 0.2); }
.is-warn { color: #ffd56a; background: rgba(255, 213, 106, 0.12); border-color: rgba(255, 213, 106, 0.2); }
.is-neutral { color: #c8d7ec; background: rgba(200, 215, 236, 0.08); border-color: rgba(200, 215, 236, 0.15); }
</style>
