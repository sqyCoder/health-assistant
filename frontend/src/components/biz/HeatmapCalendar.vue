<template>
  <div class="heatmap">
    <div class="heatmap-months">
      <span
        v-for="(label, index) in monthLabels"
        :key="index"
        class="heatmap-month"
        :style="{ width: `${label.span * 16}px` }"
      >
        {{ label.text }}
      </span>
    </div>
    <div class="heatmap-grid">
      <button
        v-for="(cell, index) in cells"
        :key="index"
        type="button"
        class="heatmap-cell"
        :class="[`level-${cell.level}`, { future: cell.future, today: cell.isToday }]"
        :title="cell.value === null ? cell.date : `${cell.date} · ${cell.value} 步`"
        @click="!cell.future && emit('select', cell.date)"
      />
    </div>
    <div class="heatmap-legend">
      <span class="legend-label">少</span>
      <span v-for="level in [0, 1, 2, 3, 4]" :key="level" class="heatmap-cell mini" :class="`level-${level}`" />
      <span class="legend-label">多</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface HeatmapDatum {
  date: string
  value: number
}

interface HeatmapCell {
  date: string
  value: number | null
  level: number
  future: boolean
  isToday: boolean
}

const props = withDefaults(
  defineProps<{
    data: HeatmapDatum[]
    weeks?: number
  }>(),
  { weeks: 20 },
)

const emit = defineEmits<{
  (e: 'select', date: string): void
}>()

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const cells = computed<HeatmapCell[]>(() => {
  const map = new Map(props.data.map((item) => [item.date, item.value]))
  const max = Math.max(1, ...props.data.map((item) => item.value))
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const todayKey = formatDate(today)
  const mondayOffset = (today.getDay() + 6) % 7
  const endMonday = new Date(today)
  endMonday.setDate(endMonday.getDate() - mondayOffset)
  const start = new Date(endMonday)
  start.setDate(start.getDate() - (props.weeks - 1) * 7)

  const result: HeatmapCell[] = []
  for (let week = 0; week < props.weeks; week++) {
    for (let dow = 0; dow < 7; dow++) {
      const date = new Date(start)
      date.setDate(start.getDate() + week * 7 + dow)
      const key = formatDate(date)
      const value = map.get(key)
      const future = date.getTime() > today.getTime()
      let level = 0
      if (value !== undefined && value > 0) {
        const ratio = value / max
        level = ratio > 0.75 ? 4 : ratio > 0.5 ? 3 : ratio > 0.25 ? 2 : 1
      }
      result.push({ date: key, value: value ?? null, level, future, isToday: key === todayKey })
    }
  }
  return result
})

const monthLabels = computed(() => {
  const labels: { text: string; span: number }[] = []
  const monthNames = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  for (let week = 0; week < props.weeks; week++) {
    const first = cells.value[week * 7]
    if (!first) {
      continue
    }
    const month = Number(first.date.slice(5, 7)) - 1
    if (labels.length === 0 || labels[labels.length - 1].text !== monthNames[month]) {
      labels.push({ text: monthNames[month], span: 1 })
    } else {
      labels[labels.length - 1].span += 1
    }
  }
  return labels.filter((label) => label.span >= 2)
})
</script>

<style scoped>
.heatmap {
  --hm-color: var(--ac-mint);
}

.heatmap-months {
  display: flex;
  gap: 3px;
  padding-left: 0;
  margin-bottom: 6px;
  overflow: hidden;
}

.heatmap-month {
  flex: none;
  font-size: 10.5px;
  color: var(--ac-text-3);
}

.heatmap-grid {
  display: grid;
  grid-auto-flow: column;
  grid-template-rows: repeat(7, 13px);
  grid-auto-columns: 13px;
  gap: 3px;
}

.heatmap-cell {
  width: 13px;
  height: 13px;
  padding: 0;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.14s ease;
}

.heatmap-cell:hover {
  transform: scale(1.25);
}

.heatmap-cell.future {
  opacity: 0.25;
  cursor: default;
}

.heatmap-cell.today {
  box-shadow: 0 0 0 1.5px var(--ac-primary);
}

.heatmap-cell.mini {
  width: 11px;
  height: 11px;
  cursor: default;
}

.level-0 {
  background: color-mix(in srgb, var(--ac-text-1) 7%, transparent);
}

.level-1 {
  background: color-mix(in srgb, var(--hm-color) 28%, transparent);
}

.level-2 {
  background: color-mix(in srgb, var(--hm-color) 48%, transparent);
}

.level-3 {
  background: color-mix(in srgb, var(--hm-color) 68%, transparent);
}

.level-4 {
  background: color-mix(in srgb, var(--hm-color) 92%, transparent);
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  justify-content: flex-end;
}

.legend-label {
  font-size: 10.5px;
  color: var(--ac-text-3);
}
</style>
