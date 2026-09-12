<template>
  <AcGlassCard spotlight padding="md" class="stat-card">
    <div class="stat-top">
      <span class="stat-icon" :class="`tone-${tone}`">
        <component :is="icon" :size="17" />
      </span>
      <span v-if="hint" class="stat-hint">{{ hint }}</span>
    </div>
    <div class="stat-value-row">
      <span class="stat-value ac-num">{{ formatted }}</span>
      <span v-if="unit" class="stat-unit">{{ unit }}</span>
    </div>
    <div class="stat-bottom">
      <span class="stat-label">{{ label }}</span>
      <Sparkline v-if="spark && spark.length > 1" :data="spark" :color="sparkColor" :height="26" class="stat-spark" />
    </div>
  </AcGlassCard>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import AcGlassCard from '@/components/ui/AcGlassCard.vue'
import Sparkline from './Sparkline.vue'
import { useCountUp } from '@/composables/useCountUp'

const props = withDefaults(
  defineProps<{
    label: string
    value: number | null | undefined
    unit?: string
    icon: Component
    tone?: 'primary' | 'mint' | 'amber' | 'coral'
    hint?: string
    spark?: number[]
    decimals?: number
  }>(),
  {
    tone: 'primary',
    decimals: 0,
  },
)

const valueRef = computed(() => props.value ?? 0)
const { display } = useCountUp(valueRef, 900)

const formatted = computed(() => {
  if (props.value === null || props.value === undefined) {
    return '-'
  }
  return display.value.toFixed(props.decimals)
})

const sparkColor = computed(() => {
  switch (props.tone) {
    case 'mint':
      return 'var(--ac-mint)'
    case 'amber':
      return 'var(--ac-amber)'
    case 'coral':
      return 'var(--ac-coral)'
    default:
      return 'var(--ac-primary)'
  }
})
</script>

<style scoped>
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 136px;
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 11px;
}

.tone-primary {
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 13%, transparent);
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

.stat-hint {
  font-size: 11px;
  color: var(--ac-text-3);
}

.stat-value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--ac-text-1);
  line-height: 1;
}

.stat-unit {
  font-size: 12px;
  color: var(--ac-text-3);
}

.stat-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}

.stat-label {
  font-size: 12.5px;
  color: var(--ac-text-2);
}

.stat-spark {
  width: 90px;
  flex: none;
}
</style>
