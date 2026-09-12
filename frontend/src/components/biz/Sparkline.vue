<template>
  <svg class="spark" :viewBox="`0 0 100 ${height}`" preserveAspectRatio="none" :style="{ height: `${height}px` }">
    <defs>
      <linearGradient :id="gradientId" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :stop-color="color" stop-opacity="0.32" />
        <stop offset="100%" :stop-color="color" stop-opacity="0" />
      </linearGradient>
    </defs>
    <path v-if="areaPath" :d="areaPath" :fill="`url(#${gradientId})`" />
    <path v-if="linePath" :d="linePath" fill="none" :stroke="color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    data: number[]
    color?: string
    height?: number
  }>(),
  {
    color: 'var(--ac-primary)',
    height: 34,
  },
)

const gradientId = `spark-${Math.random().toString(36).slice(2, 9)}`

const normalized = computed(() => {
  const values = props.data.filter((value) => Number.isFinite(value))
  if (values.length < 2) {
    return []
  }
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min || 1
  const step = 100 / (values.length - 1)
  return values.map((value, index) => ({
    x: index * step,
    y: props.height - 3 - ((value - min) / span) * (props.height - 8),
  }))
})

const linePath = computed(() => {
  if (normalized.value.length === 0) {
    return ''
  }
  return normalized.value
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(2)},${point.y.toFixed(2)}`)
    .join(' ')
})

const areaPath = computed(() => {
  if (!linePath.value) {
    return ''
  }
  const first = normalized.value[0]
  const last = normalized.value[normalized.value.length - 1]
  return `${linePath.value} L${last.x.toFixed(2)},${props.height} L${first.x.toFixed(2)},${props.height} Z`
})
</script>

<style scoped>
.spark {
  display: block;
  width: 100%;
}
</style>
