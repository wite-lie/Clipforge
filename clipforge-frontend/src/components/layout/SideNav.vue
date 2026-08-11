<template>
  <el-menu
    class="side-nav"
    :default-active="route.path"
    router
    background-color="transparent"
    text-color="#dce9fb"
    active-text-color="#06111f"
  >
    <el-menu-item index="/">
      <el-icon><House /></el-icon>
      <span>演示首页</span>
    </el-menu-item>
    <el-menu-item index="/studio">
      <el-icon><ChatLineRound /></el-icon>
      <span>项目会话</span>
    </el-menu-item>
    <el-menu-item index="/materials">
      <el-icon><Files /></el-icon>
      <span>素材中心</span>
    </el-menu-item>
    <el-menu-item index="/knowledge">
      <el-icon><Collection /></el-icon>
      <span>知识库</span>
    </el-menu-item>
    <el-menu-item index="/memory">
      <el-icon><Clock /></el-icon>
      <span>记忆</span>
    </el-menu-item>
    <el-menu-item index="/observability">
      <el-icon><TrendCharts /></el-icon>
      <span>观测面板</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ChatLineRound, Clock, Collection, Files, House, TrendCharts } from '@element-plus/icons-vue'

const route = useRoute()
</script>

<style scoped>
.side-nav {
  border-right: 0;
  min-height: calc(100vh - 82px);
  padding: 12px;
}

.side-nav :deep(.el-menu-item) {
  border-radius: 8px;
  height: 46px;
  margin-bottom: 6px;
  font-size: 15px;
}

.side-nav :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #6ae4ff, #9ae6b4);
  color: #06111f;
  font-weight: 700;
}
</style>
