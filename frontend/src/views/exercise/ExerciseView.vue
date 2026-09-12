<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">运动记录</h2>
        <p class="page-sub">日常活动与训练双轨记录，坚持看得见</p>
      </div>
      <div class="head-actions">
        <button type="button" class="ghost-action" @click="openWorkout(null)">
          <Plus :size="15" /> 新增训练
        </button>
        <button type="button" class="primary-action" @click="openDaily(selectedDate)">
          <PencilLine :size="15" /> 记录今日
        </button>
      </div>
    </div>

    <div v-if="!memberStore.currentMember" class="hint-text">请先在右上角选择一个成员</div>

    <template v-else>
      <div class="ring-row">
        <div class="ring-card ac-glass">
          <ProgressRing :progress="weekActivePercent" :size="86" :thickness="7" tone="var(--ac-mint)" />
          <div class="ring-info">
            <div class="ring-value ac-num">{{ weekActiveMinutes }}<small>/150</small></div>
            <div class="ring-label">本周活动（分钟）</div>
          </div>
        </div>
        <div class="ring-card ac-glass">
          <ProgressRing :progress="monthCaloriesPercent" :size="86" :thickness="7" tone="var(--ac-amber)" />
          <div class="ring-info">
            <div class="ring-value ac-num">{{ monthCalories }}<small>/2000</small></div>
            <div class="ring-label">本月消耗（kcal）</div>
          </div>
        </div>
        <div class="ring-card ac-glass">
          <ProgressRing :progress="monthWorkoutPercent" :size="86" :thickness="7" tone="var(--ac-primary)" />
          <div class="ring-info">
            <div class="ring-value ac-num">{{ monthWorkoutCount }}<small>/8</small></div>
            <div class="ring-label">本月训练（次）</div>
          </div>
        </div>
      </div>

      <div class="panel ac-glass">
        <div class="panel-head">
          <span class="panel-title">活动热力图</span>
          <span class="panel-sub">点击任意日期快速记录</span>
        </div>
        <HeatmapCalendar :data="heatmapData" :weeks="20" @select="openDaily" />
      </div>

      <div class="panel ac-glass">
        <div class="panel-head">
          <span class="panel-title">训练记录</span>
          <div class="segmented">
            <button
              v-for="option in typeFilters"
              :key="option.value ?? 'all'"
              type="button"
              class="segmented-item"
              :class="{ active: workoutFilterType === option.value }"
              @click="workoutFilterType = option.value; loadWorkouts()"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <AcEmptyState
          v-if="workouts.length === 0"
          art="data"
          title="还没有训练记录"
          description="记录一次快走、跑步或力量训练，开始积累你的运动轨迹。"
        >
          <template #action>
            <button type="button" class="primary-action" @click="openWorkout(null)">
              <Plus :size="15" /> 新增训练
            </button>
          </template>
        </AcEmptyState>

        <div v-else v-auto-animate class="workout-list">
          <div v-for="workout in workouts" :key="workout.id" class="workout-card">
            <span class="workout-icon" :style="workoutIconStyle(workout.workoutType)">
              <component :is="workoutIcon(workout.workoutType)" :size="18" />
            </span>
            <div class="workout-main">
              <div class="workout-title-line">
                <span class="workout-title">{{ workout.title || labelOf(WORKOUT_TYPE_LABELS, workout.workoutType) }}</span>
                <span class="workout-date">{{ workout.workoutDate }}</span>
              </div>
              <div class="workout-meta">
                <span v-if="workout.durationMin"><Timer :size="12" /> {{ workout.durationMin }} 分钟</span>
                <span v-if="workout.distanceKm"><Route :size="12" /> {{ workout.distanceKm }} km</span>
                <span v-if="workout.avgHeartRate"><HeartPulse :size="12" /> {{ workout.avgHeartRate }} bpm</span>
                <span v-if="workout.caloriesKcal"><Flame :size="12" /> {{ workout.caloriesKcal }} kcal</span>
                <span v-if="workout.intensity" class="intensity">
                  <i class="intensity-bar" :class="`i-${workout.intensity}`" /> {{ labelOf(INTENSITY_LABELS, workout.intensity) }}
                </span>
              </div>
            </div>
            <div class="workout-actions">
              <button type="button" class="row-btn" @click="openWorkout(workout)"><Pencil :size="13" /></button>
              <button type="button" class="row-btn danger" @click="removeWorkout(workout)"><Trash2 :size="13" /></button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-drawer v-model="dailyDrawerVisible" :title="`记录 ${selectedDate}`" size="420px" direction="rtl">
      <div class="drawer-form">
        <label v-for="field in dailyFields" :key="field.key" class="form-row">
          <span>{{ field.label }}</span>
          <el-input-number
            v-if="field.type === 'number'"
            v-model="dailyForm[field.key]"
            :min="0"
            :max="field.max ?? 100000"
            :precision="field.precision ?? 0"
            controls-position="right"
            style="width: 100%"
          />
          <el-select v-else-if="field.type === 'select'" v-model="dailyForm[field.key]" clearable style="width: 100%">
            <el-option v-for="(label, value) in field.options" :key="value" :label="label" :value="value" />
          </el-select>
          <el-input v-else v-model="dailyForm[field.key]" style="width: 100%" />
        </label>
        <el-button type="primary" :loading="savingDaily" style="width: 100%" @click="saveDaily">保存记录</el-button>
      </div>
    </el-drawer>

    <el-dialog v-model="workoutDialogVisible" :title="workoutForm.id ? '编辑训练' : '新增训练'" width="560px">
      <el-form :model="workoutForm" label-width="86px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="运动日期" required>
              <el-date-picker v-model="workoutForm.workoutDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运动类型" required>
              <el-select v-model="workoutForm.workoutType" style="width: 100%">
                <el-option v-for="(label, value) in WORKOUT_TYPE_LABELS" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标题">
              <el-input v-model="workoutForm.title" maxlength="100" placeholder="如：晨跑 5 公里" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(分)">
              <el-input-number v-model="workoutForm.durationMin" :min="0" :max="1440" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="强度">
              <el-select v-model="workoutForm.intensity" clearable style="width: 100%">
                <el-option v-for="(label, value) in INTENSITY_LABELS" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="距离(km)">
              <el-input-number v-model="workoutForm.distanceKm" :min="0" :precision="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="消耗(kcal)">
              <el-input-number v-model="workoutForm.caloriesKcal" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平均心率">
              <el-input-number v-model="workoutForm.avgHeartRate" :min="20" :max="250" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感受">
              <el-select v-model="workoutForm.feeling" clearable style="width: 100%">
                <el-option v-for="(label, value) in FEELING_LABELS" :key="value" :label="label" :value="value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="workoutForm.note" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="workoutDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingWorkout" @click="saveWorkout">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch, type Component } from 'vue'
