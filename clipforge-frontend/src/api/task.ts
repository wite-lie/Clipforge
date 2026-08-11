import { request } from './http'
import type { TaskStatusResponse } from '@/types/api'

export function queryTaskStatus(taskId: string) {
  return request<TaskStatusResponse>({
    method: 'GET',
    url: `/api/tasks/${encodeURIComponent(taskId)}/status`
  })
}
