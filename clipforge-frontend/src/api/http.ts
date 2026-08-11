import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'

const DEFAULT_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export const http: AxiosInstance = axios.create({
  baseURL: DEFAULT_BASE_URL,
  timeout: 120000
})

export class ApiError extends Error {
  code?: string
  status?: number

  constructor(message: string, code?: string, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await http.request<ApiResponse<T>>(config)
    const payload = response.data
    if (!payload.success) {
      throw new ApiError(payload.message || '请求失败', payload.code, response.status)
    }
    return payload.data
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    if (axios.isAxiosError(error)) {
      throw normalizeAxiosError(error)
    }
    throw new ApiError('未知错误')
  }
}

export function normalizeAxiosError(error: AxiosError<ApiResponse<unknown>>): ApiError {
  const payload = error.response?.data
  if (payload) {
    return new ApiError(payload.message || error.message, payload.code, error.response?.status)
  }
  return new ApiError(error.message || '网络请求失败', undefined, error.response?.status)
}

export function buildQuery(params: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}

export function toFormData(fields: Record<string, unknown>): FormData {
  const formData = new FormData()
  Object.entries(fields).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    if (Array.isArray(value)) {
      value.forEach((item) => formData.append(key, String(item)))
      return
    }
    formData.append(key, value instanceof Blob ? value : String(value))
  })
  return formData
}
