<template>
  <div class="dashboard">
    <template v-if="!memberStore.loaded">
      <div class="bento">
        <div class="col-12 lg-col-6"><AcSkeleton variant="card" /></div>
        <div class="col-6 lg-col-3"><AcSkeleton variant="card" /></div>
        <div class="col-6 lg-col-3"><AcSkeleton variant="card" /></div>
        <div v-for="index in 4" :key="index" class="col-6 lg-col-3"><AcSkeleton variant="card" /></div>
      </div>
    </template>

    <AcGlassCard v-else-if="!memberStore.currentMember" class="welcome" spotlight>
      <div class="welcome-art">
        <div class="orb orb-1" />
        <div class="orb orb-2" />
        <HeartPulse :size="30" />
      </div>
      <h2 class="welcome-title">欢迎使用智能健康助手</h2>
      <p class="welcome-desc">一个账号管好全家的健康：体检解读、运动记录、个性建议与健康问答。</p>
      <div class="welcome-steps">
        <div class="step"><span>1</span>添加家庭成员</div>
        <div class="step-arrow">→</div>
        <div class="step"><span>2</span>上传体检报告</div>
        <div class="step-arrow">→</div>
        <div class="step"><span>3</span>生成解读与建议</div>
      </div>
      <button type="button" class="primary-action" @click="router.push('/members/new')">
        添加第一位成员
        <ArrowRight :size="16" />
      </button>
    </AcGlassCard>

    <div v-else class="bento">
      <AcGlassCard class="col-12 lg-col-6 hero" spotlight>
        <div class="hero-content">
          <div class="hero-greeting">{{ greeting }}，{{ userStore.displayName }}</div>
          <div class="hero-name">
            <span class="ac-gradient-text">{{ memberStore.currentMember.name }}</span>
            <span class="hero-relation">{{ labelOf(RELATION_LABELS, memberStore.currentMember.relation) }}</span>
          </div>
          <div class="hero-sub">
            {{ genderText }} · {{ memberStore.currentMember.age ?? '-' }} 岁
            <template v-if="overview.bmi"> · BMI {{ overview.bmi }}</template>
          </div>
          <div class="hero-chips">
            <span class="hero-chip" :class="{ danger: hasAbnormal }">
              <TriangleAlert v-if="hasAbnormal" :size="13" />
              <ShieldCheck v-else :size="13" />
              {{ hasAbnormal ? `${abnormalCount} 项指标异常` : '指标无明显异常' }}
            </span>
            <span class="hero-chip">
              <CalendarClock :size="13" />
              {{ overview.lastReportDate ? `最近体检 ${overview.lastReportDate}` : '暂无体检记录' }}
            </span>
          </div>
        </div>
        <div class="hero-decor" />
      </AcGlassCard>

      <AcGlassCard class="col-6 lg-col-3 score" spotlight>
        <ScoreRing :value="healthScore" :label="scoreLabel" />
      </AcGlassCard>

      <AcGlassCard class="col-6 lg-col-3 actions" spotlight>
        <div class="actions-title">快捷操作</div>
        <div class="actions-grid">
          <button v-for="action in quickActions" :key="action.label" type="button" class="action-tile" @click="router.push(action.path)">
            <span class="action-icon" :class="`tone-${action.tone}`">
              <component :is="action.icon" :size="17" />
            </span>
            {{ action.label }}
          </button>
        </div>
      </AcGlassCard>

      <StatCard class="col-6 lg-col-3" label="当前 BMI" :value="bmiValue" unit="kg/m²" :icon="Scale" tone="primary" hint="体重/身高²" :decimals="1" :spark="weightSpark" />
      <StatCard class="col-6 lg-col-3" label="异常指标（最近报告）" :value="abnormalCount" unit="项" :icon="Activity" :tone="hasAbnormal ? 'coral' : 'mint'" />
      <StatCard class="col-6 lg-col-3" label="本周活动时长" :value="weekActiveMinutes" unit="分钟" :icon="Timer" tone="mint" :spark="activitySpark" />
      <StatCard class="col-6 lg-col-3" label="本周训练次数" :value="weekWorkoutCount" unit="次" :icon="Dumbbell" tone="primary" />

      <AcGlassCard class="col-12 lg-col-8" spotlight>
        <div class="panel-head">
          <span class="panel-title">体征趋势</span>
          <div class="segmented">
            <button
              v-for="option in vitalOptions"
              :key="option.value"
              type="button"
              class="segmented-item"
              :class="{ active: vitalType === option.value }"
              @click="switchVital(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
        <div v-if="vitalPoints.length > 1" class="vital-chart">
          <Sparkline :data="vitalPoints.map((point) => point.value)" :color="vitalColor" :height="150" />
          <div class="vital-meta">
            <span>最近：{{ lastVitalText }}</span>
            <span class="text-t3">{{ vitalPoints.length }} 条记录</span>
          </div>
        </div>
        <AcEmptyState v-else art="data" title="暂无体征数据" description="在运动记录页填写体重、血压或血糖后即可看到趋势。">
          <template #action>
            <el-button type="primary" @click="router.push('/exercise')">去记录</el-button>
          </template>
        </AcEmptyState>
      </AcGlassCard>

      <AcGlassCard class="col-12 lg-col-4" spotlight>
        <div class="panel-head">
          <span class="panel-title">近期动态</span>
          <button type="button" class="link-btn" @click="router.push('/reports')">全部报告</button>
        </div>
        <div v-if="timeline.length" class="timeline">
          <div v-for="item in timeline" :key="item.id" class="timeline-item" @click="router.push(item.path)">
            <span class="timeline-dot" :class="item.tone" />
            <div class="timeline-body">
              <div class="timeline-title">{{ item.title }}</div>
              <div class="timeline-time">{{ item.time }}</div>
            </div>
          </div>
        </div>
        <AcEmptyState v-else art="report" title="还没有动态" description="上传一份体检报告，AI 马上开始工作。" />
      </AcGlassCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Activity,
  ArrowRight,
  CalendarClock,
  Dumbbell,
  FileUp,
  HeartPulse,
  MessageCircle,
  ShieldCheck,
  Sparkles,
  Scale,
  Timer,
  TriangleAlert,
} from 'lucide-vue-next'
import AcGlassCard from '@/components/ui/AcGlassCard.vue'
import AcSkeleton from '@/components/ui/AcSkeleton.vue'
import AcEmptyState from '@/components/ui/AcEmptyState.vue'
import Sparkline from '@/components/biz/Sparkline.vue'
import ScoreRing from '@/components/biz/ScoreRing.vue'
import StatCard from '@/components/biz/StatCard.vue'
import { chartApi } from '@/api/chart'
import { reportApi } from '@/api/report'
import { useMemberStore } from '@/stores/member'
import { useUserStore } from '@/stores/user'
import { RELATION_LABELS, labelOf } from '@/utils/dict'

