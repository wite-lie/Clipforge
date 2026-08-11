import { request } from './http'
import type { VideoAudioResponse, VideoFrameResponse, VideoMetadataResponse, VideoSubtitleSegmentResponse, VideoUnderstandingResponse } from '@/types/domain'

export function readVideoMetadata(materialId: string) {
  return request<VideoMetadataResponse>({
    method: 'POST',
    url: `/api/videos/${encodeURIComponent(materialId)}/metadata/read`
  })
}

export function queryVideoMetadata(materialId: string) {
  return request<VideoMetadataResponse>({
    method: 'GET',
    url: `/api/videos/${encodeURIComponent(materialId)}/metadata`
  })
}

export function extractVideoFrames(materialId: string) {
  return request<VideoFrameResponse[]>({
    method: 'POST',
    url: `/api/videos/${encodeURIComponent(materialId)}/frames/extract`
  })
}

export function registerVideoFrameMaterials(materialId: string) {
  return request<VideoFrameResponse[]>({
    method: 'POST',
    url: `/api/videos/${encodeURIComponent(materialId)}/frames/register-materials`
  })
}

export function listVideoFrames(materialId: string) {
  return request<VideoFrameResponse[]>({
    method: 'GET',
    url: `/api/videos/${encodeURIComponent(materialId)}/frames`
  })
}

export function extractVideoAudio(materialId: string) {
  return request<VideoAudioResponse>({
    method: 'POST',
    url: `/api/videos/${encodeURIComponent(materialId)}/audio/extract`
  })
}

export function queryVideoAudio(materialId: string) {
  return request<VideoAudioResponse>({
    method: 'GET',
    url: `/api/videos/${encodeURIComponent(materialId)}/audio`
  })
}

export function transcribeVideoAudio(materialId: string) {
  return request<VideoAudioResponse>({
    method: 'POST',
    url: `/api/videos/${encodeURIComponent(materialId)}/audio/transcribe`
  })
}

export function listVideoSubtitles(materialId: string) {
  return request<VideoSubtitleSegmentResponse[]>({
    method: 'GET',
    url: `/api/videos/${encodeURIComponent(materialId)}/audio/subtitles`
  })
}

export function understandVideo(materialId: string, runId?: string, instruction?: string) {
  return request<VideoUnderstandingResponse>({
    method: 'POST',
    url: `/api/videos/${encodeURIComponent(materialId)}/understand`,
    params: { runId, instruction }
  })
}

export function queryVideoUnderstanding(materialId: string) {
  return request<VideoUnderstandingResponse>({
    method: 'GET',
    url: `/api/videos/${encodeURIComponent(materialId)}/understanding`
  })
}
