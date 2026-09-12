import axios from 'axios'
import { del, get, post, put, tokenStore } from './request'
import type { IndicatorItem, PageData, Report, ReportDetail } from '@/types'

export interface ReportUploadMeta {
  memberId: string
  reportName?: string
  reportDate: string
  institution?: string
  reportType: string
}

export async function uploadReport(file: File, meta: ReportUploadMeta): Promise<Report> {
  const form = new FormData()
  form.append('file', file)
  form.append('memberId', meta.memberId)
  if (meta.reportName) form.append('reportName', meta.reportName)
  form.append('reportDate', meta.reportDate)
  if (meta.institution) form.append('institution', meta.institution)
  form.append('reportType', meta.reportType)
  const resp = await axios.post('/api/reports/upload', form, {
    headers: { Authorization: `Bearer ${tokenStore.getAccess() ?? ''}` },
    timeout: 60000,
  })
  const body = resp.data
  if (body.code !== 0) {
    throw new Error(body.message)
  }
  return body.data as Report
}

export const reportApi = {
  list(params: {
    memberId?: string
    reportType?: string
    dateFrom?: string
    dateTo?: string
    pageNum?: number
    pageSize?: number
  }): Promise<PageData<Report>> {
    return get<PageData<Report>>('/reports', params)
  },
  detail(id: string): Promise<ReportDetail> {
    return get<ReportDetail>(`/reports/${id}`)
  },
  createManual(payload: {
    memberId: string
    reportName?: string
    reportDate: string
    institution?: string
    reportType: string
    remark?: string
    indicators: IndicatorItem[]
  }): Promise<ReportDetail> {
    return post<ReportDetail>('/reports/manual', payload)
  },
  updateIndicators(id: string, indicators: IndicatorItem[]): Promise<ReportDetail> {
    return put<ReportDetail>(`/reports/${id}/indicators`, { indicators })
  },
  updateMeta(
    id: string,
    payload: { reportName: string; reportDate: string; institution?: string; reportType: string; remark?: string },
  ): Promise<Report> {
    return put<Report>(`/reports/${id}`, payload)
  },
  remove(id: string): Promise<void> {
    return del<void>(`/reports/${id}`)
  },
  parse(id: string): Promise<string> {
    return post<string>(`/reports/${id}/parse`)
  },
}
