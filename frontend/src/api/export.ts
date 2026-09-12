import { get, post } from './request'
import type { ExportRecord } from '@/types'

export const exportApi = {
  create(exportType: 'INTERPRETATION' | 'SUGGESTION', refId: string): Promise<{ exportId: string; taskId: string }> {
    return post<{ exportId: string; taskId: string }>('/exports', { exportType, refId })
  },
  get(id: string): Promise<ExportRecord> {
    return get<ExportRecord>(`/exports/${id}`)
  },
}

export const fileApi = {
  url(id: string): Promise<{ id: string; originalName?: string; url: string }> {
    return get<{ id: string; originalName?: string; url: string }>(`/files/${id}/url`)
  },
}

