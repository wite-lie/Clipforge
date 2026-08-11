<template>
  <div class="cf-page">
    <SectionCard title="页面不存在" desc="你访问的路由没有找到。">
      <el-empty description="404" />
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </SectionCard>
  </div>
</template>

<script setup lang="ts">
import SectionCard from '@/components/common/SectionCard.vue'
</script>
