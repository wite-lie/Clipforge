import { request, toFormData } from './http'
import type {
  KnowledgeBaseResponse,
  KnowledgeChunkResponse,
  KnowledgeDocumentProcessLogResponse,
  KnowledgeDocumentProcessStatusResponse,
  KnowledgeDocumentRecoveryResponse,
  KnowledgeDocumentResponse,
  KnowledgeUrlSyncExecutionResponse,
  KnowledgeUrlSyncScheduleResponse
} from '@/types/domain'

export interface CreateKnowledgeBaseRequest {
  userId?: string
  name: string
  description?: string
  category?: string
  defaultPlatform?: string
  embeddingProvider?: string
  embeddingModel?: string
  embeddingDimension?: number
  chunkStrategy?: string
  chunkConfigJson?: string
  retrievalConfigJson?: string
  enabled?: boolean
}

export interface CreateKnowledgeUrlDocumentRequest {
  sourceUrl: string
  userId?: string
  scheduleCron?: string
  scheduleEnabled?: boolean
}

export interface UpsertKnowledgeUrlSyncScheduleRequest {
  scheduleCron?: string
  enabled?: boolean
}

export interface RetryKnowledgeDocumentProcessRequest {
  retryStage?: string
}

export interface CreateKnowledgeChunkRequest {
  content: string
  titlePath?: string
  enabled?: boolean
}

export interface UpdateKnowledgeChunkRequest {
  content?: string
  titlePath?: string
  enabled?: boolean
}

export function createKnowledgeBase(payload: CreateKnowledgeBaseRequest) {
  return request<KnowledgeBaseResponse>({
    method: 'POST',
    url: '/api/knowledge-bases',
    data: payload
  })
}

export function listKnowledgeBases(userId?: string) {
  return request<KnowledgeBaseResponse[]>({
    method: 'GET',
    url: '/api/knowledge-bases',
    params: { userId }
  })
}

export function queryKnowledgeBase(knowledgeBaseId: string) {
  return request<KnowledgeBaseResponse>({
    method: 'GET',
    url: `/api/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}`
  })
}

export function uploadKnowledgeDocument(knowledgeBaseId: string, file: File, userId?: string) {
  return request<KnowledgeDocumentResponse>({
    method: 'POST',
    url: `/api/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/documents`,
    data: toFormData({ file, userId }),
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function createKnowledgeUrlDocument(knowledgeBaseId: string, payload: CreateKnowledgeUrlDocumentRequest) {
  return request<KnowledgeDocumentResponse>({
    method: 'POST',
    url: `/api/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/url-documents`,
    data: payload
  })
}

export function listKnowledgeDocuments(knowledgeBaseId: string) {
  return request<KnowledgeDocumentResponse[]>({
    method: 'GET',
    url: `/api/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/documents`
  })
}

export function listUrlSyncSchedules(knowledgeBaseId: string) {
  return request<KnowledgeUrlSyncScheduleResponse[]>({
    method: 'GET',
    url: `/api/knowledge-bases/${encodeURIComponent(knowledgeBaseId)}/url-sync-schedules`
  })
}

export function queryKnowledgeDocument(documentId: string) {
  return request<KnowledgeDocumentResponse>({
    method: 'GET',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}`
  })
}

export function upsertUrlSyncSchedule(documentId: string, payload: UpsertKnowledgeUrlSyncScheduleRequest) {
  return request<KnowledgeUrlSyncScheduleResponse>({
    method: 'PUT',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/url-sync-schedule`,
    data: payload
  })
}

export function queryUrlSyncSchedule(documentId: string) {
  return request<KnowledgeUrlSyncScheduleResponse>({
    method: 'GET',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/url-sync-schedule`
  })
}

export function syncUrlSchedule(scheduleId: string) {
  return request<KnowledgeUrlSyncExecutionResponse>({
    method: 'POST',
    url: `/api/knowledge-url-sync-schedules/${encodeURIComponent(scheduleId)}/sync`
  })
}

export function listUrlExecutions(scheduleId: string, limit?: number) {
  return request<KnowledgeUrlSyncExecutionResponse[]>({
    method: 'GET',
    url: `/api/knowledge-url-sync-schedules/${encodeURIComponent(scheduleId)}/executions`,
    params: { limit }
  })
}

export function processKnowledgeDocument(documentId: string) {
  return request({
    method: 'POST',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/process`
  })
}

export function fullProcessKnowledgeDocument(documentId: string) {
  return request({
    method: 'POST',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/process/full`
  })
}

export function asyncProcessKnowledgeDocument(documentId: string) {
  return request({
    method: 'POST',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/process/async`
  })
}

export function queryKnowledgeDocumentProcessStatus(documentId: string) {
  return request<KnowledgeDocumentProcessStatusResponse>({
    method: 'GET',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/process/status`
  })
}

export function queryKnowledgeDocumentProcessStatusByLog(processLogId: string) {
  return request<KnowledgeDocumentProcessStatusResponse>({
    method: 'GET',
    url: `/api/knowledge-document-process-logs/${encodeURIComponent(processLogId)}/status`
  })
}

export function queryKnowledgeDocumentRecovery(documentId: string) {
  return request<KnowledgeDocumentRecoveryResponse>({
    method: 'GET',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/process/recovery`
  })
}

export function retryKnowledgeDocumentProcess(documentId: string, payload: RetryKnowledgeDocumentProcessRequest = {}) {
  return request({
    method: 'POST',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/process/retry`,
    data: payload
  })
}

export function recoverStuckKnowledgeDocumentProcesses(timeoutMinutes?: number) {
  return request({
    method: 'POST',
    url: '/api/knowledge-document-process-logs/recover-stuck',
    params: { timeoutMinutes }
  })
}

export function listKnowledgeChunks(documentId: string) {
  return request<KnowledgeChunkResponse[]>({
    method: 'GET',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/chunks`
  })
}

export function listKnowledgeDocumentProcessLogs(documentId: string) {
  return request<KnowledgeDocumentProcessLogResponse[]>({
    method: 'GET',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/process-logs`
  })
}

export function queryKnowledgeChunk(documentId: string, chunkId: string) {
  return request<KnowledgeChunkResponse>({
    method: 'GET',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/chunks/${encodeURIComponent(chunkId)}`
  })
}

export function createKnowledgeChunk(documentId: string, payload: CreateKnowledgeChunkRequest) {
  return request<KnowledgeChunkResponse>({
    method: 'POST',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/chunks`,
    data: payload
  })
}

export function updateKnowledgeChunk(documentId: string, chunkId: string, payload: UpdateKnowledgeChunkRequest) {
  return request<KnowledgeChunkResponse>({
    method: 'PUT',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/chunks/${encodeURIComponent(chunkId)}`,
    data: payload
  })
}

export function setKnowledgeChunkEnabled(documentId: string, chunkId: string, enabled: boolean) {
  return request<KnowledgeChunkResponse>({
    method: 'PATCH',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/chunks/${encodeURIComponent(chunkId)}/enabled`,
    params: { enabled }
  })
}

export function deleteKnowledgeChunk(documentId: string, chunkId: string) {
  return request<string>({
    method: 'DELETE',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/chunks/${encodeURIComponent(chunkId)}`
  })
}

export function setKnowledgeDocumentEnabled(documentId: string, enabled: boolean) {
  return request<KnowledgeDocumentResponse>({
    method: 'PATCH',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}/enabled`,
    params: { enabled }
  })
}

export function deleteKnowledgeDocument(documentId: string) {
  return request<string>({
    method: 'DELETE',
    url: `/api/knowledge-documents/${encodeURIComponent(documentId)}`
  })
}