const router = useRouter()
const memberStore = useMemberStore()
const userStore = useUserStore()

const overview = reactive<Record<string, number | string | null>>({})
const weightTrend = ref<{ date: string; value: number }[]>([])
const activityTrend = ref<{ steps: number; activeMinutes: number }[]>([])
const timeline = ref<{ id: string; title: string; time: string; tone: string; path: string }[]>([])
const vitalType = ref<'weight' | 'bp' | 'glucose'>('weight')
const vitalPoints = ref<{ value: number }[]>([])

const vitalOptions = [
  { label: '体重', value: 'weight' as const },
  { label: '血压', value: 'bp' as const },
  { label: '血糖', value: 'glucose' as const },
]

const quickActions = [
  { label: '上传报告', path: '/reports', icon: FileUp, tone: 'primary' },
  { label: '记录运动', path: '/exercise', icon: Dumbbell, tone: 'mint' },
  { label: '问 AI', path: '/chat', icon: MessageCircle, tone: 'violet' },
  { label: '生成建议', path: '/suggestions', icon: Sparkles, tone: 'amber' },
]

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const genderText = computed(() => (memberStore.currentMember?.gender === 2 ? '女' : '男'))

const abnormalCount = computed(() => Number(overview.abnormalCount ?? 0))
const hasAbnormal = computed(() => abnormalCount.value > 0)
const bmiValue = computed(() => (overview.bmi === null || overview.bmi === undefined ? null : Number(overview.bmi)))
const weekActiveMinutes = computed(() => Number(overview.weekActiveMinutes ?? 0))
const weekWorkoutCount = computed(() => Number(overview.weekWorkoutCount ?? 0))

