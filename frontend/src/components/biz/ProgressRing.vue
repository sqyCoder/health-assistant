<template>
  <span class="progress-ring" :class="{ indeterminate }" :style="style">
    <span class="ring-hole" />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    progress?: number
    indeterminate?: boolean
    size?: number
    thickness?: number
    tone?: string
  }>(),
  {
    progress: 0,
    indeterminate: false,
    size: 40,
    thickness: 4,
    tone: 'var(--ac-primary)',
  },
)

const style = computed(() => {
  const clamped = Math.max(0, Math.min(100, props.progress))
  return {
    width: `${props.size}px`,
    height: `${props.size}px`,
    background: props.indeterminate
      ? `conic-gradient(from 0deg, transparent 0deg, ${props.tone} 300deg, transparent 360deg)`
      : `conic-gradient(${props.tone} ${clamped * 3.6}deg, color-mix(in srgb, var(--ac-text-1) 8%, transparent) 0deg)`,
    padding: `${props.thickness}px`,
  }
})
</script>

<style scoped>
.progress-ring {
  position: relative;
  display: inline-grid;
  place-items: center;
  border-radius: 999px;
  -webkit-mask: radial-gradient(farthest-side, transparent calc(100% - var(--ring-mask, 5px)), #000 calc(100% - var(--ring-mask, 5px)));
  mask: radial-gradient(farthest-side, transparent calc(100% - var(--ring-mask, 5px)), #000 calc(100% - var(--ring-mask, 5px)));
}

.ring-hole {
  width: 60%;
  height: 60%;
  border-radius: 999px;
  background: var(--ac-surface-solid);
}

.indeterminate {
  animation: ring-rotate 1.1s linear infinite;
}

@keyframes ring-rotate {
  to {
    transform: rotate(360deg);
  }
}

body.ac-lite .indeterminate {
  animation-duration: 2.4s;
}
</style>
