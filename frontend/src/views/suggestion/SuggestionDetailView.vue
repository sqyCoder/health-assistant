<template>
  <div v-loading="loading">
    <div class="page-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="router.push('/suggestions')">返回</el-button>
        <h2 class="page-title">锻炼与康复建议</h2>
      </div>
      <div v-if="suggestion" class="header-actions">
        <el-button type="primary" :loading="exporting" @click="exportPdf">导出 PDF</el-button>
        <el-button type="danger" plain @click="remove">删除</el-button>
      </div>
    </div>

    <template v-if="suggestion">
      <div v-if="suggestion.status === 'FAILED'" class="page-card">
        <el-alert :title="`生成失败：${suggestion.errorMsg || '请重试'}`" type="error" :closable="false" />
      </div>

      <template v-else-if="suggestion.status === 'DONE'">
        <div class="page-card">
          <div class="suggestion-meta">
            <el-tag>{{ labelOf(SUGGESTION_TYPE_LABELS, suggestion.suggestionType) }}</el-tag>
            <el-tag v-if="suggestion.goal" type="success" effect="plain">目标：{{ labelOf(GOAL_LABELS, suggestion.goal) }}</el-tag>
            <span class="text-muted">{{ suggestion.createdAt }}</span>
          </div>
          <h3 class="section-title">现状评估</h3>
          <p class="assessment">{{ suggestion.assessmentMd }}</p>
        </div>

        <div class="page-card mt-16">
          <h3 class="section-title">周训练计划</h3>
          <div v-if="weeklyPlan.length" class="week-board">
            <div
              v-for="day in weeklyPlan"
              :key="day.day"
              class="day-card"
              :class="{ today: day.day === todayKey }"
            >
              <div class="day-head">
                <span class="day-name">{{ labelOf(DAY_LABELS, day.day) }}</span>
                <span v-if="day.day === todayKey" class="today-badge">今天</span>
              </div>
              <div class="day-icon">
                <component :is="workoutIcon(day.workoutType)" :size="18" />
              </div>
              <div class="day-title">{{ day.title }}</div>
              <div class="day-meta">
                <span>{{ day.durationMin ?? '-' }} 分钟</span>
                <span class="day-intensity">
                  <i class="intensity-bar" :class="`i-${day.intensity}`" />
                  {{ labelOf(INTENSITY_LABELS, day.intensity) }}
                </span>
              </div>
              <div class="day-content">{{ day.content }}</div>
              <div v-if="day.notes" class="day-notes">{{ day.notes }}</div>
            </div>
          </div>
          <AcEmptyState v-else art="data" title="暂无周计划" description="重新生成建议以获得训练安排。" />
        </div>

        <div v-if="rehabList.length" class="page-card mt-16">
          <h3 class="section-title">康复建议</h3>
          <div v-for="(item, index) in rehabList" :key="index" class="rehab-item">
            <div class="rehab-target">{{ item.target }}</div>
            <div>{{ item.action }}（{{ item.frequency }}）</div>
            <div v-if="item.caution" class="rehab-caution">注意：{{ item.caution }}</div>
          </div>
        </div>

        <div v-if="suggestion.precautionsMd" class="page-card mt-16">
          <h3 class="section-title">禁忌与注意事项</h3>
          <div class="precautions">{{ suggestion.precautionsMd }}</div>
        </div>

        <div v-if="progressMetrics.length" class="page-card mt-16">
          <h3 class="section-title">建议跟踪的指标</h3>
          <el-tag v-for="item in progressMetrics" :key="item" class="metric-tag" effect="plain">{{ item }}</el-tag>
        </div>

        <div class="disclaimer-text">以上建议由 AI 生成，仅供参考，不构成医疗建议。如有不适请及时就医。</div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  Bike,
  Dumbbell,
  Footprints,
  HeartPulse,
  Mountain,
  Timer,
  Waves,
  Zap,
} from 'lucide-vue-next'
import AcEmptyState from '@/components/ui/AcEmptyState.vue'
import { exportApi, fileApi } from '@/api/export'
import { suggestionApi } from '@/api/suggestion'
import { taskApi } from '@/api/task'
import { DAY_LABELS, GOAL_LABELS, INTENSITY_LABELS, SUGGESTION_TYPE_LABELS, labelOf } from '@/utils/dict'
import type { RehabAdvice, Suggestion, WeeklyPlanDay } from '@/types'

const route = useRoute()
const router = useRouter()
const suggestionId = computed(() => (typeof route.params.id === 'string' ? route.params.id : ''))
const loading = ref(false)
const exporting = ref(false)
const suggestion = ref<Suggestion | null>(null)
const weeklyPlan = ref<WeeklyPlanDay[]>([])
const rehabList = ref<RehabAdvice[]>([])
const progressMetrics = ref<string[]>([])

