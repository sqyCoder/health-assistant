<template>
  <header class="topbar" :class="{ scrolled }">
    <div class="topbar-left">
      <Transition name="crumb" mode="out-in">
        <h1 :key="pageTitle" class="page-title">{{ pageTitle }}</h1>
      </Transition>
      <span class="topbar-date">{{ today }}</span>
    </div>

    <div class="topbar-right">
      <button type="button" class="search-trigger" @click="ui.openPalette()">
        <Search :size="15" />
        <span class="search-label">搜索页面、成员或操作</span>
        <span class="search-keys">
          <kbd>Ctrl</kbd><kbd>K</kbd>
        </span>
      </button>

      <MemberDock />

      <button type="button" class="icon-btn" :title="themeStore.dark ? '切换到浅色' : '切换到深色'" @click="onToggleTheme">
        <Sun v-if="themeStore.dark" :size="17" />
        <Moon v-else :size="17" />
      </button>

      <div ref="menuRef" class="user-menu">
        <button type="button" class="user-trigger" @click="menuOpen = !menuOpen">
          <span class="user-avatar">{{ userStore.displayName.charAt(0) || 'U' }}</span>
        </button>
        <Transition name="menu">
          <div v-if="menuOpen" class="menu-panel ac-glass">
            <div class="menu-head">
              <div class="menu-name">{{ userStore.displayName }}</div>
              <div class="menu-sub">{{ userStore.user?.username }}</div>
            </div>
            <button type="button" class="menu-item" @click="emitAndClose('profile')">
              <UserCog :size="15" /> 个人资料
            </button>
            <button type="button" class="menu-item" @click="emitAndClose('password')">
              <KeyRound :size="15" /> 修改密码
            </button>
            <button type="button" class="menu-item" @click="emitAndClose('tour')">
              <Sparkles :size="15" /> 新手引导
            </button>
            <div class="menu-divider" />
            <button type="button" class="menu-item danger" @click="emitAndClose('logout')">
              <LogOut :size="15" /> 退出登录
            </button>
          </div>
        </Transition>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { KeyRound, LogOut, Moon, Search, Sparkles, Sun, UserCog } from 'lucide-vue-next'
import MemberDock from './MemberDock.vue'
import { useUiStore } from '@/stores/ui'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { useTour } from '@/composables/useTour'

const emit = defineEmits<{
  (e: 'profile'): void
  (e: 'password'): void
  (e: 'logout'): void
}>()

const route = useRoute()
const ui = useUiStore()
const themeStore = useThemeStore()
const userStore = useUserStore()
const { startTour } = useTour()

const menuOpen = ref(false)
const scrolled = ref(false)
const menuRef = ref<HTMLElement>()

const pageTitle = computed(() => (route.meta.title as string) || '智能健康助手')

const today = computed(() => {
  const now = new Date()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${now.getMonth() + 1} 月 ${now.getDate()} 日 · 星期${weekdays[now.getDay()]}`
})

function onToggleTheme(event: MouseEvent): void {
  themeStore.toggle({ x: event.clientX, y: event.clientY })
}

function emitAndClose(command: 'profile' | 'password' | 'tour' | 'logout'): void {
  menuOpen.value = false
  if (command === 'tour') {
    void startTour(true)
  } else if (command === 'logout') {
    emit('logout')
  } else if (command === 'profile') {
    emit('profile')
  } else if (command === 'password') {
    emit('password')
  }
}

function onScroll(): void {
  scrolled.value = window.scrollY > 8
}

function onClickOutside(event: MouseEvent): void {
  if (menuOpen.value && menuRef.value && !menuRef.value.contains(event.target as Node)) {
    menuOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  document.addEventListener('click', onClickOutside)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  document.removeEventListener('click', onClickOutside)
})
</script>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 64px;
  padding: 0 4px;
  transition: background-color 0.3s ease, backdrop-filter 0.3s ease, border-color 0.3s ease;
  border-bottom: 1px solid transparent;
}

.topbar.scrolled {
  background: color-mix(in srgb, var(--ac-surface) 88%, transparent);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  border-bottom-color: var(--ac-border);
}

body.ac-lite .topbar.scrolled {
  backdrop-filter: none;
}

.topbar-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--ac-text-1);
  white-space: nowrap;
}

.topbar-date {
  font-size: 12px;
  color: var(--ac-text-3);
  white-space: nowrap;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 10px 0 12px;
  border: 1px solid var(--ac-border);
  border-radius: 12px;
  background: color-mix(in srgb, var(--ac-surface-solid) 55%, transparent);
  color: var(--ac-text-3);
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease;
}

.search-trigger:hover {
  border-color: var(--ac-border-strong);
  color: var(--ac-text-2);
}

.search-label {
  font-size: 13px;
}

.search-keys {
  display: flex;
  gap: 3px;
}

.search-keys kbd {
  padding: 1px 5px;
  font-size: 10px;
  font-family: var(--ac-font-mono);
  border-radius: 5px;
  color: var(--ac-text-3);
  background: color-mix(in srgb, var(--ac-text-1) 7%, transparent);
  border: 1px solid var(--ac-border);
}

.icon-btn {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 1px solid var(--ac-border);
  border-radius: 12px;
  background: color-mix(in srgb, var(--ac-surface-solid) 55%, transparent);
  color: var(--ac-text-2);
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.icon-btn:hover {
  color: var(--ac-text-1);
  border-color: var(--ac-border-strong);
}

.icon-btn:active {
  transform: scale(0.94);
}

.user-menu {
  position: relative;
}

.user-trigger {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 999px;
  padding: 0;
  cursor: pointer;
  background-image: var(--ac-gradient-primary);
  transition: transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.user-trigger:hover {
  transform: translateY(-1px);
}

.user-trigger:active {
  transform: scale(0.95);
}

.user-avatar {
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.menu-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 200px;
  padding: 8px;
  border-radius: 16px;
  z-index: 300;
}

.menu-head {
  padding: 8px 10px 10px;
  border-bottom: 1px solid var(--ac-border);
  margin-bottom: 6px;
}

.menu-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ac-text-1);
}

.menu-sub {
  font-size: 11.5px;
  color: var(--ac-text-3);
  margin-top: 2px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 9px 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--ac-text-2);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.menu-item:hover {
  background: color-mix(in srgb, var(--ac-primary) 9%, transparent);
  color: var(--ac-text-1);
}

.menu-item.danger {
  color: var(--ac-coral);
}

.menu-item.danger:hover {
  background: var(--ac-danger-bg);
}

.menu-divider {
  height: 1px;
  margin: 6px 4px;
  background: var(--ac-border);
}

.crumb-enter-active {
  transition: opacity 0.24s ease, transform 0.24s cubic-bezier(0.2, 0, 0, 1);
}

.crumb-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.crumb-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.crumb-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

.menu-enter-active {
  transition: opacity 0.2s ease;
}

.menu-enter-active .menu-panel {
  transition: transform 0.28s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.2s ease;
}

.menu-leave-active {
  transition: opacity 0.14s ease;
}

.menu-enter-from,
.menu-leave-to {
  opacity: 0;
}

.menu-enter-from .menu-panel {
  transform: translateY(-6px) scale(0.96);
  opacity: 0;
}

@media (max-width: 768px) {
  .search-trigger {
    display: none;
  }

  .topbar-date {
    display: none;
  }
}
</style>
