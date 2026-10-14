import { request } from './http'
import type { ConversationMessageResponse, ConversationResponse, ProjectResponse } from '@/types/domain'
import type { CreatePlanSubmitResponse } from './creation'
import type { CreationOutputType } from '@/types/api'

export interface CreateProjectRequest {
  userId?: string
  name: string
  description?: string
}

export interface CreateConversationRequest {
  userId?: string
  title?: string
}

export interface UpdateConversationRequest {
  title: string
}

export interface QueryConversationMessagesResponse {
  conversationId?: string
  messages?: ConversationMessageResponse[]
}

export interface ConversationRouteRequest {
  userId?: string
  projectId?: string
  conversationId?: string
  runId?: string
  message: string
  platform?: string
  stylePreference?: string
  outputType?: CreationOutputType
  materialIds?: string[]
  processTaskIds?: string[]
  knowledgeBaseIds?: string[]
  recentMessages?: string[]
}

export interface ConversationRouteResponse {
  intentType?: string
  confidence?: number
  recognitionMethod?: string
  routeTarget?: string
  answerText?: string
  missingContext?: string[]
  creation?: CreatePlanSubmitResponse
  taskStatuses?: unknown[]
  usageSummary?: unknown
  qualityReport?: unknown
  reason?: string
}

export function createProject(payload: CreateProjectRequest) {
  return request<ProjectResponse>({
    method: 'POST',
    url: '/api/projects',
    data: payload
  })
}

export function queryProject(projectId: string) {
  return request<ProjectResponse>({
    method: 'GET',
    url: `/api/projects/${encodeURIComponent(projectId)}`
  })
}

export function listProjects(userId?: string) {
  return request<ProjectResponse[]>({
    method: 'GET',
    url: '/api/projects',
    params: { userId }
  })
}

export function createConversation(projectId: string, payload: CreateConversationRequest) {
  return request<ConversationResponse>({
    method: 'POST',
    url: `/api/projects/${encodeURIComponent(projectId)}/conversations`,
    data: payload
  })
}

export function listProjectConversations(projectId: string) {
  return request<ConversationResponse[]>({
    method: 'GET',
    url: `/api/projects/${encodeURIComponent(projectId)}/conversations`
  })
}

export function listConversationMessages(conversationId: string) {
  return request<QueryConversationMessagesResponse>({
    method: 'GET',
    url: `/api/conversations/${encodeURIComponent(conversationId)}/messages`
  })
}

export function deleteConversation(conversationId: string) {
  return request<void>({
    method: 'DELETE',
    url: `/api/conversations/${encodeURIComponent(conversationId)}`
  })
}

export function updateConversation(conversationId: string, payload: UpdateConversationRequest) {
  return request<ConversationResponse>({
    method: 'PATCH',
    url: `/api/conversations/${encodeURIComponent(conversationId)}`,
    data: payload
  })
}

export function appendConversationMessage(conversationId: string, payload: { runId?: string; role: string; content: string }) {
  return request<ConversationMessageResponse>({
    method: 'POST',
    url: `/api/conversations/${encodeURIComponent(conversationId)}/messages`,
    data: payload
  })
}

export function routeConversation(payload: ConversationRouteRequest) {
  return request<ConversationRouteResponse>({
    method: 'POST',
    url: '/api/conversations/route',
    data: payload
  })
}
