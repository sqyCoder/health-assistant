<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">数据分析</h2>
      <span v-if="!memberStore.currentMember" class="text-muted">请先在右上角选择一个成员</span>
    </div>

    <div class="page-card">
      <div class="chart-header">
        <span class="chart-title">指标趋势</span>
        <div class="chart-filters">
          <el-select
            v-model="indicatorCode"
            filterable
            remote
            clearable
            :remote-method="searchMetricOptions"
            placeholder="搜索指标（如 血糖）"
            style="width: 220px"
            @change="loadIndicatorTrend"
          >
            <el-option v-for="metric in metricOptions" :key="metric.id" :label="`${metric.name}${metric.unit ? ' (' + metric.unit + ')' : ''}`" :value="metric.code" />
          </el-select>
          <el-date-picker v-model="trendRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" @change="loadIndicatorTrend" />
        </div>
      </div>
      <div ref="indicatorChartRef" class="chart-box"></div>
    </div>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="chart-header">
            <span class="chart-title">运动量趋势</span>
            <el-radio-group v-model="exerciseGranularity" size="small" @change="loadExerciseTrend">
              <el-radio-button value="day">按天</el-radio-button>
              <el-radio-button value="week">按周</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="exerciseChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="chart-header">
            <span class="chart-title">运动类型分布</span>
          </div>
          <div ref="pieChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card mt-16">
      <div class="chart-header">
        <span class="chart-title">体征趋势</span>
        <el-radio-group v-model="vitalType" size="small" @change="loadVitalTrend">
          <el-radio-button value="weight">体重</el-radio-button>
          <el-radio-button value="bp">血压</el-radio-button>
          <el-radio-button value="glucose">血糖</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="vitalChartRef" class="chart-box"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { chartApi } from '@/api/chart'
import { buildAcTheme } from '@/design/echarts-theme'
import { metricApi } from '@/api/metric'
import { useMemberStore } from '@/stores/member'
import { useThemeStore } from '@/stores/theme'
import { JUDGE_STATUS_LABELS, labelOf } from '@/utils/dict'
import type { ChartPoint, MetricItem } from '@/types'

const memberStore = useMemberStore()
const themeStore = useThemeStore()

const indicatorChartRef = ref<HTMLDivElement>()
const exerciseChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
const vitalChartRef = ref<HTMLDivElement>()

let indicatorChart: echarts.ECharts | null = null
let exerciseChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let vitalChart: echarts.ECharts | null = null

const metricOptions = ref<MetricItem[]>([])
const indicatorCode = ref<string>()
const trendRange = ref<[string, string] | undefined>(defaultYearRange())
const exerciseGranularity = ref<'day' | 'week'>('day')
const vitalType = ref<'weight' | 'bp' | 'glucose'>('weight')

onMounted(async () => {
  initCharts()
  window.addEventListener('resize', resizeCharts)
  await initMetricOptions()
  await Promise.all([loadIndicatorTrend(), loadExerciseTrend(), loadDistribution(), loadVitalTrend()])
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})

watch(() => memberStore.currentMemberId, reloadAll)
watch(
  () => themeStore.dark,
  () => {
    disposeCharts()
    initCharts()
    void reloadAll()
  },
)

function defaultYearRange(): [string, string] {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth() - 11, 1)
  const fmt = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return [fmt(from), fmt(now)]
}

function initCharts(): void {
  echarts.registerTheme('ac-dark', buildAcTheme(true))
  echarts.registerTheme('ac-light', buildAcTheme(false))
  const theme = themeStore.dark ? 'ac-dark' : 'ac-light'
  if (indicatorChartRef.value) indicatorChart = echarts.init(indicatorChartRef.value, theme)
  if (exerciseChartRef.value) exerciseChart = echarts.init(exerciseChartRef.value, theme)
  if (pieChartRef.value) pieChart = echarts.init(pieChartRef.value, theme)
  if (vitalChartRef.value) vitalChart = echarts.init(vitalChartRef.value, theme)
}

