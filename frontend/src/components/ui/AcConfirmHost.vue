<template>
  <Teleport to="body">
    <Transition name="confirm">
      <div v-if="ui.confirm.visible" class="confirm-overlay" @click.self="ui.resolveConfirm(false)">
        <div class="confirm-card ac-glass" role="dialog" aria-modal="true">
          <div class="confirm-icon" :class="{ danger: ui.confirm.danger }">
            <TriangleAlert v-if="ui.confirm.danger" :size="22" />
            <CircleHelp v-else :size="22" />
          </div>
          <div class="confirm-title">{{ ui.confirm.title }}</div>
          <div v-if="ui.confirm.content" class="confirm-content">{{ ui.confirm.content }}</div>
          <div class="confirm-actions">
            <button type="button" class="btn ghost" @click="ui.resolveConfirm(false)">
              {{ ui.confirm.cancelText }}
            </button>
            <button
              type="button"
              class="btn"
              :class="{ danger: ui.confirm.danger }"
              @click="ui.resolveConfirm(true)"
            >
              {{ ui.confirm.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { CircleHelp, TriangleAlert } from 'lucide-vue-next'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()

function onKeydown(event: KeyboardEvent): void {
  if (!ui.confirm.visible) {
    return
  }
  if (event.key === 'Escape') {
    ui.resolveConfirm(false)
  } else if (event.key === 'Enter') {
    ui.resolveConfirm(true)
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 400;
  display: grid;
  place-items: center;
  background: var(--ac-overlay);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.confirm-card {
  width: min(400px, calc(100vw - 40px));
  padding: 24px 22px 18px;
  border-radius: 20px;
  text-align: center;
}

.confirm-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  border-radius: 16px;
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.confirm-icon.danger {
  color: var(--ac-coral);
  background: var(--ac-danger-bg);
  animation: confirm-pulse 1.6s ease-in-out infinite;
}

@keyframes confirm-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 transparent;
  }
  50% {
    box-shadow: 0 0 0 8px color-mix(in srgb, var(--ac-coral) 12%, transparent);
  }
}

.confirm-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.confirm-content {
  margin-top: 8px;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ac-text-2);
  word-break: break-word;
}

.confirm-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.btn {
  flex: 1;
  height: 38px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  color: #fff;
  background-image: var(--ac-gradient-primary);
  transition: transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.2s ease;
}

.btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px -12px color-mix(in srgb, var(--ac-primary) 70%, transparent);
}

.btn:active {
  transform: scale(0.97);
}

.btn.danger {
  background-image: var(--ac-gradient-danger);
}

.btn.ghost {
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-text-1) 6%, transparent);
}

.btn.ghost:hover {
  box-shadow: none;
  color: var(--ac-text-1);
}

.confirm-enter-active {
  transition: opacity 0.28s ease;
}

.confirm-enter-active .confirm-card {
  transition: transform 0.34s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.28s ease;
}

.confirm-leave-active {
  transition: opacity 0.18s ease;
}

.confirm-enter-from,
.confirm-leave-to {
  opacity: 0;
}

.confirm-enter-from .confirm-card {
  transform: scale(0.94) translateY(8px);
  opacity: 0;
}

body.ac-lite .confirm-icon.danger {
  animation: none;
}
</style>
