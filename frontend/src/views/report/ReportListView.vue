<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">体检报告</h2>
        <p class="page-sub">上传电子版 PDF，AI 自动提取指标并解读</p>
      </div>
      <div class="head-actions">
        <button type="button" class="ghost-action" @click="manualVisible = true">
          <PenLine :size="15" /> 手动录入
        </button>
        <button type="button" class="primary-action" @click="openUpload">
          <FileUp :size="15" /> 上传 PDF 报告
        </button>
      </div>
    </div>

    <div class="toolbar">
      <div class="segmented">
        <button
          v-for="option in typeOptions"
          :key="option.value ?? 'all'"
          type="button"
          class="segmented-item"
          :class="{ active: filterType === option.value }"
          @click="filterType = option.value; load()"
        >
          {{ option.label }}
        </button>
      </div>
      <span v-if="!memberStore.currentMember" class="toolbar-hint">请先在右上角选择一个成员</span>
    </div>

    <div v-if="loading" class="report-list">
      <div v-for="index in 3" :key="index" class="report-skeleton ac-glass">
        <div class="ac-shimmer skeleton-ring" />
        <div class="skeleton-lines">
          <div class="ac-shimmer skeleton-line" />
          <div class="ac-shimmer skeleton-line short" />
        </div>
      </div>
    </div>

    <AcEmptyState
      v-else-if="reports.length === 0"
      art="report"
      title="暂无体检报告"
      description="上传一份电子版体检 PDF，10 秒左右即可自动提取全部指标。"
    >
      <template #action>
        <button type="button" class="primary-action" @click="openUpload">
          <FileUp :size="15" /> 上传第一份报告
        </button>
      </template>
    </AcEmptyState>

    <div v-else v-auto-animate class="report-list">
      <div
        v-for="report in reports"
        :key="report.id"
        class="report-row ac-glass ac-glass-hover"
        @click="router.push(`/reports/${report.id}`)"
      >
        <div class="row-status">
          <ProgressRing v-if="isParsing(report.parseStatus)" :indeterminate="true" />
          <span v-else class="status-icon" :class="statusTone(report.parseStatus)">
            <Loader2 v-if="isParsing(report.parseStatus)" :size="18" class="spin" />
            <FileCheck2 v-else-if="report.parseStatus === 'CONFIRMED'" :size="18" />
            <ScanLine v-else-if="report.parseStatus === 'PARSED'" :size="18" />
            <FileWarning v-else-if="report.parseStatus === 'PARSE_FAILED'" :size="18" />
            <FileText v-else :size="18" />
          </span>
        </div>

        <div class="row-main">
          <div class="row-title-line">
            <span class="row-title">{{ report.reportName }}</span>
            <StatusPill
              :label="labelOf(PARSE_STATUS_LABELS, report.parseStatus)"
              :tone="statusPillTone(report.parseStatus)"
              :pulse="isParsing(report.parseStatus)"
            />
            <span v-if="report.sourceType === 'PDF'" class="row-source">PDF</span>
            <span v-else class="row-source">手动</span>
          </div>
          <div class="row-meta">
            <span><CalendarDays :size="12" /> {{ report.reportDate }}</span>
            <span v-if="report.institution"><Building2 :size="12" /> {{ report.institution }}</span>
            <span><ListChecks :size="12" /> {{ report.indicatorCount ?? 0 }} 项指标</span>
            <span v-if="report.abnormalCount > 0" class="row-abnormal">
              <TriangleAlert :size="12" /> {{ report.abnormalCount }} 项异常
            </span>
          </div>
        </div>

        <div class="row-actions">
          <button v-if="report.sourceType === 'PDF'" type="button" class="row-btn" @click.stop="reparse(report)">
            <RefreshCw :size="14" /> 重解析
          </button>
          <button type="button" class="row-btn danger" @click.stop="remove(report)">
            <Trash2 :size="14" />
          </button>
          <ChevronRight :size="16" class="row-arrow" />
        </div>
      </div>
    </div>

    <el-dialog v-model="uploadVisible" title="上传 PDF 体检报告" width="520px">
      <div class="dropzone" :class="{ dragging }" @dragover.prevent="dragging = true" @dragleave="dragging = false" @drop.prevent="onDrop">
        <input ref="fileInputRef" type="file" accept=".pdf,application/pdf" class="file-input" @change="onFilePicked" />
        <div class="dropzone-inner" @click="fileInputRef?.click()">
          <span class="dropzone-icon"><UploadCloud :size="22" /></span>
          <template v-if="uploadFile">
            <div class="dropzone-file">{{ uploadFile.name }}</div>
            <div class="dropzone-hint">{{ (uploadFile.size / 1024 / 1024).toFixed(1) }} MB · 点击重新选择</div>
          </template>
          <template v-else>
            <div class="dropzone-title">拖拽 PDF 到此处，或点击选择</div>
            <div class="dropzone-hint">仅支持电子版 PDF（≤20MB），扫描件请手动录入</div>
          </template>
        </div>
      </div>

      <div class="upload-form">
        <label class="form-row">
          <span>报告名称</span>
          <input v-model="uploadForm.reportName" placeholder="选填，默认 姓名+体检报告" />
        </label>
        <label class="form-row">
          <span>体检日期 *</span>
          <input v-model="uploadForm.reportDate" type="date" />
        </label>
        <label class="form-row">
          <span>体检机构</span>
          <input v-model="uploadForm.institution" placeholder="选填" />
        </label>
        <label class="form-row">
          <span>报告类型</span>
          <select v-model="uploadForm.reportType">
            <option v-for="(label, value) in REPORT_TYPE_LABELS" :key="value" :value="value">{{ label }}</option>
          </select>
        </label>
      </div>

      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传并解析</el-button>
      </template>
    </el-dialog>

    <IndicatorEditorDialog v-model="manualVisible" title="手动录入报告" :show-meta="true" :loading="savingManual" @submit="submitManual" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Building2,
  CalendarDays,
  ChevronRight,
  FileCheck2,
  FileText,
  FileUp,
  FileWarning,
  ListChecks,
  Loader2,
  PenLine,
  RefreshCw,
  ScanLine,
  Trash2,
  TriangleAlert,
  UploadCloud,
} from 'lucide-vue-next'
import { vAutoAnimate } from '@formkit/auto-animate/vue'
import AcEmptyState from '@/components/ui/AcEmptyState.vue'
import StatusPill from '@/components/biz/StatusPill.vue'
import ProgressRing from '@/components/biz/ProgressRing.vue'
import IndicatorEditorDialog from '@/components/IndicatorEditorDialog.vue'
import { reportApi, uploadReport } from '@/api/report'
import { useMemberStore } from '@/stores/member'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import { PARSE_STATUS_LABELS, REPORT_TYPE_LABELS, labelOf } from '@/utils/dict'
import type { IndicatorItem, Report } from '@/types'