const healthScore = computed(() => {
  let score = 100
  const abnormal = Number(overview.abnormalCount ?? 0)
  score -= Math.min(30, abnormal * 8)
  const bmi = Number(overview.bmi ?? 0)
  if (bmi > 24) {
    score -= Math.min(20, (bmi - 24) * 5)
  } else if (bmi > 0 && bmi < 18.5) {
    score -= Math.min(20, (18.5 - bmi) * 5)
  }
  const active = Number(overview.weekActiveMinutes ?? 0)
  if (active < 150) {
    score -= Math.min(10, (150 - active) / 12)
  }
  return Math.max(5, Math.round(score))
})

const scoreLabel = computed(() => {
  if (healthScore.value >= 85) return '状态优秀'
  if (healthScore.value >= 65) return '状态良好'
  return '需要关注'
})

const weightSpark = computed(() => weightTrend.value.map((point) => point.value))

const activitySpark = computed(() => activityTrend.value.map((point) => point.activeMinutes))

const vitalColor = computed(() => {
  if (vitalType.value === 'weight') return 'var(--ac-cyan)'
  if (vitalType.value === 'bp') return 'var(--ac-coral)'
  return 'var(--ac-amber)'
})

const lastVitalText = computed(() => {
  const last = vitalPoints.value[vitalPoints.value.length - 1]
  return last ? String(last.value) : '-'
})

onMounted(load)
watch(() => memberStore.currentMemberId, load)

async function load(): Promise<void> {
  const memberId = memberStore.currentMemberId
  if (!memberId) {
    return
  }
  const [overviewData, weights, activity, reports] = await Promise.all([
    chartApi.overview(memberId),
    chartApi.vitalTrend(memberId, 'weight'),
    chartApi.exerciseTrend(memberId, 'day'),
    reportApi.list({ memberId, pageNum: 1, pageSize: 5 }),
  ])
  Object.assign(overview, overviewData)
  weightTrend.value = weights.map((point) => ({ date: String(point.date), value: Number(point.value ?? 0) })).filter((point) => point.value > 0)
  activityTrend.value = activity.map((point) => ({
    steps: Number(point.steps ?? 0),
    activeMinutes: Number(point.activeMinutes ?? 0),
  }))
  timeline.value = reports.list.map((report) => ({
    id: report.id,
    title: `${report.reportName}（${report.indicatorCount} 项指标）`,
    time: relativeTime(report.createdAt),
    tone: report.abnormalCount > 0 ? 'tone-coral' : 'tone-mint',
    path: `/reports/${report.id}`,
  }))
  await switchVital(vitalType.value)
}

async function switchVital(type: 'weight' | 'bp' | 'glucose'): Promise<void> {
  vitalType.value = type
  const memberId = memberStore.currentMemberId
  if (!memberId) {
    return
  }
  const points = await chartApi.vitalTrend(memberId, type)
  if (type === 'bp') {
    vitalPoints.value = points.map((point) => ({ value: Number(point.systolic ?? 0) })).filter((point) => point.value > 0)
  } else {
    vitalPoints.value = points.map((point) => ({ value: Number(point.value ?? 0) })).filter((point) => point.value > 0)
  }
}

function relativeTime(value?: string): string {
  if (!value) {
    return ''
  }
  const time = new Date(value.replace(' ', 'T')).getTime()
  const diff = Date.now() - time
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return value.slice(0, 10)
}
</script>

<style scoped>
.dashboard {
  padding-bottom: 24px;
}

.bento {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 16px;
}

.col-6 {
  grid-column: span 6;
}

.col-12 {
  grid-column: span 12;
}

@media (min-width: 1024px) {
  .lg-col-3 {
    grid-column: span 3;
  }

  .lg-col-4 {
    grid-column: span 4;
  }

  .lg-col-6 {
    grid-column: span 6;
  }

  .lg-col-8 {
    grid-column: span 8;
  }
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 56px 24px;
}

.welcome-art {
  position: relative;
  display: grid;
  place-items: center;
  width: 92px;
  height: 92px;
  margin-bottom: 18px;
  border-radius: 26px;
  color: #fff;
  background-image: var(--ac-gradient-primary);
  box-shadow: var(--ac-shadow-glow);
}

.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(12px);
  opacity: 0.6;
  animation: welcome-float 4s ease-in-out infinite;
}

.orb-1 {
  width: 36px;
  height: 36px;
  top: -8px;
  left: -10px;
  background: color-mix(in srgb, var(--ac-cyan) 50%, transparent);
}

.orb-2 {
  width: 28px;
  height: 28px;
  bottom: -6px;
  right: -8px;
  background: color-mix(in srgb, var(--ac-violet) 55%, transparent);
  animation-delay: 1.4s;
}