function disposeCharts(): void {
  indicatorChart?.dispose()
  exerciseChart?.dispose()
  pieChart?.dispose()
  vitalChart?.dispose()
  indicatorChart = null
  exerciseChart = null
  pieChart = null
  vitalChart = null
}

function hexToRgba(color: string, alpha: number): string {
  if (!color.startsWith('#') || color.length < 7) {
    return color
  }
  const r = parseInt(color.slice(1, 3), 16)
  const g = parseInt(color.slice(3, 5), 16)
  const b = parseInt(color.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function areaGradient(color: string): Record<string, unknown> {
  return {
    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: hexToRgba(color, 0.3) },
      { offset: 1, color: hexToRgba(color, 0) },
    ]),
  }
}

function resizeCharts(): void {
  indicatorChart?.resize()
  exerciseChart?.resize()
  pieChart?.resize()
  vitalChart?.resize()
}

async function initMetricOptions(): Promise<void> {
  const page = await metricApi.search(undefined, undefined, 100)
  metricOptions.value = page.list
  if (!indicatorCode.value && page.list.length > 0) {
    const preferred = page.list.find((metric) => metric.code === 'GLU') ?? page.list[0]
    indicatorCode.value = preferred.code
  }
}

async function searchMetricOptions(keyword: string): Promise<void> {
  if (!keyword) {
    return
  }
  const page = await metricApi.search(keyword, undefined, 50)
  metricOptions.value = page.list
}

function textColor(): string {
  return themeStore.dark ? '#cfd3dc' : '#303133'
}

function baseGrid() {
  return {
    left: 56,
    right: 32,
    top: 44,
    bottom: 36,
  }
}

function baseAxisStyle() {
  const color = textColor()
  return {
    axisLabel: { color },
    nameTextStyle: { color },
    axisLine: { lineStyle: { color: themeStore.dark ? '#4c4d4f' : '#dcdfe6' } },
  }
}

function lineOption(categories: string[], series: { name: string; data: unknown[]; color?: string }[]) {
  const axisStyle = baseAxisStyle()
  return {
    tooltip: { trigger: 'axis' },
    legend: { textStyle: { color: textColor() }, top: 8 },
    grid: baseGrid(),
    xAxis: { type: 'category', data: categories, ...axisStyle },
    yAxis: { type: 'value', ...axisStyle, splitLine: { lineStyle: { color: themeStore.dark ? '#363637' : '#ebeef5' } } },
    series: series.map((item) => ({
      name: item.name,
      type: 'line',
      smooth: true,
      connectNulls: true,
      data: item.data,
      itemStyle: item.color ? { color: item.color } : undefined,
      lineStyle: item.color ? { color: item.color, width: 2.5 } : undefined,
      areaStyle: item.color ? areaGradient(item.color) : undefined,
    })),
  }
}

async function loadIndicatorTrend(): Promise<void> {
  if (!memberStore.currentMemberId || !indicatorCode.value || !indicatorChart) {
    indicatorChart?.clear()
    return
  }
  const points = await chartApi.indicatorTrend(
    memberStore.currentMemberId,
    indicatorCode.value,
    trendRange.value?.[0],
    trendRange.value?.[1],
  )
  if (points.length === 0) {
    indicatorChart.clear()
    indicatorChart.setOption({
      title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: textColor(), fontSize: 14 } },
    })
    return
  }
  const dates = points.map((point) => String(point.date))
  const values = points.map((point) => {
    const status = String(point.status ?? '')
    const abnormal = ['HIGH', 'LOW', 'ABNORMAL', 'POSITIVE'].includes(status)
    return {
      value: point.value as number,
      itemStyle: { color: abnormal ? '#e5484d' : '#30a46c' },
    }
  })
  const unit = points[0].unit ? ` (${points[0].unit})` : ''
  const metricName = metricOptions.value.find((metric) => metric.code === indicatorCode.value)?.name ?? indicatorCode.value
  const option = lineOption(dates, [{ name: `${metricName}${unit}`, data: values, color: '#2f7de1' }]) as Record<string, unknown>
  option.tooltip = {
    trigger: 'axis',
    formatter: (params: unknown) => {
      const list = params as { dataIndex: number }[]
      const index = list[0]?.dataIndex ?? 0
      const point = points[index]
      return `${point.date}<br/>数值：${point.value} ${point.unit ?? ''}<br/>判定：${labelOf(JUDGE_STATUS_LABELS, String(point.status))}`
    },
  }
  indicatorChart.setOption(option as echarts.EChartsOption, true)
}

