import { request } from './http'
import type { CreationHistoryMemoryResponse, ProjectMemoryResponse, UserPreferenceMemoryResponse } from '@/types/domain'

export function listUserPreferences(userId?: string) {
  return request<UserPreferenceMemoryResponse[]>({
    method: 'GET',
    url: '/api/memories/user-preferences',
    params: { userId }
  })
}

export function listProjectMemories(projectId?: string) {
  return request<ProjectMemoryResponse[]>({
    method: 'GET',
    url: `/api/memories/projects/${encodeURIComponent(projectId || '')}`,
    params: { projectId }
  })
}

export function listCreationHistories(conversationId?: string) {
  return request<CreationHistoryMemoryResponse[]>({
    method: 'GET',
    url: '/api/memories/creation-histories',
    params: { conversationId }
  })
}
