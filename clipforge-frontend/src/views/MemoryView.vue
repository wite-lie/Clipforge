<template>
  <div class="cf-grid" style="gap: 20px">
    <SectionCard title="记忆中心" desc="按当前项目和会话读取偏好、项目记忆和创作历史摘要。">
      <div class="cf-form-inline">
        <el-form-item label="项目">
          <el-select v-model="store.selectedProjectId" filterable placeholder="选择项目" @change="onProjectChange">
            <el-option v-for="item in store.projects" :key="item.projectId" :label="item.name || '未命名项目'" :value="item.projectId" />
          </el-select>
        </el-form-item>
        <el-form-item label="会话">
          <el-select v-model="store.selectedConversationId" filterable placeholder="选择会话" @change="onConversationChange">
            <el-option v-for="item in store.conversations" :key="item.conversationId" :label="item.title || '未命名会话'" :value="item.conversationId" />
          </el-select>
        </el-form-item>
        <el-space>
          <el-button type="primary" @click="loadAll">查询记忆</el-button>
          <el-button @click="reloadWorkspace">刷新项目会话</el-button>
        </el-space>
      </div>
    </SectionCard>

    <section class="cf-grid cf-grid-3">
      <SectionCard title="个人偏好">
        <el-empty v-if="!userPreferences.length" description="暂无个人偏好记忆" />
        <div v-else class="cf-list">
          <div v-for="item in userPreferences" :key="item.memoryId" class="cf-list-item">
            <div><strong>{{ item.key }}</strong></div>
            <div class="cf-subtitle">{{ item.value }}</div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="项目记忆">
        <el-empty v-if="!projectMemories.length" description="暂无项目记忆" />
        <div v-else class="cf-list">
          <div v-for="item in projectMemories" :key="item.memoryId" class="cf-list-item">
            <div><strong>{{ item.key }}</strong></div>
            <div class="cf-subtitle">{{ item.value }}</div>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="创作历史">
        <el-empty v-if="!creationHistories.length" description="暂无创作历史摘要" />
        <div v-else class="cf-list">
          <div v-for="item in creationHistories" :key="item.memoryId" class="cf-list-item">
            <div><strong>{{ item.summary || '历史摘要' }}</strong></div>
            <div class="cf-subtitle">{{ (item.keyPoints || []).join(' / ') }}</div>
          </div>
        </div>
      </SectionCard>
    </section>

    <SectionCard title="记忆调试输出" desc="只用于联调观察后端返回结构。">
      <InfoSummary :rows="memorySummaryRows" />
    </SectionCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import SectionCard from '@/components/common/SectionCard.vue'
import InfoSummary from '@/components/common/InfoSummary.vue'
import { listCreationHistories, listProjectMemories, listUserPreferences } from '@/api/memory'
import { loadWorkspace } from '@/services/workspaceBootstrap'
import { useWorkspaceStore } from '@/stores/workspace'
import type { CreationHistoryMemoryResponse, ProjectMemoryResponse, UserPreferenceMemoryResponse } from '@/types/domain'
import { DEMO } from '@/utils/demo'

const store = useWorkspaceStore()
const userPreferences = ref<UserPreferenceMemoryResponse[]>([])
const projectMemories = ref<ProjectMemoryResponse[]>([])
const creationHistories = ref<CreationHistoryMemoryResponse[]>([])

const memorySummaryRows = computed(() => [
  { label: '个人偏好数量', value: userPreferences.value.length },
  { label: '项目记忆数量', value: projectMemories.value.length },
  { label: '历史摘要数量', value: creationHistories.value.length },
  { label: '当前会话', value: store.conversations.find((item) => item.conversationId === store.selectedConversationId)?.title || '-' }
])

async function reloadWorkspace() {
  await loadWorkspace(store)
  await loadAll()
}

async function onProjectChange() {
  store.persistSelection()
  await reloadWorkspace()
}

async function onConversationChange() {
  store.persistSelection()
  await loadAll()
}

async function loadAll() {
  userPreferences.value = await listUserPreferences(DEMO.userId).catch(() => [])
  projectMemories.value = store.selectedProjectId ? await listProjectMemories(store.selectedProjectId).catch(() => []) : []
  creationHistories.value = store.selectedConversationId ? await listCreationHistories(store.selectedConversationId).catch(() => []) : []
}

onMounted(() => {
  void reloadWorkspace()
})
</script>