const router = useRouter()
const memberStore = useMemberStore()
const confirm = useConfirm()
const toast = useToast()

const loading = ref(false)
const uploading = ref(false)
const savingManual = ref(false)
const reports = ref<Report[]>([])
const filterType = ref<string | undefined>()
const uploadVisible = ref(false)
const manualVisible = ref(false)
const dragging = ref(false)
const uploadFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement>()
let pollTimer: ReturnType<typeof setInterval> | null = null
let pollCount = 0

const uploadForm = reactive({
  reportName: '',
  reportDate: new Date().toISOString().slice(0, 10),
  institution: '',
  reportType: 'ANNUAL',
})

const typeOptions = computed(() => [
  { label: '全部', value: undefined as string | undefined },
  ...Object.entries(REPORT_TYPE_LABELS).map(([value, label]) => ({ label, value: value as string | undefined })),
])

onMounted(load)
watch(() => memberStore.currentMemberId, load)
onBeforeUnmount(stopPolling)

async function load(): Promise<void> {
  const memberId = memberStore.currentMemberId
  if (!memberId) {
    reports.value = []
    return
  }
  loading.value = true
  try {
    const page = await reportApi.list({
      memberId,
      reportType: filterType.value,
      pageNum: 1,
      pageSize: 50,
    })
    reports.value = page.list
    if (page.list.some((report) => isParsing(report.parseStatus))) {
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
    pollCount += 1
    if (pollCount > 40) {
      stopPolling()
      return
    }
    const memberId = memberStore.currentMemberId
    if (!memberId) {
      return
    }
    const page = await reportApi.list({ memberId, reportType: filterType.value, pageNum: 1, pageSize: 50 })
    reports.value = page.list
    if (!page.list.some((report) => isParsing(report.parseStatus))) {
      stopPolling()
      toast.success('报告解析完成')
    }
  }, 4000)
}

