import { request } from './http'
import type {
  ModelUsageSummaryResponse,
  QueryModelCallRecordsResponse,
  QueryQualityReportResponse,
  QueryTraceResponse
} from '@/types/domain'

export function queryTrace(runId: string) {
  return request<QueryTraceResponse>({
    method: 'GET',
    url: `/api/agent-runs/${encodeURIComponent(runId)}/trace`
  })
}

export function queryModelCalls(runId: string) {
  return request<QueryModelCallRecordsResponse>({
    method: 'GET',
    url: `/api/agent-runs/${encodeURIComponent(runId)}/model-calls`
  })
}

export function queryQualityReport(runId: string) {
  return request<QueryQualityReportResponse>({
    method: 'GET',
    url: `/api/agent-runs/${encodeURIComponent(runId)}/quality-report`
  })
}

export function queryModelUsageSummary(params?: Record<string, unknown>) {
  return request<ModelUsageSummaryResponse>({
    method: 'GET',
    url: '/api/model-usage/summary',
    params
  })
}
