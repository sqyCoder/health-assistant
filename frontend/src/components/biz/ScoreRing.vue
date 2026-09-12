<template>
  <div class="score-ring" :style="{ width: `${size}px`, height: `${size}px` }">
    <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
      <defs>
        <linearGradient :id="gradientId" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" :stop-color="fromColor" />
          <stop offset="100%" :stop-color="toColor" />
        </linearGradient>
      </defs>
      <circle
        :cx="size / 2"
        :cy="size / 2"
        :r="radius"
        fill="none"
        stroke="var(--ac-border)"
        :stroke-width="stroke"
      />
      <circle
        class="ring-progress"
        :cx="size / 2"
        :cy="size / 2"
        :r="radius"
        fill="none"
        :stroke="`url(#${gradientId})`"
        :stroke-width="stroke"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        :transform="`rotate(-90 ${size / 2} ${size / 2})`"
      />
    </svg>
    <div class="ring-center">
      <div class="ring-value ac-num">{{ Math.round(display) }}</div>
      <div class="ring-label">{{ label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useCountUp } from '@/composables/useCountUp'

const props = withDefaults(
  defineProps<{
    value: number
    size?: number
    stroke?: number
    label?: string
  }>(),
  {
    size: 150,
    stroke: 10,
    label: '健康分',
  },
)

const gradientId = `ring-${Math.random().toString(36).slice(2, 9)}`
const radius = computed(() => (props.size - props.stroke) / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)

const valueRef = computed(() => props.value)
const { display } = useCountUp(valueRef, 1100)

const dashOffset = ref(circumference.value)

watch(
  () => props.value,
  () => {
    const clamped = Math.max(0, Math.min(100, props.value))
    dashOffset.value = circumference.value * (1 - clamped / 100)
  },
  { immediate: true },
)

const fromColor = computed(() => {
  if (props.value >= 80) return 'var(--ac-mint)'
  if (props.value >= 60) return 'var(--ac-cyan)'
  return 'var(--ac-amber)'
})

const toColor = computed(() => {
  if (props.value >= 80) return 'var(--ac-cyan)'
  if (props.value >= 60) return 'var(--ac-primary)'
  return 'var(--ac-coral)'
})
</script>

<style scoped>
.score-ring {
  position: relative;
  display: grid;
  place-items: center;
}

.ring-progress {
  transition: stroke-dashoffset 1.1s cubic-bezier(0.05, 0.7, 0.1, 1);
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-value {
  font-size: 30px;
  font-weight: 700;
  color: var(--ac-text-1);
}

.ring-label {
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--ac-text-3);
}
</style>