function stopPolling(): void {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function isParsing(status: string): boolean {
  return status === 'UPLOADED' || status === 'PARSING'
}

function statusTone(status: string): string {
  if (status === 'CONFIRMED') return 'tone-mint'
  if (status === 'PARSED') return 'tone-blue'
  if (status === 'PARSE_FAILED' || status === 'NEED_MANUAL') return 'tone-coral'
  return 'tone-gray'
}

function statusPillTone(status: string): 'mint' | 'amber' | 'coral' | 'blue' | 'gray' {
  if (status === 'CONFIRMED') return 'mint'
  if (status === 'PARSED') return 'amber'
  if (status === 'PARSE_FAILED' || status === 'NEED_MANUAL') return 'coral'
  if (isParsing(status)) return 'blue'
  return 'gray'
}

function openUpload(): void {
  if (!memberStore.currentMemberId) {
    toast.info('请先选择一个成员')
    return
  }
  uploadVisible.value = true
}

function onDrop(event: DragEvent): void {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    setFile(file)
  }
}

function onFilePicked(event: Event): void {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (file) {
    setFile(file)
  }
}

function setFile(file: File): void {
  if (!file.name.toLowerCase().endsWith('.pdf') && file.type !== 'application/pdf') {
    toast.error('文件类型不支持', '请选择 PDF 文件')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    toast.error('文件过大', '请选择 20MB 以内的 PDF')
    return
  }
  uploadFile.value = file
}

async function submitUpload(): Promise<void> {
  if (!uploadFile.value) {
    toast.error('请先选择 PDF 文件')
    return
  }
  if (!uploadForm.reportDate) {
    toast.error('请选择体检日期')
    return
  }
  const memberId = memberStore.currentMemberId
  if (!memberId) {
    return
  }
  uploading.value = true
  try {
    await uploadReport(uploadFile.value, {
      memberId,
      reportName: uploadForm.reportName || undefined,
      reportDate: uploadForm.reportDate,
      institution: uploadForm.institution || undefined,
      reportType: uploadForm.reportType,
    })
    uploadVisible.value = false
    uploadFile.value = null
    toast.success('上传成功', 'AI 正在后台解析，完成后会自动刷新')
    await load()
  } catch (error) {
    toast.error('上传失败', error instanceof Error ? error.message : undefined)
  } finally {
    uploading.value = false
  }
}

async function submitManual(payload: {
  meta: { reportName: string; reportDate: string; institution: string; reportType: string } | null
  indicators: IndicatorItem[]
}): Promise<void> {
  const memberId = memberStore.currentMemberId
  if (!memberId || !payload.meta) {
    return
  }
  savingManual.value = true
  try {
    const detail = await reportApi.createManual({
      memberId,
      reportName: payload.meta.reportName || undefined,
      reportDate: payload.meta.reportDate,
      institution: payload.meta.institution || undefined,
      reportType: payload.meta.reportType,
      indicators: payload.indicators,
    })
    manualVisible.value = false
    toast.success(`已录入 ${detail.indicators.length} 项指标`)
    router.push(`/reports/${detail.report.id}`)
  } finally {
    savingManual.value = false
  }
}

async function reparse(report: Report): Promise<void> {
  await reportApi.parse(report.id)
  toast.success('已提交重新解析')
  await load()
}

async function remove(report: Report): Promise<void> {
  const confirmed = await confirm.danger(`删除报告「${report.reportName}」？`, '报告及其全部指标明细将被删除，云存储中的文件也会移除。')
  if (!confirmed) {
    return
  }
  await reportApi.remove(report.id)
  toast.success('已删除')
  await load()
}
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.page-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--ac-text-3);
}

.head-actions {
  display: flex;
  gap: 10px;
}

