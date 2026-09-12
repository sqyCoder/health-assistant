import { get } from './request'
import type { ChartPoint } from '@/types'

export const chartApi = {
  overview(memberId: string): Promise<Record<string, unknown>> {
    return get<Record<string, unknown>>(`/charts/${memberId}/overview`)
  },
  indicatorTrend(memberId: string, metricCode: string, from?: string, to?: string): Promise<ChartPoint[]> {
    return get<ChartPoint[]>(`/charts/${memberId}/indicator-trend`, { metricCode, from, to })
  },
  exerciseTrend(memberId: string, granularity: 'day' | 'week', from?: string, to?: string): Promise<ChartPoint[]> {
    return get<ChartPoint[]>(`/charts/${memberId}/exercise-trend`, { granularity, from, to })
  },
  workoutDistribution(memberId: string, from?: string, to?: string): Promise<ChartPoint[]> {
    return get<ChartPoint[]>(`/charts/${memberId}/workout-distribution`, { from, to })
  },
  vitalTrend(memberId: string, type: 'weight' | 'bp' | 'glucose', from?: string, to?: string): Promise<ChartPoint[]> {
    return get<ChartPoint[]>(`/charts/${memberId}/vital-trend`, { type, from, to })
  },
}
