import { del, get, post, put } from './request'
import type { AiLogRecord, MetricItem, PageData, PromptTemplate, TaskInfo, UserInfo } from '@/types'

export const adminApi = {
  users(params: { keyword?: string; status?: number; pageNum?: number; pageSize?: number }): Promise<PageData<UserInfo>> {
    return get<PageData<UserInfo>>('/admin/users', params)
  },
  updateUserStatus(id: string, status: number): Promise<void> {
    return put<void>(`/admin/users/${id}/status`, { status })
  },
  resetPassword(id: string): Promise<{ tempPassword: string }> {
    return put<{ tempPassword: string }>(`/admin/users/${id}/reset-password`)
  },

  metrics(params: { keyword?: string; categoryId?: string; pageNum?: number; pageSize?: number }): Promise<PageData<MetricItem>> {
    return get<PageData<MetricItem>>('/admin/metrics', params)
  },
  createMetric(data: object): Promise<unknown> {
    return post<unknown>('/admin/metrics', data)
  },
  updateMetric(id: string, data: object): Promise<unknown> {
    return put<unknown>(`/admin/metrics/${id}`, data)
  },
  updateMetricStatus(id: string, status: number): Promise<void> {
    return put<void>(`/admin/metrics/${id}/status`, { status })
  },
  replaceRanges(id: string, ranges: object[]): Promise<unknown> {
    return put<unknown>(`/admin/metrics/${id}/ranges`, { ranges })
  },

  prompts(code?: string): Promise<PromptTemplate[]> {
    return get<PromptTemplate[]>('/admin/prompts', { code })
  },
  createPrompt(data: object): Promise<PromptTemplate> {
    return post<PromptTemplate>('/admin/prompts', data)
  },
  updatePromptStatus(id: string, status: string): Promise<void> {
    return put<void>(`/admin/prompts/${id}/status`, { status })
  },

  aiLogs(params: {
    scene?: string
    userId?: string
    dateFrom?: string
    dateTo?: string
    pageNum?: number
    pageSize?: number
  }): Promise<PageData<AiLogRecord>> {
    return get<PageData<AiLogRecord>>('/admin/ai-logs', params)
  },
  aiLogSummary(days = 30): Promise<{ date: string; count: number; tokens: number; cost: number }[]> {
    return get<{ date: string; count: number; tokens: number; cost: number }[]>('/admin/ai-logs/summary', { days })
  },

  tasks(params: { status?: string; taskType?: string; pageNum?: number; pageSize?: number }): Promise<PageData<TaskInfo>> {
    return get<PageData<TaskInfo>>('/admin/tasks', params)
  },
  retryTask(id: string): Promise<void> {
    return post<void>(`/admin/tasks/${id}/retry`)
  },
}
