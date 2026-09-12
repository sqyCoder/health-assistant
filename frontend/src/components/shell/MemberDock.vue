<template>
  <div ref="dockRef" class="member-dock" data-tour="member-dock">
    <button type="button" class="dock-trigger" @click="open = !open">
      <span v-if="current" class="dock-avatar" :style="avatarStyle(current)">
        {{ current.name.charAt(0) }}
      </span>
      <span v-else class="dock-avatar empty">
        <UserPlus :size="15" />
      </span>
      <span v-if="current" class="dock-name">{{ current.name }}</span>
      <ChevronDown :size="13" class="dock-caret" :class="{ flipped: open }" />
    </button>

    <Transition name="dock">
      <div v-if="open" class="dock-panel ac-glass">
        <div class="dock-panel-title">切换成员</div>
        <div class="dock-members ac-no-scrollbar">
          <button
            v-for="(member, index) in memberStore.members"
            :key="member.id"
            type="button"
            class="dock-member"
            :class="{ active: member.id === memberStore.currentMemberId }"
            :style="{ transitionDelay: `${index * 30}ms` }"
            @click="select(member.id)"
          >
            <span class="dock-member-avatar" :style="avatarStyle(member)">
              {{ member.name.charAt(0) }}
            </span>
            <span class="dock-member-name">{{ member.name }}</span>
          </button>
          <button type="button" class="dock-member add" @click="addMember">
            <span class="dock-member-avatar add-avatar">
              <Plus :size="16" />
            </span>
            <span class="dock-member-name">新增</span>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronDown, Plus, UserPlus } from 'lucide-vue-next'
import { useMemberStore } from '@/stores/member'
import type { Member } from '@/types'

const router = useRouter()
const memberStore = useMemberStore()
const open = ref(false)
const dockRef = ref<HTMLElement>()

const current = computed(() => memberStore.currentMember)

const HUES = [232, 258, 190, 160, 28, 340]

function avatarStyle(member: Member): Record<string, string> {
  const hue = HUES[hashCode(member.id) % HUES.length]
  return {
    backgroundImage: `linear-gradient(135deg, hsl(${hue} 82% 62%), hsl(${hue + 34} 78% 56%))`,
  }
}

function hashCode(value: string): number {
  let hash = 0
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

function select(id: string): void {
  memberStore.setCurrent(id)
  open.value = false
}

function addMember(): void {
  open.value = false
  router.push('/members/new')
}

function onClickOutside(event: MouseEvent): void {
  if (open.value && dockRef.value && !dockRef.value.contains(event.target as Node)) {
    open.value = false
  }
}

onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<style scoped>
.member-dock {
  position: relative;
}

.dock-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 10px 0 6px;
  border: 1px solid var(--ac-border);
  border-radius: 999px;
  background: color-mix(in srgb, var(--ac-surface-solid) 65%, transparent);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.dock-trigger:hover {
  border-color: var(--ac-border-strong);
}

.dock-trigger:active {
  transform: scale(0.97);
}

.dock-avatar {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  flex: none;
}

.dock-avatar.empty {
  color: var(--ac-text-2);
  background: color-mix(in srgb, var(--ac-text-1) 8%, transparent);
}

.dock-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ac-text-1);
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dock-caret {
  color: var(--ac-text-3);
  transition: transform 0.24s cubic-bezier(0.2, 0, 0, 1);
}

.dock-caret.flipped {
  transform: rotate(180deg);
}

.dock-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 250px;
  padding: 12px;
  border-radius: 16px;
  z-index: 300;
}

.dock-panel-title {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ac-text-3);
  margin-bottom: 8px;
}

.dock-members {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dock-member {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  width: 64px;
  padding: 8px 4px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.dock-member:hover {
  background: color-mix(in srgb, var(--ac-primary) 8%, transparent);
  transform: translateY(-2px);
}

.dock-member.active {
  border-color: color-mix(in srgb, var(--ac-primary) 35%, transparent);
  background: color-mix(in srgb, var(--ac-primary) 10%, transparent);
}

.dock-member-avatar {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.dock-member.active .dock-member-avatar {
  box-shadow: 0 0 0 2px var(--ac-surface-solid), 0 0 0 4px color-mix(in srgb, var(--ac-primary) 55%, transparent);
}

.add-avatar {
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
  border: 1px dashed color-mix(in srgb, var(--ac-primary) 45%, transparent);
}

.dock-member-name {
  font-size: 11.5px;
  color: var(--ac-text-2);
  max-width: 58px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dock-enter-active {
  transition: opacity 0.22s ease;
}

.dock-enter-active .dock-panel {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.22s ease;
}

.dock-leave-active {
  transition: opacity 0.16s ease;
}

.dock-enter-from,
.dock-leave-to {
  opacity: 0;
}

.dock-enter-from .dock-panel {
  transform: translateY(-8px) scale(0.96);
  opacity: 0;
}
</style>