.primary-action,
.ghost-action {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 40px;
  padding: 0 18px;
  border-radius: 12px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.24s ease, border-color 0.2s ease;
}

.primary-action {
  border: none;
  color: #fff;
  background-image: var(--ac-gradient-primary);
  box-shadow: 0 12px 26px -14px color-mix(in srgb, var(--ac-primary) 80%, transparent);
}

.primary-action:hover {
  transform: translateY(-2px);
}

.ghost-action {
  border: 1px solid var(--ac-border);
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-surface-solid) 55%, transparent);
}

.ghost-action:hover {
  border-color: var(--ac-border-strong);
  color: var(--ac-text-1);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.segmented {
  display: inline-flex;
  flex-wrap: wrap;
  padding: 3px;
  gap: 2px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ac-text-1) 6%, transparent);
}

.segmented-item {
  padding: 6px 14px;
  border: none;
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--ac-text-2);
  background: transparent;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.segmented-item.active {
  color: var(--ac-text-1);
  font-weight: 600;
  background: var(--ac-surface-solid);
  box-shadow: var(--ac-shadow-1);
}

.toolbar-hint {
  font-size: 12.5px;
  color: var(--ac-text-3);
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  cursor: pointer;
  transition: transform 0.24s cubic-bezier(0.2, 0, 0, 1), border-color 0.2s ease, box-shadow 0.24s ease;
}

.report-row:hover {
  transform: translateY(-2px);
}

.row-status {
  flex: none;
}

.status-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 13px;
}

.tone-mint {
  color: var(--ac-mint);
  background: var(--ac-success-bg);
}

.tone-blue {
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.tone-coral {
  color: var(--ac-coral);
  background: var(--ac-danger-bg);
}

.tone-gray {
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-text-1) 7%, transparent);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.row-title {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.row-source {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10.5px;
  color: var(--ac-text-3);
  border: 1px solid var(--ac-border);
}

.row-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--ac-text-3);
  flex-wrap: wrap;
}

.row-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.row-abnormal {
  color: var(--ac-coral) !important;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
}

.row-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border: 1px solid var(--ac-border);
  border-radius: 9px;
  background: transparent;
  font-size: 12px;
  color: var(--ac-text-2);
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, background-color 0.18s ease;
}

.row-btn:hover {
  border-color: var(--ac-border-strong);
  color: var(--ac-text-1);
}

.row-btn.danger:hover {
  color: var(--ac-coral);
  border-color: color-mix(in srgb, var(--ac-coral) 45%, transparent);
  background: var(--ac-danger-bg);
}

.row-arrow {
  color: var(--ac-text-3);
  transition: transform 0.2s ease;
}

.report-row:hover .row-arrow {
  transform: translateX(3px);
  color: var(--ac-primary);
}

.dropzone {
  border: 1.5px dashed var(--ac-border-strong);
  border-radius: 16px;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.dropzone.dragging,
.dropzone:hover {
  border-color: color-mix(in srgb, var(--ac-primary) 55%, transparent);
  background: color-mix(in srgb, var(--ac-primary) 6%, transparent);
}

.file-input {
  display: none;
}

.dropzone-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 28px 16px;
  cursor: pointer;
  text-align: center;
}

.dropzone-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 15px;
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.dropzone-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.dropzone-file {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ac-primary);
  word-break: break-all;
}

.dropzone-hint {
  font-size: 12px;
  color: var(--ac-text-3);
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.form-row {
  display: grid;
  grid-template-columns: 92px 1fr;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--ac-text-2);
}

.form-row input,
.form-row select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--ac-border);
  border-radius: 11px;
  background: color-mix(in srgb, var(--ac-surface-solid) 70%, transparent);
  color: var(--ac-text-1);
  font-size: 13.5px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-row input:focus,
.form-row select:focus {
  border-color: color-mix(in srgb, var(--ac-primary) 55%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ac-primary) 16%, transparent);
}

.report-skeleton {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border-radius: 18px;
}

.skeleton-ring {
  width: 40px;
  height: 40px;
  border-radius: 13px;
}

.skeleton-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-line {
  width: 45%;
  height: 13px;
  border-radius: 7px;
}

.skeleton-line.short {
  width: 28%;
  height: 11px;
}
</style>
