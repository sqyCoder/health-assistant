import { get } from './request'
import type { MetricCategory, MetricDetailItem, MetricItem, PageData } from '@/types'

export const metricApi = {
  categories(): Promise<MetricCategory[]> {
    return get<MetricCategory[]>('/metrics/categories')
  },
  search(keyword?: string, categoryId?: string, pageSize = 20): Promise<PageData<MetricItem>> {
    return get<PageData<MetricItem>>('/metrics', { keyword, categoryId, pageSize })
  },
  detail(id: string): Promise<MetricDetailItem> {
    return get<MetricDetailItem>(`/metrics/${id}`)
  },
}
