<template>
  <div class="page-card interpret-panel">
    <div class="panel-head">
      <div class="panel-title-wrap">
        <span class="panel-title">AI 大白话解读</span>
        <span v-if="interpretation?.status === 'DONE'" class="panel-badge">
          <Sparkles :size="12" /> {{ interpretation.tokenIn ?? 0 }} + {{ interpretation.tokenOut ?? 0 }} tokens
        </span>
      </div>
      <div class="panel-actions">
        <button type="button" class="ghost-action" :disabled="!hasIndicators || generating" @click="generate">
          <RefreshCw v-if="interpretation?.status === 'DONE'" :size="14" />
          <Sparkles v-else :size="14" />
          {{ interpretation?.status === 'DONE' ? '重新生成' : '生成解读' }}
        </button>
        <button v-if="interpretation?.status === 'DONE'" type="button" class="ghost-action" :disabled="exporting" @click="exportPdf">
          <Loader2 v-if="exporting" :size="14" class="spin" />
          <Download v-else :size="14" />
          {{ exporting ? '导出中…' : '导出 PDF' }}
        </button>
        <button v-if="interpretation?.status === 'DONE'" type="button" class="primary-action" @click="goSuggestion">
          <Dumbbell :size="14" /> 生成锻炼建议
        </button>
      </div>
    </div>

    <AiThinking v-if="generating || interpretation?.status === 'PROCESSING'" :messages="thinkingMessages" />

    <div v-else-if="interpretation?.status === 'FAILED'" class="failed-box">
      <TriangleAlert :size="16" />
      <span>生成失败：{{ interpretation.errorMsg || '请重试' }}</span>
      <button type="button" class="ghost-action" @click="generate">重试</button>
    </div>

    <template v-else-if="content">
      <div class="risk-banner" :class="`risk-${content.riskLevel ?? 'MEDIUM'}`">
        <div class="risk-gauge">
          <component :is="riskIcon" :size="20" />
        </div>
        <div>
          <div class="risk-level">{{ labelOf(RISK_LEVEL_LABELS, content.riskLevel ?? 'MEDIUM') }}</div>
          <div class="risk-summary">{{ content.summary }}</div>
        </div>
      </div>

      <div v-if="content.focusMetrics?.length" class="focus-row">
        <span class="focus-label">重点关注</span>
        <span v-for="item in content.focusMetrics" :key="item" class="focus-chip">
          <Flame :size="12" /> {{ item }}
        </span>
      </div>

      <div class="card-list">
        <div
          v-for="(card, index) in content.indicators ?? []"
          :key="card.name"
          class="indicator-card"
          :class="{ abnormal: isAbnormal(card.status) }"
          :style="{ animationDelay: `${index * 55}ms` }"
        >
          <div class="indicator-head">
            <span class="indicator-name">{{ card.name }}</span>
            <span class="indicator-value ac-num" :class="{ abnormal: isAbnormal(card.status) }">
              {{ card.value }} <small>{{ card.unit }}</small>
            </span>
            <StatusPill :label="labelOf(JUDGE_STATUS_LABELS, card.status)" :tone="judgePillTone(card.status)" />
            <span class="indicator-ref">参考 {{ card.refRange || '-' }}</span>
          </div>
          <div class="indicator-explain">{{ card.plainExplain }}</div>
          <div class="indicator-advice">
            <Lightbulb :size="13" />
            {{ card.advice }}
          </div>
        </div>
      </div>

      <div class="disclaimer">{{ content.disclaimer || '以上内容基于体检数据由 AI 生成，仅供参考，不构成医疗诊断。' }}</div>
    </template>

    <AcEmptyState
      v-else
      art="data"
      :title="hasIndicators ? '还没有解读' : '报告暂无指标'"
      :description="hasIndicators ? '点击右上角「生成解读」，AI 会把每一项指标翻译成大白话。' : '先完成报告解析或手动录入指标后再生成解读。'"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Download,
  Dumbbell,
  Flame,
  Lightbulb,
  Loader2,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
  TriangleAlert,
} from 'lucide-vue-next'
import AiThinking from '@/components/biz/AiThinking.vue'
import StatusPill from '@/components/biz/StatusPill.vue'
import AcEmptyState from '@/components/ui/AcEmptyState.vue'
import { exportApi, fileApi } from '@/api/export'
import { interpretationApi } from '@/api/interpretation'
import { taskApi } from '@/api/task'
import { useToast } from '@/composables/useToast'
import { JUDGE_STATUS_LABELS, RISK_LEVEL_LABELS, labelOf } from '@/utils/dict'
import type { Interpretation, InterpretationContent } from '@/types'

