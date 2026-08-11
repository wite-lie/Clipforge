import type { CreationEvaluationResultView, CreationPlanResult, CreationPlanVersionView, CreationRevisionResultView, ModelCallRecordView, TraceEvent } from './api'

export interface ProjectResponse {
  projectId?: string
  userId?: string
  name?: string
  description?: string
}

export interface ConversationResponse {
  conversationId?: string
  projectId?: string
  userId?: string
  title?: string
  status?: string
}

export interface ConversationMessageResponse {
  messageId?: string
  conversationId?: string
  runId?: string
  role?: string
  content?: string
}

export interface KnowledgeBaseResponse {
  knowledgeBaseId?: string
  userId?: string
  name?: string
  description?: string
  category?: string
  defaultPlatform?: string
  embeddingProvider?: string
  embeddingModel?: string
  embeddingDimension?: number
  chunkStrategy?: string
  chunkConfigJson?: string
  retrievalConfigJson?: string
  status?: string
  enabled?: number
  createTime?: string
  updateTime?: string
}

export interface KnowledgeDocumentResponse {
  documentId?: string
  knowledgeBaseId?: string
  userId?: string
  sourceType?: string
  sourceLocation?: string
  originalFilename?: string
  storedFilename?: string
  filePath?: string
  contentType?: string
  fileSize?: number
  fileHash?: string
  parseStatus?: string
  chunkStatus?: string
  embeddingStatus?: string
  overallStatus?: string
  errorMessage?: string
  chunkStrategy?: string
  chunkConfigJson?: string
  metadataJson?: string
  chunkCount?: number
  charCount?: number
  tokenCount?: number
  enabled?: number
  createTime?: string
  updateTime?: string
}

export interface KnowledgeChunkResponse {
  chunkId?: string
  knowledgeBaseId?: string
  documentId?: string
  chunkIndex?: number
  titlePath?: string
  content?: string
  contentHash?: string
  charCount?: number
  tokenCount?: number
  metadataJson?: string
  enabled?: number
  createTime?: string
  updateTime?: string
}

export interface KnowledgeDocumentProcessStatusResponse {
  documentId?: string
  processLogId?: string
  status?: string
  phase?: string
  message?: string
  errorMessage?: string
  retryCount?: number
  canRetry?: boolean
  progressPercent?: number
  source?: string
  startedAt?: string
  updatedAt?: string
  endedAt?: string
}

export interface KnowledgeDocumentProcessLogResponse {
  processLogId?: string
  documentId?: string
  phase?: string
  status?: string
  message?: string
  errorMessage?: string
  startedAt?: string
  endedAt?: string
}

export interface KnowledgeDocumentRecoveryResponse {
  documentId?: string
  recoverable?: boolean
  recommendedStage?: string
  retryCount?: number
  suggestion?: string
}

export interface KnowledgeUrlSyncScheduleResponse {
  scheduleId?: string
  documentId?: string
  knowledgeBaseId?: string
  sourceUrl?: string
  scheduleCron?: string
  enabled?: boolean
  lastSyncAt?: string
  nextSyncAt?: string
  lastStatus?: string
  lastMessage?: string
}

export interface KnowledgeUrlSyncExecutionResponse {
  executionId?: string
  scheduleId?: string
  documentId?: string
  status?: string
  changed?: boolean
  message?: string
  startedAt?: string
  endedAt?: string
}

export interface MaterialResponse {
  materialId?: string
  userId?: string
  projectId?: string
  conversationId?: string
  runId?: string
  materialType?: string
  originalFilename?: string
  storedFilename?: string
  filePath?: string
  contentType?: string
  fileSize?: number
  status?: string
  uploadBatchId?: string
  processTaskId?: string
  processStatus?: string
  processPhase?: string
  progressPercent?: number
  canUseForCreation?: boolean
  statusQueryUrl?: string
  nextAction?: string
}

export interface MaterialUnderstandingResponse {
  understandingId?: string
  materialId?: string
  materialType?: string
  understandingType?: string
  status?: string
  modelProvider?: string
  modelName?: string
  promptVersion?: string
  contentHash?: string
  contentSummary?: string
  imageUnderstandingResult?: Record<string, unknown>
  videoFrameUnderstandingResult?: Record<string, unknown>
  inputTokenCount?: number
  outputTokenCount?: number
  totalTokenCount?: number
  errorMessage?: string
}

