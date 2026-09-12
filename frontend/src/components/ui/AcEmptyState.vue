<template>
  <div class="empty">
    <div class="empty-art">
      <div class="orb orb-1" />
      <div class="orb orb-2" />
      <div class="art-core">
        <FileSearch v-if="art === 'report'" :size="28" />
        <Activity v-else-if="art === 'data'" :size="28" />
        <MessageCircleDashed v-else-if="art === 'chat'" :size="28" />
        <SearchX v-else-if="art === 'search'" :size="28" />
        <Inbox v-else :size="28" />
      </div>
    </div>
    <div class="empty-title">{{ title }}</div>
    <div v-if="description" class="empty-desc">{{ description }}</div>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Activity, FileSearch, Inbox, MessageCircleDashed, SearchX } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    art?: 'report' | 'data' | 'chat' | 'search' | 'none'
    title: string
    description?: string
  }>(),
  { art: 'none' },
)
</script>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 44px 20px;
  text-align: center;
}

.empty-art {
  position: relative;
  width: 96px;
  height: 96px;
  margin-bottom: 16px;
}

.art-core {
  position: absolute;
  inset: 22px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
  box-shadow: 0 0 0 1px var(--ac-border) inset;
  z-index: 1;
}

.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(10px);
  opacity: 0.7;
  animation: empty-float 4s ease-in-out infinite;
}

.orb-1 {
  width: 34px;
  height: 34px;
  top: 2px;
  left: 6px;
  background: color-mix(in srgb, var(--ac-violet) 45%, transparent);
}

.orb-2 {
  width: 26px;
  height: 26px;
  right: 2px;
  bottom: 6px;
  background: color-mix(in srgb, var(--ac-cyan) 45%, transparent);
  animation-delay: 1.2s;
}

@keyframes empty-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

body.ac-lite .orb {
  animation: none;
}

.empty-title {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.empty-desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ac-text-3);
  max-width: 360px;
  line-height: 1.7;
}

.empty-action {
  margin-top: 16px;
}
</style>