import {
  Bike,
  Dumbbell,
  Flame,
  Footprints,
  HeartPulse,
  Mountain,
  Pencil,
  PencilLine,
  Plus,
  Route,
  Timer,
  Trash2,
  Waves,
  Zap,
} from 'lucide-vue-next'
import { vAutoAnimate } from '@formkit/auto-animate/vue'
import HeatmapCalendar from '@/components/biz/HeatmapCalendar.vue'
import ProgressRing from '@/components/biz/ProgressRing.vue'
import AcEmptyState from '@/components/ui/AcEmptyState.vue'
import { exerciseApi } from '@/api/exercise'
import { useMemberStore } from '@/stores/member'
import { useConfirm } from '@/composables/useConfirm'
import { useToast } from '@/composables/useToast'
import {
  FEELING_LABELS,
  GLUCOSE_TYPE_LABELS,
  INTENSITY_LABELS,
  MOOD_LABELS,
  SLEEP_QUALITY_LABELS,
  WORKOUT_TYPE_LABELS,
  labelOf,
} from '@/utils/dict'
import type { ExerciseDaily, Workout, WorkoutSummary } from '@/types'

const memberStore = useMemberStore()
const confirm = useConfirm()
const toast = useToast()

const today = new Date().toISOString().slice(0, 10)
const selectedDate = ref(today)
const dailyDrawerVisible = ref(false)
const dailyForm = reactive<Record<string, any>>({})
const recentDaily = ref<ExerciseDaily[]>([])
const savingDaily = ref(false)

const summary = ref<WorkoutSummary | null>(null)
const workouts = ref<Workout[]>([])
const workoutFilterType = ref<string | undefined>()
const workoutDialogVisible = ref(false)
const savingWorkout = ref(false)
const workoutForm = reactive<Partial<Workout>>({})

