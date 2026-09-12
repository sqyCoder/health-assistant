<template>
  <Teleport to="body">
    <div class="toast-host">
      <TransitionGroup name="toast">
        <div v-for="item in ui.toasts" :key="item.id" class="toast ac-glass" :class="`toast-${item.type}`">
          <span class="toast-icon" :class="`icon-${item.type}`">
            <CheckCircle2 v-if="item.type === 'success'" :size="18" />
            <XCircle v-else-if="item.type === 'error'" :size="18" />
            <Info v-else-if="item.type === 'info'" :size="18" />
            <Loader2 v-else class="spin" :size="18" />
          </span>
          <div class="toast-body">
            <div class="toast-title">{{ item.title }}</div>
            <div v-if="item.message" class="toast-message">{{ item.message }}</div>
          </div>
          <button class="toast-close" type="button" @click="ui.removeToast(item.id)">
            <X :size="14" />
          </button>
          <span
            v-if="item.type !== 'loading'"
            class="toast-progress"
            :style="{ animationDuration: `${item.duration}ms` }"
          />
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { CheckCircle2, Info, Loader2, X, XCircle } from 'lucide-vue-next'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
</script>

<style scoped>
.toast-host {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 500;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(360px, calc(100vw - 32px));
  pointer-events: none;
}

.toast {
  position: relative;
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 12px 34px 12px 12px;
  border-radius: 14px;
  pointer-events: auto;
  overflow: hidden;
}

.toast-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  flex: none;
}

.icon-success {
  color: var(--ac-mint);
  background: var(--ac-success-bg);
}

.icon-error {
  color: var(--ac-coral);
  background: var(--ac-danger-bg);
}

.icon-info {
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.icon-loading {
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.spin {
  animation: toast-spin 1s linear infinite;
}

@keyframes toast-spin {
  to {
    transform: rotate(360deg);
  }
}

.toast-body {
  flex: 1;
  min-width: 0;
}

.toast-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.toast-message {
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--ac-text-2);
  word-break: break-word;
}

.toast-close {
  position: absolute;
  top: 10px;
  right: 8px;
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--ac-text-3);
  cursor: pointer;
}

.toast-close:hover {
  color: var(--ac-text-1);
  background: color-mix(in srgb, var(--ac-text-1) 8%, transparent);
}

.toast-progress {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 2px;
  width: 100%;
  transform-origin: left;
  background: var(--ac-gradient-primary);
  animation: toast-progress linear forwards;
}

@keyframes toast-progress {
  from {
    transform: scaleX(1);
  }
  to {
    transform: scaleX(0);
  }
}

.toast-enter-active {
  transition: all 0.32s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-leave-active {
  transition: all 0.2s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(24px) scale(0.96);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(24px) scale(0.96);
}

body.ac-lite .toast-enter-active {
  transition: opacity 0.2s ease;
}

@media (max-width: 640px) {
  .toast-host {
    top: 12px;
    right: 12px;
    left: 12px;
    width: auto;
  }
}
</style>
