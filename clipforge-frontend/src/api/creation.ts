import { request } from './http'
import type { CreationPlanResult, ModelCallRecordView, TraceEvent } from '@/types/api'
import type { QueryPlanResponse, QueryQualityReportResponse } from '@/types/domain'

export interface CreatePlanSubmitRequest {
  userId?: string
  projectId?: string
  conversationId?: string
  goal: string
  platform: string
  stylePreference?: string
  materialIds?: string[]
  knowledgeBaseIds?: string[]
}

export interface CreatePlanSubmitResponse {
  runId?: string
  projectId?: string
  conversationId?: string
  planId?: string
  result?: CreationPlanResult
  traceEvents?: TraceEvent[]
}

export function createPlan(payload: CreatePlanSubmitRequest) {
  return request<CreatePlanSubmitResponse>({
    method: 'POST',
    url: '/api/creation/plans',
    data: payload
  })
}

export function queryPlan(runId: string) {
  return request<QueryPlanResponse>({
    method: 'GET',
    url: `/api/creation/plans/${encodeURIComponent(runId)}`
  })
}

export function queryQualityReport(runId: string) {
  return request<QueryQualityReportResponse>({
    method: 'GET',
    url: `/api/agent-runs/${encodeURIComponent(runId)}/quality-report`
  })
}

export function queryTrace(runId: string) {
  return request<{ runId?: string; traceEvents?: TraceEvent[] }>({
    method: 'GET',
    url: `/api/agent-runs/${encodeURIComponent(runId)}/trace`
  })
}

export function queryModelCalls(runId: string) {
  return request<{ runId?: string; totalPromptTokens?: number; totalCompletionTokens?: number; totalTokens?: number; records?: ModelCallRecordView[] }>({
    method: 'GET',
    url: `/api/agent-runs/${encodeURIComponent(runId)}/model-calls`
  })
}