const dailyFields: {
  key: string
  label: string
  type: 'number' | 'select' | 'text'
  max?: number
  precision?: number
  options?: Record<string, string>
}[] = [
  { key: 'steps', label: '步数', type: 'number', max: 200000 },
  { key: 'activeMinutes', label: '活动时长(分)', type: 'number', max: 1440 },
  { key: 'caloriesKcal', label: '消耗(kcal)', type: 'number', max: 10000, precision: 1 },
  { key: 'sleepHours', label: '睡眠时长(h)', type: 'number', max: 24, precision: 1 },
  { key: 'sleepQuality', label: '睡眠质量', type: 'select', options: SLEEP_QUALITY_LABELS },
  { key: 'weightKg', label: '体重(kg)', type: 'number', max: 500, precision: 1 },
  { key: 'systolicBp', label: '收缩压', type: 'number', max: 300 },
  { key: 'diastolicBp', label: '舒张压', type: 'number', max: 200 },
  { key: 'bloodGlucose', label: '血糖(mmol/L)', type: 'number', max: 50, precision: 1 },
  { key: 'glucoseType', label: '血糖类型', type: 'select', options: GLUCOSE_TYPE_LABELS },
  { key: 'mood', label: '心情', type: 'select', options: MOOD_LABELS },
  { key: 'note', label: '备注', type: 'text' },
]

const typeFilters = computed(() => [
  { label: '全部', value: undefined as string | undefined },
  ...Object.entries(WORKOUT_TYPE_LABELS).map(([value, label]) => ({ label, value: value as string | undefined })),
])

const heatmapData = computed(() =>
  recentDaily.value
    .filter((item) => item.steps)
    .map((item) => ({ date: item.recordDate, value: Number(item.steps ?? 0) })),
)

const weekActiveMinutes = computed(() => {
  const start = new Date()
  start.setDate(start.getDate() - 6)
  const startKey = start.toISOString().slice(0, 10)
  return recentDaily.value
    .filter((item) => item.recordDate >= startKey)
    .reduce((sum, item) => sum + (item.activeMinutes ?? 0), 0)
})

const weekActivePercent = computed(() => Math.min(100, (weekActiveMinutes.value / 150) * 100))
const monthCalories = computed(() => Math.round(Number(summary.value?.totalCalories ?? 0)))
const monthCaloriesPercent = computed(() => Math.min(100, (monthCalories.value / 2000) * 100))
const monthWorkoutCount = computed(() => summary.value?.totalCount ?? 0)
const monthWorkoutPercent = computed(() => Math.min(100, (monthWorkoutCount.value / 8) * 100))

onMounted(init)
watch(() => memberStore.currentMemberId, init)

async function init(): Promise<void> {
  await Promise.all([loadRecent(), loadWorkouts()])
}

async function loadRecent(): Promise<void> {
  if (!memberStore.currentMemberId) {
    return
  }
  recentDaily.value = await exerciseApi.listDaily(memberStore.currentMemberId)
}

async function openDaily(date: string): Promise<void> {
  if (!memberStore.currentMemberId) {
    toast.info('请先选择一个成员')
    return
  }
  selectedDate.value = date
  Object.keys(dailyForm).forEach((key) => delete dailyForm[key])
  const daily = recentDaily.value.find((item) => item.recordDate === date)
    ?? await exerciseApi.getDaily(memberStore.currentMemberId, date)
  if (daily) {
    Object.assign(dailyForm, daily)
  }
  dailyDrawerVisible.value = true
}

async function saveDaily(): Promise<void> {
  if (!memberStore.currentMemberId) {
    return
  }
  savingDaily.value = true
  try {
    await exerciseApi.saveDaily(memberStore.currentMemberId, { ...dailyForm, recordDate: selectedDate.value })
    dailyDrawerVisible.value = false
    toast.success('已保存', selectedDate.value)
    await loadRecent()
  } finally {
    savingDaily.value = false
  }
}

function monthRange(): { from: string; to: string } {
  const now = new Date()
  const from = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  return { from, to: today }
}

async function loadWorkouts(): Promise<void> {
  if (!memberStore.currentMemberId) {
    workouts.value = []
    return
  }
  const range = monthRange()
  const [page, summaryData] = await Promise.all([
    exerciseApi.listWorkouts(memberStore.currentMemberId, {
      workoutType: workoutFilterType.value,
      pageNum: 1,
      pageSize: 50,
    }),
    exerciseApi.summary(memberStore.currentMemberId, range.from, range.to),
  ])
  workouts.value = page.list
  summary.value = summaryData
}

