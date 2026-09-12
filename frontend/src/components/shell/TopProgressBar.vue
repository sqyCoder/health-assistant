<template>
  <Teleport to="body">
    <Transition name="bar">
      <div v-if="ui.progress.visible" class="top-progress">
        <div class="top-progress-fill" :style="{ width: `${ui.progress.value}%` }" />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const router = useRouter()
let trickle: ReturnType<typeof setInterval> | null = null
let removeBefore: (() => void) | null = null
let removeAfter: (() => void) | null = null
let removeError: (() => void) | null = null

function start(): void {
  ui.progressStart()
  if (trickle) {
    clearInterval(trickle)
  }
  trickle = setInterval(() => {
    if (!ui.progress.visible) {
      return
    }
    const remain = 90 - ui.progress.value
    if (remain > 0.5) {
      ui.progress.value += remain * 0.12
    }
  }, 240)
}

function done(): void {
  if (trickle) {
    clearInterval(trickle)
    trickle = null
  }
  ui.progressDone()
}

onMounted(() => {
  removeBefore = router.beforeEach((_to, _from, next) => {
    start()
    next()
  })
  removeAfter = router.afterEach(() => done())
  removeError = router.onError(() => done())
})

onBeforeUnmount(() => {
  removeBefore?.()
  removeAfter?.()
  removeError?.()
  if (trickle) {
    clearInterval(trickle)
  }
})
</script>

<style scoped>
.top-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 600;
  pointer-events: none;
}

.top-progress-fill {
  height: 100%;
  background: var(--ac-gradient-primary);
  box-shadow: 0 0 12px color-mix(in srgb, var(--ac-primary) 70%, transparent);
  border-radius: 0 999px 999px 0;
  transition: width 0.24s cubic-bezier(0.2, 0, 0, 1);
}

.bar-leave-active {
  transition: opacity 0.3s ease;
}

.bar-leave-to {
  opacity: 0;
}
</style>
