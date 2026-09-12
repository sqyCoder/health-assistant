<template>
  <div class="indicator-bar" :title="tooltip">
    <div class="track">
      <div class="range" :style="rangeStyle" />
      <div class="marker" :class="`tone-${tone}`" :style="markerStyle" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value: number | null | undefined
  refMin?: number | null
  refMax?: number | null
  status?: string | null
}>()

const hasRange = computed(() => props.refMin !== null && props.refMin !== undefined
  && props.refMax !== null && props.refMax !== undefined && Number(props.refMax) > Number(props.refMin))

const rangeStyle = computed(() => {
  if (!hasRange.value) {
    return { left: '12%', right: '12%' }
  }
  return { left: '12%', right: '12%' }
})

const tone = computed(() => {
  const status = props.status ?? ''
  if (['HIGH', 'ABNORMAL', 'POSITIVE'].includes(status)) return 'coral'
  if (status === 'LOW') return 'amber'
  if (['NORMAL', 'NEGATIVE'].includes(status)) return 'mint'
  return 'gray'
})

const markerRatio = computed(() => {
  if (!hasRange.value || props.value === null || props.value === undefined || Number.isNaN(Number(props.value))) {
    return null
  }
  const min = Number(props.refMin)
  const max = Number(props.refMax)
  const value = Number(props.value)
  const ratio = (value - min) / (max - min)
  if (props.status === 'HIGH' || ratio > 1) {
    return Math.min(1.06, 1 + Math.min(0.06, (ratio - 1) * 0.5))
  }
  if (props.status === 'LOW' || ratio < 0) {
    return Math.max(-0.06, ratio * 0.5)
  }
  return ratio
})

const markerStyle = computed(() => {
  if (markerRatio.value === null) {
    return { display: 'none' }
  }
  const clamped = Math.max(-0.06, Math.min(1.06, markerRatio.value))
  const percent = 12 + clamped * 76
  return { left: `${percent}%` }
})

const tooltip = computed(() => {
  if (props.value === null || props.value === undefined) {
    return ''
  }
  if (!hasRange.value) {
    return `${props.value}`
  }
  return `${props.value}（参考 ${props.refMin} ~ ${props.refMax}）`
})
</script>

<style scoped>
.indicator-bar {
  width: 100%;
  min-width: 90px;
}

.track {
  position: relative;
  height: 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ac-text-1) 6%, transparent);
  overflow: visible;
}

.range {
  position: absolute;
  top: 0;
  bottom: 0;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ac-mint) 26%, transparent);
}

.marker {
  position: absolute;
  top: 50%;
  width: 12px;
  height: 12px;
  margin-left: -6px;
  border-radius: 999px;
  transform: translateY(-50%);
  border: 2px solid var(--ac-surface-solid);
  box-shadow: var(--ac-shadow-1);
  transition: left 0.5s cubic-bezier(0.2, 0, 0, 1);
}

.marker.tone-mint {
  background: var(--ac-mint);
}

.marker.tone-amber {
  background: var(--ac-amber);
}

.marker.tone-coral {
  background: var(--ac-coral);
  animation: marker-breath 2s ease-in-out infinite;
}

.marker.tone-gray {
  background: var(--ac-text-3);
}

@keyframes marker-breath {
  0%,
  100% {
    box-shadow: 0 0 0 0 color-mix(in srgb, var(--ac-coral) 30%, transparent);
  }
  50% {
    box-shadow: 0 0 0 6px transparent;
  }
}

body.ac-lite .marker.tone-coral {
  animation: none;
}
</style>