function openWorkout(workout: Workout | null): void {
  if (!memberStore.currentMemberId) {
    toast.info('请先选择一个成员')
    return
  }
  Object.keys(workoutForm).forEach((key) => delete (workoutForm as Record<string, unknown>)[key])
  if (workout) {
    Object.assign(workoutForm, workout)
  } else {
    Object.assign(workoutForm, { workoutDate: today, workoutType: 'WALKING', intensity: 'MEDIUM' })
  }
  workoutDialogVisible.value = true
}

async function saveWorkout(): Promise<void> {
  if (!memberStore.currentMemberId) {
    return
  }
  if (!workoutForm.workoutDate || !workoutForm.workoutType) {
    toast.error('请填写运动日期和类型')
    return
  }
  savingWorkout.value = true
  try {
    if (workoutForm.id) {
      await exerciseApi.updateWorkout(memberStore.currentMemberId, workoutForm.id, workoutForm)
    } else {
      await exerciseApi.addWorkout(memberStore.currentMemberId, workoutForm)
    }
    workoutDialogVisible.value = false
    toast.success('已保存')
    await loadWorkouts()
  } finally {
    savingWorkout.value = false
  }
}

async function removeWorkout(workout: Workout): Promise<void> {
  if (!memberStore.currentMemberId) {
    return
  }
  const confirmed = await confirm.danger('删除这条训练记录？', workout.title || labelOf(WORKOUT_TYPE_LABELS, workout.workoutType))
  if (!confirmed) {
    return
  }
  await exerciseApi.removeWorkout(memberStore.currentMemberId, workout.id)
  toast.success('已删除')
  await loadWorkouts()
}

const ICONS: Record<string, Component> = {
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

const HUES: Record<string, number> = {
  RUNNING: 12,
  WALKING: 150,
  CYCLING: 200,
  SWIMMING: 190,
  STRENGTH: 262,
  YOGA: 320,
  BALL_GAME: 35,
  HIKING: 95,
  OTHER: 220,
}

function workoutIcon(type: string): Component {
  return ICONS[type] ?? Footprints
}

function workoutIconStyle(type: string): Record<string, string> {
  const hue = HUES[type] ?? 220
  return {
    color: `hsl(${hue} 75% 50%)`,
    background: `hsl(${hue} 75% 50% / 0.14)`,
  }
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
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), border-color 0.2s ease;
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

.hint-text {
  font-size: 13px;
  color: var(--ac-text-3);
}

.ring-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.ring-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px;
  border-radius: 20px;
}

.ring-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.ring-value small {
  font-size: 12px;
  font-weight: 500;
  color: var(--ac-text-3);
}

.ring-label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ac-text-2);
}

.panel {
  padding: 18px;
  border-radius: 20px;
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.panel-title {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.panel-sub {
  font-size: 12px;
  color: var(--ac-text-3);
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
  padding: 5px 12px;
  border: none;
  border-radius: 999px;
  font-size: 12px;
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

.workout-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.workout-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 16px;
  border: 1px solid var(--ac-border);
  border-radius: 16px;
  background: color-mix(in srgb, var(--ac-surface-solid) 45%, transparent);
  transition: transform 0.22s cubic-bezier(0.2, 0, 0, 1), border-color 0.2s ease, box-shadow 0.22s ease;
}

.workout-card:hover {
  transform: translateY(-2px);
  border-color: var(--ac-border-strong);
  box-shadow: var(--ac-shadow-2);
}

.workout-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  flex: none;
}

.workout-main {
  flex: 1;
  min-width: 0;
}

.workout-title-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.workout-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.workout-date {
  font-size: 11.5px;
  color: var(--ac-text-3);
}

.workout-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--ac-text-3);
  flex-wrap: wrap;
}

.workout-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.intensity-bar {
  display: inline-block;
  width: 4px;
  height: 12px;
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

.workout-actions {
  display: flex;
  gap: 4px;
  flex: none;
  opacity: 0;
  transition: opacity 0.18s ease;
}

.workout-card:hover .workout-actions {
  opacity: 1;
}

body.ac-lite .workout-actions {
  opacity: 1;
}

.row-btn {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--ac-border);
  border-radius: 9px;
  background: transparent;
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

.drawer-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-row {
  display: grid;
  grid-template-columns: 110px 1fr;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--ac-text-2);
}

@media (max-width: 900px) {
  .ring-row {
    grid-template-columns: 1fr;
  }
}
</style>
