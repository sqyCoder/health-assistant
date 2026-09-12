import { get, post } from './request'
import type { Interpretation } from '@/types'

export const interpretationApi = {
  generate(reportId: string): Promise<{ interpretationId: string; taskId: string }> {
    return post<{ interpretationId: string; taskId: string }>(`/reports/${reportId}/interpretations`)
  },
  latest(reportId: string): Promise<Interpretation | null> {
    return get<Interpretation | null>(`/reports/${reportId}/interpretations/latest`)
  },
}
