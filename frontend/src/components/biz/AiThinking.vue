<template>
  <div class="ai-thinking">
    <div class="orb">
      <div class="orb-core" />
      <div class="orb-ring ring-1" />
      <div class="orb-ring ring-2" />
    </div>
    <div class="thinking-text">{{ messages[index % messages.length] }}</div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    messages?: string[]
  }>(),
  {
    messages: () => ['正在解读数据…', '正在生成内容…'],
  },
)

const index = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  timer = setInterval(() => {
    index.value += 1
  }, 1900)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})

void props
</script>

<style scoped>
.ai-thinking {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 34px 0;
}

.orb {
  position: relative;
  width: 72px;
  height: 72px;
}

.orb-core {
  position: absolute;
  inset: 18px;
  border-radius: 999px;
  background-image: var(--ac-gradient-primary);
  box-shadow: 0 0 26px -4px color-mix(in srgb, var(--ac-primary) 70%, transparent);
  animation: core-breathe 2s ease-in-out infinite;
}

.orb-ring {
  position: absolute;
  border-radius: 999px;
  border: 1.5px solid color-mix(in srgb, var(--ac-primary) 40%, transparent);
}

.ring-1 {
  inset: 6px;
  animation: ring-spin 3.4s linear infinite;
  border-top-color: transparent;
  border-right-color: transparent;
}

.ring-2 {
  inset: 0;
  animation: ring-spin 5s linear infinite reverse;
  border-bottom-color: transparent;
  border-left-color: transparent;
}

@keyframes core-breathe {
  0%,
  100% {
    transform: scale(0.94);
  }
  50% {
    transform: scale(1.06);
  }
}

@keyframes ring-spin {
  to {
    transform: rotate(360deg);
  }
}

.thinking-text {
  font-size: 13px;
  color: var(--ac-text-2);
  animation: text-fade 1.9s ease-in-out infinite;
}

@keyframes text-fade {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
}

body.ac-lite .orb-core,
body.ac-lite .orb-ring,
body.ac-lite .thinking-text {
  animation: none;
}
</style>
