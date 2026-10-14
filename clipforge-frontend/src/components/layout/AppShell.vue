<template>
  <div class="cf-shell">
    <SideNav />
    <div class="cf-workspace">
      <TopBar />
      <main class="cf-page">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import TopBar from './TopBar.vue'
import SideNav from './SideNav.vue'
</script>