const props = defineProps<{
  reportId: string
  hasIndicators: boolean
}>()

const router = useRouter()
const toast = useToast()
const interpretation = ref<Interpretation | null>(null)
const generating = ref(false)
const exporting = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

const thinkingMessages = ['正在读懂每一项指标…', '正在翻译成大白话…', '正在评估风险等级…', '正在生成可执行建议…']

const content = computed<InterpretationContent | null>(() => {
  if (!interpretation.value?.contentJson) {
    return null
  }
  try {
    return JSON.parse(interpretation.value.contentJson) as InterpretationContent
  } catch {
    return null
  }
})

const riskIcon = computed(() => {
  const level = content.value?.riskLevel
  return level === 'LOW' ? ShieldCheck : level === 'HIGH' ? ShieldAlert : TriangleAlert
})

onMounted(load)
onBeforeUnmount(stopPolling)

async function load(): Promise<void> {
  interpretation.value = await interpretationApi.latest(props.reportId)
  if (interpretation.value?.status === 'PROCESSING') {
    startPolling()
  }
}

function startPolling(): void {
  if (pollTimer) {
    return
  }
  pollTimer = setInterval(async () => {
    const latest = await interpretationApi.latest(props.reportId)
    interpretation.value = latest
    if (latest?.status !== 'PROCESSING') {
      stopPolling()
      if (latest?.status === 'DONE') {
        toast.success('解读生成完成')
      }
    }
  }, 3000)
}

function stopPolling(): void {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function generate(): Promise<void> {
  generating.value = true
  try {
    const { taskId } = await interpretationApi.generate(props.reportId)
    for (let i = 0; i < 60; i++) {
      await new Promise((resolve) => setTimeout(resolve, 3000))
      const task = await taskApi.get(taskId)
      if (task.status === 'SUCCESS' || task.status === 'FAILED') {
        if (task.status === 'FAILED') {
          toast.error('生成失败', task.errorMsg || '请重试')
        }
        break
      }
    }
    interpretation.value = await interpretationApi.latest(props.reportId)
    if (interpretation.value?.status === 'DONE') {
      toast.success('解读生成完成')
    }
  } finally {
    generating.value = false
  }
}

async function exportPdf(): Promise<void> {
  if (!interpretation.value) {
    return
  }
  exporting.value = true
  try {
    const { exportId, taskId } = await exportApi.create('INTERPRETATION', interpretation.value.id)
    for (let i = 0; i < 40; i++) {
      await new Promise((resolve) => setTimeout(resolve, 2000))
      const task = await taskApi.get(taskId)
      if (task.status === 'SUCCESS' || task.status === 'FAILED') {
        if (task.status === 'FAILED') {
          toast.error('导出失败', '请稍后重试')
          return
        }
        break
      }
    }
    const record = await exportApi.get(exportId)
    if (!record.fileId) {
      toast.error('导出文件生成失败')
      return
    }
    const file = await fileApi.url(record.fileId)
    window.open(file.url, '_blank')
    toast.success('PDF 已生成')
  } finally {
    exporting.value = false
  }
}

function goSuggestion(): void {
  router.push({ path: '/suggestions', query: { sourceReport: props.reportId } })
}

function isAbnormal(status: string): boolean {
  return ['HIGH', 'LOW', 'ABNORMAL', 'POSITIVE'].includes(status)
}

function judgePillTone(status: string): 'mint' | 'amber' | 'coral' | 'blue' | 'gray' {
  if (['NORMAL', 'NEGATIVE'].includes(status)) return 'mint'
  if (status === 'LOW') return 'amber'
  if (['HIGH', 'ABNORMAL', 'POSITIVE'].includes(status)) return 'coral'
  return 'gray'
}
</script>

<style scoped>
.interpret-panel {
  border-radius: 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.panel-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.panel-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  color: var(--ac-violet);
  background: color-mix(in srgb, var(--ac-violet) 12%, transparent);
}

.panel-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.primary-action,
.ghost-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 11px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1), border-color 0.2s ease, opacity 0.2s ease;
}

