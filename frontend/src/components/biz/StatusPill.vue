<template>
  <span class="pill" :class="[`tone-${tone}`, { pulse }]">
    <span class="dot" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    tone?: 'mint' | 'amber' | 'coral' | 'blue' | 'gray' | 'violet'
    pulse?: boolean
  }>(),
  { tone: 'gray', pulse: false },
)
</script>

<style scoped>
.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: currentColor;
}

.tone-mint {
  color: var(--ac-mint);
  background: var(--ac-success-bg);
}

.tone-amber {
  color: var(--ac-amber);
  background: var(--ac-warning-bg);
}

.tone-coral {
  color: var(--ac-coral);
  background: var(--ac-danger-bg);
}

.tone-blue {
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.tone-violet {
  color: var(--ac-violet);
  background: color-mix(in srgb, var(--ac-violet) 14%, transparent);
}

.tone-gray {
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-text-1) 7%, transparent);
}

.pulse .dot {
  animation: pill-pulse 1.6s ease-in-out infinite;
}

@keyframes pill-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 color-mix(in srgb, currentColor 45%, transparent);
  }
  50% {
    box-shadow: 0 0 0 5px transparent;
  }
}

body.ac-lite .pulse .dot {
  animation: none;
}
</style>