export interface VideoMetadataResponse {
  metadataId?: string
  materialId?: string
  durationSeconds?: number
  width?: number
  height?: number
  frameRate?: number
  bitRate?: number
  formatName?: string
  videoCodec?: string
  hasAudio?: boolean
  audioCodec?: string
  audioStreamCount?: number
  probeStatus?: string
  probeErrorMessage?: string
  durationWithinConfiguredRange?: boolean
  durationWarningMessage?: string
}

export interface VideoFrameResponse {
  frameId?: string
  materialId?: string
  frameMaterialId?: string
  frameIndex?: number
  timestampSeconds?: number
  filePath?: string
  width?: number
  height?: number
  extractStatus?: string
  errorMessage?: string
  keyFrameSelected?: boolean
  qualityScore?: number
  brightness?: number
  sharpnessScore?: number
  imageHash?: string
  rejectReason?: string
}

export interface VideoAudioResponse {
  audioId?: string
  materialId?: string
  audioFilePath?: string
  durationSeconds?: number
  hasAudioStream?: boolean
  audioExtractStatus?: string
  speechRecognitionStatus?: string
  transcriptText?: string
  errorMessage?: string
}

export interface VideoSubtitleSegmentResponse {
  segmentId?: string
  audioId?: string
  materialId?: string
  segmentIndex?: number
  startSeconds?: number
  endSeconds?: number
  text?: string
  confidence?: number
  source?: string
}

export interface VideoUnderstandingResult {
  overallTheme?: string
  keyVisualMoments?: string[]
  highlightCandidates?: string[]
  summaryForCreation?: string
  riskNotes?: string[]
  audioSummary?: string
}

export interface VideoUnderstandingResponse {
  understandingId?: string
  materialId?: string
  understandingStatus?: string
  result?: VideoUnderstandingResult
  errorMessage?: string
}

export interface ModelUsageSummaryResponse {
  runId?: string
  totalPromptTokens?: number
  totalCompletionTokens?: number
  totalTokens?: number
  records?: Array<{
    purpose?: string
    provider?: string
    modelName?: string
    promptTokens?: number
    completionTokens?: number
    totalTokens?: number
  }>
}

export interface QueryTraceResponse {
  runId?: string
  traceEvents?: TraceEvent[]
}

export interface QueryModelCallRecordsResponse {
  runId?: string
  totalPromptTokens?: number
  totalCompletionTokens?: number
  totalTokens?: number
  records?: ModelCallRecordView[]
}

export interface QueryPlanResponse {
  runId?: string
  taskId?: string
  resultId?: string
  result?: CreationPlanResult
}

export interface QueryQualityReportResponse {
  runId?: string
  taskId?: string
  resultId?: string
  finalResult?: CreationPlanResult
  finalScore?: number
  evaluationTriggered?: boolean
  revisionTriggered?: boolean
  revisionCount?: number
  totalPromptTokens?: number
  totalCompletionTokens?: number
  totalTokens?: number
  tokensByPurpose?: Record<string, number>
  estimatedInputCost?: string
  estimatedOutputCost?: string
  estimatedTotalCost?: string
  currency?: string
  priceMatched?: boolean
  costByPurpose?: Record<string, string>
  finalVersionId?: string
  versions?: CreationPlanVersionView[]
  evaluations?: CreationEvaluationResultView[]
  revisions?: CreationRevisionResultView[]
  evaluationEvents?: TraceEvent[]
  revisionEvents?: TraceEvent[]
  modelCallRecords?: ModelCallRecordView[]
}

export interface UserPreferenceMemoryResponse {
  memoryId?: string
  userId?: string
  key?: string
  value?: string
  source?: string
  confidence?: number
  enabled?: boolean
  updateTime?: string
}

export interface ProjectMemoryResponse {
  memoryId?: string
  projectId?: string
  key?: string
  value?: string
  source?: string
  confidence?: number
  enabled?: boolean
  updateTime?: string
}

export interface CreationHistoryMemoryResponse {
  memoryId?: string
  projectId?: string
  conversationId?: string
  runId?: string
  summary?: string
  keyPoints?: string[]
  enabled?: boolean
  updateTime?: string
}
