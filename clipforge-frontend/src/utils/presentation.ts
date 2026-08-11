import type { TaskStatusResponse } from '@/types/api'
import type {
  KnowledgeDocumentProcessLogResponse,
  KnowledgeDocumentProcessStatusResponse,
  KnowledgeUrlSyncScheduleResponse,
  MaterialResponse,
  MaterialUnderstandingResponse,
  VideoAudioResponse,
  VideoMetadataResponse,
  VideoSubtitleSegmentResponse,
  VideoUnderstandingResponse
} from '@/types/domain'
import type { InfoRow, InfoSection } from '@/components/common/InfoSummary.vue'
import { materialTypeLabel, statusLabel } from '@/utils/display'

export function taskRows(status?: TaskStatusResponse | null, fallback?: MaterialResponse): InfoRow[] {
  return [
    { label: '任务状态', value: statusLabel(status?.status || fallback?.processStatus || fallback?.status) },
    { label: '处理阶段', value: status?.phase || fallback?.processPhase },
    { label: '处理进度', value: `${status?.progressPercent ?? fallback?.progressPercent ?? 0}%` },
    { label: '下一步动作', value: status?.nextAction || fallback?.nextAction || '-' },
    { label: '是否可重试', value: status?.canRetry },
    { label: '失败原因', value: status?.errorMessage || '-' }
  ]
}

export function materialRows(material?: MaterialResponse): InfoRow[] {
  return [
    { label: '文件名', value: material?.originalFilename },
    { label: '素材类型', value: materialTypeLabel(material?.materialType) },
    { label: '处理状态', value: statusLabel(material?.processStatus || material?.status) },
    { label: '处理进度', value: `${material?.progressPercent ?? 0}%` },
    { label: '可用于创作', value: material?.canUseForCreation },
    { label: '文件大小', value: formatBytes(material?.fileSize) }
  ]
}

export function materialUnderstandingRows(value?: MaterialUnderstandingResponse | VideoUnderstandingResponse | null): InfoRow[] {
  const video = value as VideoUnderstandingResponse | undefined
  const image = value as MaterialUnderstandingResponse | undefined
  return [
    { label: '理解状态', value: statusLabel(video?.understandingStatus || image?.status) },
    { label: '模型供应商', value: image?.modelProvider },
    { label: '模型名称', value: image?.modelName },
    { label: 'Token 用量', value: image?.totalTokenCount },
    { label: '错误信息', value: video?.errorMessage || image?.errorMessage || '-' }
  ]
}

export function materialUnderstandingSections(value?: MaterialUnderstandingResponse | VideoUnderstandingResponse | null): InfoSection[] {
  const video = value as VideoUnderstandingResponse | undefined
  const image = value as MaterialUnderstandingResponse | undefined
  const result = video?.result
  return [
    { title: '素材摘要', text: image?.contentSummary || result?.summaryForCreation },
    { title: '视频主题', text: result?.overallTheme },
    { title: '关键画面线索', items: result?.keyVisualMoments },
    { title: '高光片段建议', items: result?.highlightCandidates },
    { title: '音频摘要', text: result?.audioSummary },
    { title: '风险提示', items: result?.riskNotes }
  ]
}

export function videoMetadataRows(metadata?: VideoMetadataResponse | null): InfoRow[] {
  return [
    { label: '时长', value: metadata?.durationSeconds == null ? '-' : `${metadata.durationSeconds} 秒` },
    { label: '分辨率', value: metadata?.width && metadata?.height ? `${metadata.width} x ${metadata.height}` : '-' },
    { label: '帧率', value: metadata?.frameRate },
    { label: '码率', value: metadata?.bitRate },
    { label: '视频编码', value: metadata?.videoCodec },
    { label: '音频编码', value: metadata?.audioCodec },
    { label: '包含音频', value: metadata?.hasAudio },
    { label: '读取状态', value: statusLabel(metadata?.probeStatus) }
  ]
}

export function audioRows(audio?: VideoAudioResponse | null): InfoRow[] {
  return [
    { label: '音频状态', value: statusLabel(audio?.audioExtractStatus) },
    { label: '语音识别', value: statusLabel(audio?.speechRecognitionStatus) },
    { label: '音频时长', value: audio?.durationSeconds == null ? '-' : `${audio.durationSeconds} 秒` },
    { label: '存在音轨', value: audio?.hasAudioStream },
    { label: '错误信息', value: audio?.errorMessage || '-' }
  ]
}

export function subtitleSections(audio?: VideoAudioResponse | null, subtitles?: VideoSubtitleSegmentResponse[]): InfoSection[] {
  return [
    { title: '识别文本', text: audio?.transcriptText },
    {
      title: '字幕片段',
      items: (subtitles || []).slice(0, 20).map((item) => {
        const start = item.startSeconds == null ? '?' : item.startSeconds.toFixed(1)
        const end = item.endSeconds == null ? '?' : item.endSeconds.toFixed(1)
        return `${start}s - ${end}s：${item.text || ''}`
      })
    }
  ]
}

export function documentStatusRows(status?: KnowledgeDocumentProcessStatusResponse | null, fallback?: Record<string, unknown>): InfoRow[] {
  return [
    { label: '处理状态', value: statusLabel(status?.status || String(fallback?.overallStatus || '')) },
    { label: '当前阶段', value: status?.phase || '-' },
    { label: '处理进度', value: `${status?.progressPercent ?? '-'}%` },
    { label: '状态说明', value: status?.message || '-' },
    { label: '可重试', value: status?.canRetry },
    { label: '失败原因', value: status?.errorMessage || '-' }
  ]
}

export function logRows(log: KnowledgeDocumentProcessLogResponse): InfoRow[] {
  return [
    { label: '阶段', value: log.phase },
    { label: '状态', value: statusLabel(log.status) },
    { label: '消息', value: log.message || '-' },
    { label: '错误', value: log.errorMessage || '-' }
  ]
}

export function urlScheduleRows(schedule?: KnowledgeUrlSyncScheduleResponse | null): InfoRow[] {
  return [
    { label: '来源地址', value: schedule?.sourceUrl },
    { label: '同步周期', value: schedule?.scheduleCron },
    { label: '是否启用', value: schedule?.enabled },
    { label: '最近同步', value: schedule?.lastSyncAt || '-' },
    { label: '下次同步', value: schedule?.nextSyncAt || '-' },
    { label: '最近状态', value: statusLabel(schedule?.lastStatus) },
    { label: '说明', value: schedule?.lastMessage || '-' }
  ]
}

export function formatBytes(size?: number) {
  if (size == null) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