@keyframes welcome-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-7px);
  }
}

.welcome-title {
  margin: 0;
  font-size: 22px;
  color: var(--ac-text-1);
}

.welcome-desc {
  margin: 8px 0 22px;
  font-size: 13.5px;
  color: var(--ac-text-2);
  max-width: 420px;
  line-height: 1.8;
}

.welcome-steps {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 26px;
  flex-wrap: wrap;
  justify-content: center;
}

.step {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-primary) 8%, transparent);
  border: 1px solid var(--ac-border);
}

.step span {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
  background-image: var(--ac-gradient-primary);
}

.step-arrow {
  color: var(--ac-text-3);
}

.primary-action {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 22px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  background-image: var(--ac-gradient-primary);
  box-shadow: 0 12px 28px -14px color-mix(in srgb, var(--ac-primary) 80%, transparent);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.24s ease;
}

.primary-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px -16px color-mix(in srgb, var(--ac-primary) 85%, transparent);
}

.hero {
  position: relative;
  overflow: hidden;
  min-height: 180px;
  display: flex;
  align-items: center;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-greeting {
  font-size: 13px;
  color: var(--ac-text-2);
}

.hero-name {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 8px 0 10px;
  font-size: 30px;
  font-weight: 700;
}

.hero-relation {
  font-size: 12px;
  font-weight: 500;
  color: var(--ac-text-2);
  padding: 3px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ac-text-1) 7%, transparent);
}

.hero-sub {
  font-size: 13px;
  color: var(--ac-text-2);
}

.hero-chips {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 11px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-text-1) 5%, transparent);
  border: 1px solid var(--ac-border);
}

.hero-chip.danger {
  color: var(--ac-coral);
  background: var(--ac-danger-bg);
  border-color: color-mix(in srgb, var(--ac-coral) 30%, transparent);
}

.hero-decor {
  position: absolute;
  right: -70px;
  top: -70px;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  background: conic-gradient(
    from 0deg,
    color-mix(in srgb, var(--ac-primary) 26%, transparent),
    color-mix(in srgb, var(--ac-violet) 20%, transparent),
    color-mix(in srgb, var(--ac-cyan) 22%, transparent),
    color-mix(in srgb, var(--ac-primary) 26%, transparent)
  );
  filter: blur(38px);
  opacity: 0.65;
  animation: hero-spin 26s linear infinite;
}

@keyframes hero-spin {
  to {
    transform: rotate(360deg);
  }
}

body.ac-lite .hero-decor {
  animation: none;
}

.score {
  display: grid;
  place-items: center;
}

.actions-title {
  font-size: 12.5px;
  color: var(--ac-text-2);
  margin-bottom: 10px;
}

.actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.action-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 12px 6px;
  border: 1px solid var(--ac-border);
  border-radius: 14px;
  background: color-mix(in srgb, var(--ac-surface-solid) 45%, transparent);
  color: var(--ac-text-2);
  font-size: 12px;
  cursor: pointer;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.action-tile:hover {
  transform: translateY(-2px);
  border-color: var(--ac-border-strong);
  color: var(--ac-text-1);
  box-shadow: var(--ac-shadow-2);
}

.action-icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
}

.tone-primary {
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 13%, transparent);
}

.tone-mint {
  color: var(--ac-mint);
  background: var(--ac-success-bg);
}

.tone-violet {
  color: var(--ac-violet);
  background: color-mix(in srgb, var(--ac-violet) 14%, transparent);
}

.tone-amber {
  color: var(--ac-amber);
  background: var(--ac-warning-bg);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-title {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.segmented {
  display: inline-flex;
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

.vital-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ac-text-2);
}

.text-t3 {
  color: var(--ac-text-3);
}

.link-btn {
  border: none;
  background: transparent;
  color: var(--ac-primary);
  font-size: 12.5px;
  cursor: pointer;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-item {
  display: flex;
  gap: 10px;
  padding: 9px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.18s ease;
}

.timeline-item:hover {
  background: color-mix(in srgb, var(--ac-primary) 7%, transparent);
}

.timeline-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 999px;
  flex: none;
}

.tone-coral {
  background: var(--ac-coral);
  box-shadow: 0 0 0 4px var(--ac-danger-bg);
}

.tone-mint {
  background: var(--ac-mint);
  box-shadow: 0 0 0 4px var(--ac-success-bg);
}

.timeline-title {
  font-size: 13px;
  color: var(--ac-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-time {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--ac-text-3);
}
</style>
