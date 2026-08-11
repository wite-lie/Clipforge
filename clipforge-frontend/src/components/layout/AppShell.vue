<template>
  <div class="cf-shell">
    <TopBar />
    <el-container class="cf-main">
      <el-aside width="230px" class="border-r border-[rgba(148,163,184,0.18)]">
        <SideNav />
      </el-aside>
      <el-main class="cf-page">
        <RouterView />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import TopBar from './TopBar.vue'
import SideNav from './SideNav.vue'
</script>
