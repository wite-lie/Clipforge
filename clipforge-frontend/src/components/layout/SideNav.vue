<template>
  <aside class="side-nav">
    <div class="brand-lockup">
      <div class="brand-mark">C</div>
      <div>
        <div class="brand-name">ClipForge</div>
        <div class="brand-caption">内容创作工作台</div>
      </div>
    </div>
    <button class="create-entry" @click="router.push('/studio')"><el-icon><Plus /></el-icon><span>开始创作</span></button>
    <nav class="nav-section" aria-label="主导航">
      <div class="nav-label">工作区</div>
      <button v-for="item in primaryItems" :key="item.path" class="nav-item" :class="{ active: route.path === item.path }" @click="router.push(item.path)">
        <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
      </button>
    </nav>
    <nav class="nav-section secondary" aria-label="资产导航">
      <div class="nav-label">资产与洞察</div>
      <button v-for="item in assetItems" :key="item.path" class="nav-item" :class="{ active: route.path === item.path }" @click="router.push(item.path)">
        <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
      </button>
    </nav>
    <div class="side-nav-footer">
      <button class="nav-item feedback" @click="feedbackVisible = !feedbackVisible"><el-icon><ChatDotRound /></el-icon><span>反馈建议</span></button>
      <div v-if="feedbackVisible" class="feedback-note">欢迎告诉我们哪里还可以更好。</div>
      <div class="user-chip">
        <div class="user-avatar">C</div>
        <div class="user-copy"><strong>本地工作区</strong><span>演示环境</span></div>
        <el-icon><MoreFilled /></el-icon>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, ChatLineRound, Collection, Document, Files, House, MoreFilled, Plus, TrendCharts } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const feedbackVisible = ref(false)
const primaryItems = [
  { path: '/', label: '首页', icon: House },
  { path: '/studio', label: '创作空间', icon: ChatLineRound },
  { path: '/materials', label: '素材中心', icon: Files }
]
const assetItems = [
  { path: '/knowledge', label: '知识库', icon: Collection },
  { path: '/memory', label: '记忆', icon: Document },
  { path: '/observability', label: '运行观测', icon: TrendCharts }
]
</script>

<style scoped>
.side-nav{width:238px;min-width:238px;min-height:100vh;display:flex;flex-direction:column;padding:22px 14px 16px;background:var(--cf-sidebar);border-right:1px solid var(--cf-border)}
.brand-lockup{display:flex;align-items:center;gap:10px;padding:0 10px 25px}.brand-mark{width:32px;height:32px;display:grid;place-items:center;border-radius:10px;background:var(--cf-ink);color:#fff;font-size:18px;font-weight:700}.brand-name{color:var(--cf-ink);font-size:19px;font-weight:800;letter-spacing:-.02em}.brand-caption{margin-top:2px;color:var(--cf-text-muted);font-size:11px}
.create-entry,.nav-item{border:0;cursor:pointer;font:inherit}.create-entry{height:42px;display:flex;align-items:center;justify-content:center;gap:8px;margin:0 4px 25px;border:1px solid var(--cf-border-strong);border-radius:9px;background:#fff;color:var(--cf-ink);font-size:14px;font-weight:700;transition:160ms ease}.create-entry:hover{border-color:var(--cf-green);color:var(--cf-green-deep);box-shadow:0 6px 18px rgba(39,107,87,.1)}
.nav-section{display:grid;gap:4px}.nav-section.secondary{margin-top:25px}.nav-label{padding:0 12px 8px;color:var(--cf-text-faint);font-size:11px;font-weight:700;letter-spacing:.08em;text-transform:uppercase}.nav-item{min-height:40px;display:flex;align-items:center;gap:11px;padding:0 12px;border-radius:8px;background:transparent;color:var(--cf-text-muted);text-align:left;font-size:14px;transition:160ms ease}.nav-item:hover,.nav-item.active{background:var(--cf-green-pale);color:var(--cf-green-deep)}.nav-item.active{font-weight:700}.nav-item .el-icon{font-size:17px}
.side-nav-footer{margin-top:auto}.feedback{width:100%}.feedback-note{margin:7px 10px 12px;padding:9px 10px;border-radius:7px;background:#f5f8f6;color:var(--cf-text-muted);font-size:12px;line-height:1.5}.user-chip{display:flex;align-items:center;gap:9px;margin-top:13px;padding:12px 9px 0;border-top:1px solid var(--cf-border)}.user-avatar,.top-avatar{display:grid;place-items:center;border-radius:50%;background:var(--cf-ink);color:#fff;font-size:12px;font-weight:700}.user-avatar{width:30px;height:30px}.user-copy{min-width:0;flex:1;display:grid;gap:2px}.user-copy strong{color:var(--cf-ink);font-size:12px}.user-copy span{color:var(--cf-text-faint);font-size:11px}.user-chip>.el-icon{color:var(--cf-text-faint)}

@media(max-width:900px){
  .side-nav{width:70px;min-width:70px;padding:18px 8px 12px}
  .brand-lockup{justify-content:center;padding:0 0 22px}
  .brand-lockup>div:last-child,.create-entry span,.nav-label,.nav-item span,.user-copy,.user-chip>.el-icon,.feedback-note{display:none}
  .create-entry{margin:0 0 22px}
  .nav-item{justify-content:center;padding:0}
  .user-chip{justify-content:center;padding-left:0;padding-right:0}
}
</style>
