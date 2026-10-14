export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
}

export interface PageLike<T> {
  items: T[]
  total?: number
}

export interface TraceEvent {
  runId?: string
  eventType: string
  message: string
  detail?: string
  occurredAt?: string
}

export interface ModelCallRecordView {
  recordId?: string
  provider?: string
  modelName?: string
  purpose?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  latencyMs?: number
  success?: boolean
  errorCode?: string | null
  errorMessage?: string | null
}

export interface CreationPlanVersionView {
  versionId?: string
  versionType?: string
  label?: string
  createdAt?: string
  promptTokens?: number
  completionTokens?: number
}

export interface CreationEvaluationResultView {
  evaluationId?: string
  versionId?: string
  score?: number
  summary?: string
  strengths?: string[]
  weaknesses?: string[]
}

export interface CreationRevisionResultView {
  revisionId?: string
  fromVersionId?: string
  toVersionId?: string
  reason?: string
}

export interface CreationPlanResult {
  theme?: string
  targetAudience?: string
  hook?: string
  storyboard?: string[]
  voiceoverScript?: string
  subtitleSuggestions?: string[]
  titleCandidates?: string[]
  publishCopy?: string
  hashtags?: string[]
  score?: number
  improvementSuggestions?: string[]
  creativeNotes?: string[]
  materialSummaryText?: string
  knowledgeSummaryText?: string
}

export type CreationOutputType = 'VIDEO_PLAN' | 'ARTICLE_PLAN'

export interface ArticleSourceEvidence {
  sourceType?: string
  sourceId?: string
  quote?: string
}

export interface ArticleImageSlot {
  slotId?: string
  sectionTitle?: string
  placement?: string
  sourceType?: string
  recommendedMaterialIds?: string[]
  boundMaterialId?: string
  imagePrompt?: string
  reason?: string
  status?: string
}

export interface ArticleSection {
  sectionTitle?: string
  body?: string
  keyPoints?: string[]
  suggestedMaterialIds?: string[]
  imageSlots?: ArticleImageSlot[]
  sourceEvidence?: ArticleSourceEvidence[]
}

export interface ArticleCreationPlan {
  theme?: string
  targetAudience?: string
  titleCandidates?: string[]
  summary?: string
  lead?: string
  sections?: ArticleSection[]
  imageSlots?: ArticleImageSlot[]
  conclusion?: string
  interactionPrompt?: string
  publishCopy?: string
  hashtags?: string[]
  sourceEvidence?: ArticleSourceEvidence[]
  riskWarnings?: string[]
  score?: number
  improvementSuggestions?: string[]
}

export interface CreationOutputView {
  outputId?: string
  runId?: string
  taskId?: string
  outputType?: CreationOutputType
  schemaVersion?: string
  payloadJson?: string
  createTime?: string
}

export interface CreationMarkdownExportResponse {
  exportId?: string
  runId?: string
  outputId?: string
  outputType?: CreationOutputType
  exportFormat?: string
  fileName?: string
  filePath?: string
  contentType?: string
  sizeBytes?: number
  status?: string
  errorMessage?: string
  createTime?: string
}

export interface TaskStatusResponse {
  taskId?: string
  taskType?: string
  bizId?: string
  bizType?: string
  status?: string
  phase?: string
  progressPercent?: number
  message?: string
  errorCode?: string
  errorMessage?: string
  retryCount?: number
  canRetry?: boolean
  nextAction?: string
  startedAt?: string
  updatedAt?: string
  endedAt?: string
  source?: string
}
