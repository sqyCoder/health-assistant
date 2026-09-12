<template>
  <div v-if="variant === 'card'" class="sk-card">
    <div class="ac-shimmer sk-line" style="width: 42%; height: 16px" />
    <div class="ac-shimmer sk-line" style="width: 68%; height: 28px" />
    <div class="ac-shimmer sk-line" style="width: 100%; height: 44px" />
  </div>
  <div v-else-if="variant === 'chart'" class="sk-chart">
    <div class="ac-shimmer sk-chart-area" />
  </div>
  <div v-else-if="variant === 'text'" class="sk-text">
    <div
      v-for="index in rows"
      :key="index"
      class="ac-shimmer sk-line"
      :style="{ width: `${100 - (index - 1) * 14}%`, height: '12px' }"
    />
  </div>
  <div v-else class="sk-table">
    <div class="ac-shimmer sk-line" style="width: 100%; height: 38px" />
    <div
      v-for="index in rows"
      :key="index"
      class="ac-shimmer sk-line"
      style="width: 100%; height: 44px"
    />
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'card' | 'table' | 'chart' | 'text'
    rows?: number
  }>(),
  {
    variant: 'card',
    rows: 4,
  },
)
</script>

<style scoped>
.sk-card,
.sk-table,
.sk-text {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sk-line {
  border-radius: 10px;
}

.sk-chart {
  display: grid;
  place-items: center;
  height: 100%;
  min-height: 220px;
}

.sk-chart-area {
  width: 100%;
  height: 100%;
  border-radius: 14px;
}
</style>
