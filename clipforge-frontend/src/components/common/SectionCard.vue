<template>
  <el-card class="cf-card" shadow="never">
    <template #header>
      <div class="cf-panel-header">
        <div>
          <h3 class="cf-title">{{ title }}</h3>
          <p v-if="desc" class="cf-subtitle">{{ desc }}</p>
        </div>
        <div><slot name="actions" /></div>
      </div>
    </template>
    <div class="cf-panel-body">
      <slot />
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  desc?: string
}>()
</script>
