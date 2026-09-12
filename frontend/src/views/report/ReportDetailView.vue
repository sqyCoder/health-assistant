<template>
  <div v-loading="loading">
    <div class="page-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="router.back()">返回</el-button>
        <h2 class="page-title">{{ detail?.report.reportName || '报告详情' }}</h2>
      </div>
      <div v-if="detail" class="header-actions">
        <el-button :icon="EditPen" @click="reviewVisible = true">校对指标</el-button>
        <el-button v-if="detail.report.sourceType === 'PDF'" :icon="RefreshRight" @click="reparse">重新解析</el-button>
        <el-button type="danger" plain :icon="Delete" @click="remove">删除</el-button>
      </div>
    </div>

    <template v-if="detail">
      <div v-if="detail.report.parseStatus === 'PARSING' || detail.report.parseStatus === 'UPLOADED'" class="page-card parsing-tip">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>报告解析中（AI 正在提取指标），请稍候…</span>
      </div>
      <div v-if="detail.report.parseStatus === 'PARSE_FAILED'" class="page-card failure-tip">
        <span>解析失败，可点击右上角"重新解析"重试，或使用"校对指标"手动录入。</span>
      </div>
      <div v-if="detail.report.parseStatus === 'NEED_MANUAL'" class="page-card failure-tip">
        <span>检测到扫描版 PDF，暂不支持自动解析，请使用"校对指标"手动录入。</span>
      </div>

      <div class="page-card">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="体检日期">{{ detail.report.reportDate }}</el-descriptions-item>
          <el-descriptions-item label="报告类型">{{ labelOf(REPORT_TYPE_LABELS, detail.report.reportType) }}</el-descriptions-item>
          <el-descriptions-item label="体检机构">{{ detail.report.institution || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ detail.report.sourceType === 'PDF' ? 'PDF 上传' : '手动录入' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="detail.report.parseStatus === 'CONFIRMED' ? 'success' : 'warning'">
              {{ labelOf(PARSE_STATUS_LABELS, detail.report.parseStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="指标统计">
            {{ detail.report.indicatorCount }} 项
            <el-tag v-if="detail.report.abnormalCount > 0" type="danger" size="small" class="ml-4">
              {{ detail.report.abnormalCount }} 项异常
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="page-card mt-16">
        <h3 class="section-title">指标明细</h3>
        <el-table :data="detail.indicators" empty-text="暂无指标">
          <el-table-column label="指标" min-width="150">
            <template #default="{ row }">
              <div>{{ row.metricName }}</div>
              <div v-if="row.categoryName" class="text-muted sub-text">{{ row.categoryName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="检验值" width="110">
            <template #default="{ row }">
              <span :class="isAbnormal(row.judgeStatus) ? 'abnormal-value' : ''">{{ row.valueText ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="90">
            <template #default="{ row }">{{ row.unit || '-' }}</template>
          </el-table-column>
          <el-table-column prop="refRangeText" label="参考范围" width="120">
            <template #default="{ row }">{{ row.refRangeText || '-' }}</template>
          </el-table-column>
          <el-table-column label="区间位置" width="150">
            <template #default="{ row }">
              <IndicatorBar
                :value="row.valueNumeric"
                :ref-min="row.refMin"
                :ref-max="row.refMax"
                :status="row.judgeStatus"
              />
            </template>
          </el-table-column>
          <el-table-column label="判定" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="judgeTagType(row.judgeStatus)">
                {{ labelOf(JUDGE_STATUS_LABELS, row.judgeStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="匹配" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.matchType" size="small" effect="plain" :type="row.matchType === 'FUZZY' ? 'warning' : 'info'">
                {{ row.matchType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="judgeNote" label="说明" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.judgeNote || '-' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <InterpretationPanel :report-id="reportId" :has-indicators="(detail.report.indicatorCount ?? 0) > 0" />
    </template>

    <IndicatorEditorDialog
      v-model="reviewVisible"
      title="校对指标（保存后全量替换）"
      :show-meta="false"
      :loading="saving"
      :initial-indicators="editorItems"
      @submit="submitReview"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, EditPen, Loading, RefreshRight } from '@element-plus/icons-vue'
import { reportApi } from '@/api/report'
import IndicatorEditorDialog from '@/components/IndicatorEditorDialog.vue'
import InterpretationPanel from '@/components/InterpretationPanel.vue'
import IndicatorBar from '@/components/biz/IndicatorBar.vue'
import {
  JUDGE_STATUS_LABELS,
  JUDGE_STATUS_TAG_TYPES,
  PARSE_STATUS_LABELS,
  REPORT_TYPE_LABELS,
  labelOf,
} from '@/utils/dict'
import type { IndicatorItem, ReportDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const reportId = computed(() => (typeof route.params.id === 'string' ? route.params.id : ''))
const loading = ref(false)
const saving = ref(false)
const detail = ref<ReportDetail | null>(null)
const reviewVisible = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null
let pollCount = 0

const editorItems = computed<IndicatorItem[]>(() =>
  (detail.value?.indicators ?? []).map((indicator) => ({
    metricId: indicator.metricId,
    metricName: indicator.metricName,
    valueText: indicator.valueText,
    unit: indicator.unit,
    refRangeText: indicator.refRangeText,
    sort: indicator.sort,
  })),
)

onMounted(load)

onBeforeUnmount(() => stopPolling())

async function load(): Promise<void> {
  if (!reportId.value) {
    return
  }
  loading.value = true
  try {
    detail.value = await reportApi.detail(reportId.value)
    const status = detail.value.report.parseStatus
    if (status === 'UPLOADED' || status === 'PARSING') {
      startPolling()
    } else {
      stopPolling()
    }
  } finally {
    loading.value = false
  }
}

function startPolling(): void {
  if (pollTimer) {
    return
  }
  pollCount = 0
  pollTimer = setInterval(async () => {
    pollCount++
    if (pollCount > 20) {
      stopPolling()
      return
    }
    if (!reportId.value) {
      return
    }
    try {
      const latest = await reportApi.detail(reportId.value)
      detail.value = latest
      const status = latest.report.parseStatus
      if (status !== 'UPLOADED' && status !== 'PARSING') {
        stopPolling()
        ElMessage.success(`解析完成，共 ${latest.report.indicatorCount} 项指标`)
      }
    } catch {
      stopPolling()
    }
  }, 3000)
}

function stopPolling(): void {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function submitReview(payload: { indicators: IndicatorItem[] }): Promise<void> {
  if (!reportId.value) {
    return
  }
  saving.value = true
  try {
    detail.value = await reportApi.updateIndicators(reportId.value, payload.indicators)
    ElMessage.success('校对已保存')
    reviewVisible.value = false
  } finally {
    saving.value = false
  }
}

async function reparse(): Promise<void> {
  if (!reportId.value) {
    return
  }
  await reportApi.parse(reportId.value)
  ElMessage.success('已提交重新解析')
  detail.value = await reportApi.detail(reportId.value)
  startPolling()
}

async function remove(): Promise<void> {
  if (!reportId.value) {
    return
  }
  await ElMessageBox.confirm('确定删除该报告吗？', '删除确认', { type: 'warning' })
  await reportApi.remove(reportId.value)
  ElMessage.success('已删除')
  router.push('/reports')
}

function isAbnormal(status: string): boolean {
  return ['HIGH', 'LOW', 'ABNORMAL', 'POSITIVE'].includes(status)
}

function judgeTagType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  return (JUDGE_STATUS_TAG_TYPES[status] ?? 'info') as 'success' | 'warning' | 'danger' | 'info'
}
</script>

<style scoped>
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.parsing-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-primary);
}

.failure-tip {
  color: var(--el-color-warning);
}

.section-title {
  margin: 0 0 14px;
  font-size: 16px;
}

.abnormal-value {
  color: var(--el-color-danger);
  font-weight: 600;
}

.sub-text {
  font-size: 12px;
}

.ml-4 {
  margin-left: 4px;
}
</style>
