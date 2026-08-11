<template>
  <pre class="cf-json">{{ text }}</pre>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { safeJson } from '@/utils/format'

const props = defineProps<{ value?: unknown }>()
const text = computed(() => {
  if (typeof props.value === 'string') return safeJson(props.value)
  if (props.value === undefined) return ''
  return JSON.stringify(props.value, null, 2)
})
</script>
