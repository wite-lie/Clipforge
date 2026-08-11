import { request, toFormData } from './http'
import type { MaterialResponse, MaterialUnderstandingResponse } from '@/types/domain'
import type { TaskStatusResponse } from '@/types/api'

export interface UploadMaterialRequest {
  file: File
  userId?: string
  projectId?: string
  conversationId?: string
  runId?: string
  uploadBatchId?: string
}

export async function uploadImage(payload: UploadMaterialRequest) {
  const formData = toFormData(payload as unknown as Record<string, unknown>)
  return request<MaterialResponse>({
    method: 'POST',
    url: '/api/materials/images',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function uploadVideo(payload: UploadMaterialRequest) {
  const formData = toFormData(payload as unknown as Record<string, unknown>)
  return request<MaterialResponse>({
    method: 'POST',
    url: '/api/materials/videos',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listMaterials(params: { userId?: string; projectId?: string; conversationId?: string; runId?: string; materialType?: string }) {
  return request<MaterialResponse[]>({
    method: 'GET',
    url: '/api/materials',
    params
  })
}

export function queryMaterial(materialId: string) {
  return request<MaterialResponse>({
    method: 'GET',
    url: `/api/materials/${encodeURIComponent(materialId)}`
  })
}

export function queryMaterialUnderstanding(materialId: string) {
  return request<MaterialUnderstandingResponse>({
    method: 'GET',
    url: `/api/materials/${encodeURIComponent(materialId)}/understanding`
  })
}

export function queryTaskStatus(taskId: string) {
  return request<TaskStatusResponse>({
    method: 'GET',
    url: `/api/tasks/${encodeURIComponent(taskId)}/status`
  })
}