async function loadExerciseTrend(): Promise<void> {
  if (!memberStore.currentMemberId || !exerciseChart) {
    exerciseChart?.clear()
    return
  }
  const points = await chartApi.exerciseTrend(memberStore.currentMemberId, exerciseGranularity.value)
  const categories = points.map((point) => String(point.date))
  const steps = points.map((point) => Number(point.steps ?? 0))
  const calories = points.map((point) => (point.calories == null ? null : Number(point.calories)))
  const axisStyle = baseAxisStyle()
  exerciseChart.setOption(
    {
      tooltip: { trigger: 'axis' },
      legend: { textStyle: { color: textColor() }, top: 8 },
      grid: baseGrid(),
      xAxis: { type: 'category', data: categories, ...axisStyle },
      yAxis: [
        { type: 'value', name: '步数', ...axisStyle, splitLine: { lineStyle: { color: themeStore.dark ? '#363637' : '#ebeef5' } } },
        { type: 'value', name: '消耗(kcal)', ...axisStyle },
      ],
      series: [
        { name: '步数', type: 'bar', data: steps, itemStyle: { color: '#2f7de1', borderRadius: [4, 4, 0, 0] } },
        { name: '消耗(kcal)', type: 'line', yAxisIndex: 1, smooth: true, data: calories, itemStyle: { color: '#f5a524' } },
      ],
    },
    true,
  )
}

async function loadDistribution(): Promise<void> {
  if (!memberStore.currentMemberId || !pieChart) {
    pieChart?.clear()
    return
  }
  const points = await chartApi.workoutDistribution(memberStore.currentMemberId)
  if (points.length === 0) {
    pieChart.clear()
    pieChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: textColor(), fontSize: 14 } } })
    return
  }
  pieChart.setOption(
    {
      tooltip: { trigger: 'item', formatter: '{b}: {c} 次' },
      legend: { bottom: 0, textStyle: { color: textColor() } },
      series: [
        {
          type: 'pie',
          radius: ['40%', '68%'],
          center: ['50%', '46%'],
          data: points.map((point) => ({ name: String(point.type), value: Number(point.count ?? 0) })),
          label: { color: textColor() },
        },
      ],
    },
    true,
  )
}

async function loadVitalTrend(): Promise<void> {
  if (!memberStore.currentMemberId || !vitalChart) {
    vitalChart?.clear()
    return
  }
  const points = await chartApi.vitalTrend(memberStore.currentMemberId, vitalType.value)
  if (points.length === 0) {
    vitalChart.clear()
    vitalChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: textColor(), fontSize: 14 } } })
    return
  }
  const categories = points.map((point) => String(point.date))
  if (vitalType.value === 'bp') {
    vitalChart.setOption(
      lineOption(categories, [
        { name: '收缩压', data: points.map((point) => (point.systolic == null ? null : Number(point.systolic))), color: '#e5484d' },
        { name: '舒张压', data: points.map((point) => (point.diastolic == null ? null : Number(point.diastolic))), color: '#2f7de1' },
      ]),
      true,
    )
  } else {
    const name = vitalType.value === 'weight' ? '体重(kg)' : '血糖(mmol/L)'
    vitalChart.setOption(
      lineOption(categories, [
        { name, data: points.map((point) => (point.value == null ? null : Number(point.value))), color: vitalType.value === 'weight' ? '#2bb3a3' : '#f5a524' },
      ]),
      true,
    )
  }
}

async function reloadAll(): Promise<void> {
  await Promise.all([loadIndicatorTrend(), loadExerciseTrend(), loadDistribution(), loadVitalTrend()])
}

function reRenderAll(): void {
  reloadAll()
}
</script>

<style scoped>
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
}

.chart-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chart-box {
  width: 100%;
  height: 300px;
}
</style>
