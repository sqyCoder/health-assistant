import { del, get, post } from './request'
import type { PageData, Suggestion } from '@/types'

export const suggestionApi = {
  generate(
    memberId: string,
    payload: { suggestionType: string; sourceType?: string; sourceId?: string; goal?: string },
  ): Promise<{ suggestionId: string; taskId: string }> {
    return post<{ suggestionId: string; taskId: string }>(`/members/${memberId}/suggestions`, payload)
  },
  list(memberId: string, pageNum = 1, pageSize = 10): Promise<PageData<Suggestion>> {
    return get<PageData<Suggestion>>(`/members/${memberId}/suggestions`, { pageNum, pageSize })
  },
  get(id: string): Promise<Suggestion> {
    return get<Suggestion>(`/suggestions/${id}`)
  },
  remove(id: string): Promise<void> {
    return del<void>(`/suggestions/${id}`)
  },
}
