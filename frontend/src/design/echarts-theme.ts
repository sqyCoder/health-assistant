import type { EChartsOption } from 'echarts'

export const AC_CHART_COLORS = [
  '#6C8CFF',
  '#7C5CFF',
  '#22D3EE',
  '#2DD4A7',
  '#F5A524',
  '#FF6E8A',
  '#8B5CF6',
  '#38BDF8',
]

export function buildAcTheme(dark: boolean) {
  const text1 = dark ? '#E8EEF9' : '#0F172A'
  const text2 = dark ? '#9AA7BD' : '#475569'
  const text3 = dark ? '#5B6B85' : '#94A3B8'
  const border = dark ? 'rgba(148,163,184,0.16)' : 'rgba(15,23,42,0.10)'
  const splitLine = dark ? 'rgba(148,163,184,0.10)' : 'rgba(15,23,42,0.06)'
  const tooltipBg = dark ? 'rgba(17,24,39,0.92)' : 'rgba(255,255,255,0.95)'

  return {
    color: AC_CHART_COLORS,
    backgroundColor: 'transparent',
    textStyle: { color: text2, fontFamily: 'Inter Variable, PingFang SC, Microsoft YaHei, sans-serif' },
    title: {
      textStyle: { color: text1, fontWeight: 600, fontSize: 14 },
      subtextStyle: { color: text3 },
    },
    legend: {
      textStyle: { color: text2, fontSize: 12 },
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
    },
    tooltip: {
      backgroundColor: tooltipBg,
      borderColor: border,
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: text1, fontSize: 12.5 },
      extraCssText: 'border-radius:12px;box-shadow:0 12px 32px -12px rgba(7,11,20,.35);backdrop-filter:blur(12px);',
      axisPointer: {
        lineStyle: { color: border },
        crossStyle: { color: border },
      },
    },
    grid: { left: 48, right: 28, top: 44, bottom: 34, containLabel: false },
    categoryAxis: {
      axisLine: { lineStyle: { color: border } },
      axisTick: { show: false },
      axisLabel: { color: text3, fontSize: 11 },
      splitLine: { show: false },
    },
    valueAxis: {
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: text3, fontSize: 11 },
      splitLine: { lineStyle: { color: splitLine } },
    },
    line: {
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { width: 2.5 },
      emphasis: { focus: 'series' },
    },
    bar: {
      itemStyle: { borderRadius: [6, 6, 0, 0] },
      emphasis: { focus: 'series' },
    },
    pie: {
      itemStyle: { borderColor: dark ? '#0B1120' : '#ffffff', borderWidth: 2 },
      label: { color: text2 },
    },
  } as const
}

export function findSeriesName(option: EChartsOption): string {
  const series = option.series
  if (Array.isArray(series) && series.length > 0) {
    const first = series[0] as { name?: string }
    return first.name ?? ''
  }
  return ''
}
