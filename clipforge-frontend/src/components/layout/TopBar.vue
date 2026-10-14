<template>
  <header class="cf-topbar">
    <div class="topbar-inner">
      <div class="breadcrumb"><span>ClipForge</span><el-icon><ArrowRight /></el-icon><strong>{{ pageTitle }}</strong></div>
      <div class="top-actions"><span class="status-dot"><i /> 服务正常</span><button class="icon-action" title="刷新页面" @click="reload"><el-icon><Refresh /></el-icon></button><div class="top-avatar">C</div></div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const pageTitle = computed(() => ({
  '/': '首页',
  '/studio': '创作空间',
  '/materials': '素材中心',
  '/knowledge': '知识库',
  '/memory': '记忆',
  '/observability': '运行观测'
}[route.path] || '工作区'))

function reload() { window.location.reload() }
</script>

<style scoped>
.topbar-inner{min-height:62px;display:flex;align-items:center;justify-content:space-between;padding:0 32px;border-bottom:1px solid var(--cf-border);background:rgba(255,255,255,.86)}.breadcrumb{display:flex;align-items:center;gap:8px;color:var(--cf-text-faint);font-size:13px}.breadcrumb strong{color:var(--cf-ink);font-weight:700}.top-actions{display:flex;align-items:center;gap:14px}.status-dot{display:inline-flex;align-items:center;gap:6px;color:var(--cf-text-muted);font-size:12px}.status-dot i{width:7px;height:7px;border-radius:50%;background:#4cae86;box-shadow:0 0 0 3px rgba(76,174,134,.12)}.icon-action{border:0;background:transparent;color:var(--cf-text-muted);cursor:pointer;font-size:17px}.top-avatar{width:30px;height:30px;display:grid;place-items:center;border-radius:50%;background:var(--cf-green-pale);color:var(--cf-green-deep);font-size:12px;font-weight:800}

@media(max-width:900px){
  .topbar-inner{padding:0 16px}
  .status-dot{display:none}
}
</style>
