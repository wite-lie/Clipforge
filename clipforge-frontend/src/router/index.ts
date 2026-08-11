import { createRouter, createWebHashHistory } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
import HomeView from '@/views/HomeView.vue'
import StudioView from '@/views/StudioView.vue'
import MaterialsView from '@/views/MaterialsView.vue'
import KnowledgeView from '@/views/KnowledgeView.vue'
import MemoryView from '@/views/MemoryView.vue'
import ObservabilityView from '@/views/ObservabilityView.vue'
import NotFoundView from '@/views/NotFoundView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      component: AppShell,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'studio', name: 'studio', component: StudioView },
        { path: 'materials', name: 'materials', component: MaterialsView },
        { path: 'knowledge', name: 'knowledge', component: KnowledgeView },
        { path: 'memory', name: 'memory', component: MemoryView },
        { path: 'observability', name: 'observability', component: ObservabilityView },
        { path: ':pathMatch(.*)*', name: 'not-found', component: NotFoundView }
      ]
    }
  ]
})

export default router