.primary-action {
  border: none;
  color: #fff;
  background-image: var(--ac-gradient-primary);
}

.ghost-action {
  border: 1px solid var(--ac-border);
  color: var(--ac-text-2);
  background: transparent;
}

.ghost-action:hover:not(:disabled) {
  border-color: var(--ac-border-strong);
  color: var(--ac-text-1);
}

.primary-action:hover:not(:disabled),
.ghost-action:hover:not(:disabled) {
  transform: translateY(-1px);
}

.primary-action:disabled,
.ghost-action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.failed-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 14px;
  font-size: 13.5px;
  color: var(--ac-coral);
  background: var(--ac-danger-bg);
}

.risk-banner {
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 16px;
  margin-bottom: 16px;
}

.risk-LOW {
  background: linear-gradient(135deg, color-mix(in srgb, var(--ac-mint) 14%, transparent), color-mix(in srgb, var(--ac-cyan) 10%, transparent));
  border: 1px solid color-mix(in srgb, var(--ac-mint) 30%, transparent);
}

.risk-MEDIUM {
  background: linear-gradient(135deg, color-mix(in srgb, var(--ac-amber) 14%, transparent), color-mix(in srgb, var(--ac-coral) 8%, transparent));
  border: 1px solid color-mix(in srgb, var(--ac-amber) 32%, transparent);
}

.risk-HIGH {
  background: linear-gradient(135deg, color-mix(in srgb, var(--ac-coral) 16%, transparent), color-mix(in srgb, var(--ac-amber) 10%, transparent));
  border: 1px solid color-mix(in srgb, var(--ac-coral) 34%, transparent);
}

.risk-gauge {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  flex: none;
  color: var(--ac-text-1);
  background: color-mix(in srgb, var(--ac-surface-solid) 55%, transparent);
}

.risk-level {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--ac-text-1);
  margin-bottom: 4px;
}

.risk-summary {
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ac-text-2);
}

.focus-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.focus-label {
  font-size: 12.5px;
  color: var(--ac-text-3);
}

.focus-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 11px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--ac-coral);
  background: var(--ac-danger-bg);
  border: 1px solid color-mix(in srgb, var(--ac-coral) 26%, transparent);
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.indicator-card {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--ac-border);
  background: color-mix(in srgb, var(--ac-surface-solid) 55%, transparent);
  animation: card-in 0.5s cubic-bezier(0.05, 0.7, 0.1, 1) both;
  transition: transform 0.2s cubic-bezier(0.2, 0, 0, 1), box-shadow 0.24s ease, border-color 0.2s ease;
}

.indicator-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--ac-shadow-2);
  border-color: var(--ac-border-strong);
}

.indicator-card.abnormal {
  border-left: 3px solid var(--ac-coral);
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

body.ac-lite .indicator-card {
  animation: none;
}

.indicator-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.indicator-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.indicator-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.indicator-value.abnormal {
  color: var(--ac-coral);
}

.indicator-value small {
  font-size: 11px;
  font-weight: 500;
  color: var(--ac-text-3);
}

.indicator-ref {
  font-size: 11.5px;
  color: var(--ac-text-3);
}

.indicator-explain {
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ac-text-2);
}

.indicator-advice {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin-top: 9px;
  padding: 9px 12px;
  border-radius: 11px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 8%, transparent);
}

.disclaimer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--ac-border);
  font-size: 12px;
  color: var(--ac-text-3);
}
</style>