const DAY_ORDER = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
const todayKey = computed(() => DAY_ORDER[new Date().getDay()])

const WORKOUT_ICONS: Record<string, Component> = {
  RUNNING: Zap,
  WALKING: Footprints,
  CYCLING: Bike,
  SWIMMING: Waves,
  STRENGTH: Dumbbell,
  YOGA: HeartPulse,
  BALL_GAME: Timer,
  HIKING: Mountain,
  OTHER: Footprints,
}

function workoutIcon(type: string): Component {
  return WORKOUT_ICONS[type] ?? Footprints
}

onMounted(load)

async function load(): Promise<void> {
  if (!suggestionId.value) {
    return
  }
  loading.value = true
  try {
    suggestion.value = await suggestionApi.get(suggestionId.value)
    weeklyPlan.value = parseJson<WeeklyPlanDay[]>(suggestion.value.weeklyPlanJson, [])
    rehabList.value = parseJson<RehabAdvice[]>(suggestion.value.rehabJson, [])
    progressMetrics.value = parseJson<string[]>(suggestion.value.progressMetricsJson, [])
  } finally {
    loading.value = false
  }
}

function parseJson<T>(value: string | undefined, fallback: T): T {
  if (!value) {
    return fallback
  }
  try {
    return JSON.parse(value) as T
  } catch {
    return fallback
  }
}

async function exportPdf(): Promise<void> {
  if (!suggestion.value) {
    return
  }
  exporting.value = true
  try {
    const { exportId, taskId } = await exportApi.create('SUGGESTION', suggestion.value.id)
    for (let i = 0; i < 40; i++) {
      await new Promise((resolve) => setTimeout(resolve, 2000))
      const task = await taskApi.get(taskId)
      if (task.status === 'SUCCESS' || task.status === 'FAILED') {
        if (task.status === 'FAILED') {
          ElMessage.error('导出失败，请重试')
          return
        }
        break
      }
    }
    const record = await exportApi.get(exportId)
    if (!record.fileId) {
      ElMessage.error('导出文件生成失败')
      return
    }
    const file = await fileApi.url(record.fileId)
    window.open(file.url, '_blank')
  } finally {
    exporting.value = false
  }
}

async function remove(): Promise<void> {
  if (!suggestion.value) {
    return
  }
  await ElMessageBox.confirm('确定删除该建议吗？', '提示', { type: 'warning' })
  await suggestionApi.remove(suggestion.value.id)
  ElMessage.success('已删除')
  router.push('/suggestions')
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

.suggestion-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 16px;
}

.week-board {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}

.day-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 12px 14px;
  border: 1px solid var(--ac-border);
  border-radius: 16px;
  background: color-mix(in srgb, var(--ac-surface-solid) 50%, transparent);
  transition: transform 0.22s cubic-bezier(0.2, 0, 0, 1), border-color 0.2s ease, box-shadow 0.22s ease;
}

.day-card:hover {
  transform: translateY(-3px);
  border-color: var(--ac-border-strong);
  box-shadow: var(--ac-shadow-2);
}

.day-card.today {
  border-color: color-mix(in srgb, var(--ac-primary) 55%, transparent);
  box-shadow: var(--ac-shadow-glow);
}

.day-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.day-name {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--ac-text-2);
}

.today-badge {
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10px;
  color: #fff;
  background-image: var(--ac-gradient-primary);
}

.day-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 11px;
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.day-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.day-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 11.5px;
  color: var(--ac-text-3);
}

.day-intensity {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.intensity-bar {
  display: inline-block;
  width: 4px;
  height: 11px;
  border-radius: 999px;
}

.i-LOW {
  background: var(--ac-mint);
}

.i-MEDIUM {
  background: var(--ac-amber);
}

.i-HIGH {
  background: var(--ac-coral);
}

.day-content {
  font-size: 12px;
  line-height: 1.65;
  color: var(--ac-text-2);
}

.day-notes {
  padding-top: 6px;
  border-top: 1px dashed var(--ac-border);
  font-size: 11px;
  color: var(--ac-text-3);
}

@media (max-width: 1100px) {
  .week-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.assessment {
  line-height: 1.8;
  margin: 0;
}

.rehab-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 8px;
}

.rehab-target {
  font-weight: 600;
  margin-bottom: 4px;
}

.rehab-caution {
  margin-top: 4px;
  color: var(--el-color-danger);
  font-size: 13px;
}

.precautions {
  white-space: pre-wrap;
  line-height: 1.8;
}

.metric-tag {
  margin-right: 8px;
}

.disclaimer-text {
  margin-top: 14px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
