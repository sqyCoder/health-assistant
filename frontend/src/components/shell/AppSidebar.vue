<template>
  <aside class="sidebar ac-glass" :class="{ collapsed }" data-tour="sidebar">
    <div class="sidebar-logo" @click="router.push('/dashboard')">
      <span class="logo-mark">
        <HeartPulse :size="18" />
      </span>
      <Transition name="fade">
        <span v-if="!collapsed" class="logo-text">
          <span class="ac-gradient-text">健康助手</span>
        </span>
      </Transition>
    </div>

    <nav ref="navRef" class="sidebar-nav">
      <div class="nav-pill" :class="{ ready: pill.ready }" :style="pillStyle" />
      <template v-for="group in groups" :key="group.title">
        <div v-if="!collapsed" class="nav-group-title">{{ group.title }}</div>
        <div v-else class="nav-group-gap" />
        <button
          v-for="item in group.items"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :data-path="item.path"
          :title="collapsed ? item.label : undefined"
          @click="router.push(item.path)"
        >
          <span class="nav-icon">
            <component :is="item.icon" :size="18" />
          </span>
          <Transition name="fade">
            <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
          </Transition>
        </button>
      </template>
    </nav>

    <div class="sidebar-footer">
      <button type="button" class="nav-item" title="主题切换" @click="onToggleTheme">
        <span class="nav-icon">
          <Sun v-if="themeStore.dark" :size="18" />
          <Moon v-else :size="18" />
        </span>
        <Transition name="fade">
          <span v-if="!collapsed" class="nav-label">{{ themeStore.dark ? '浅色模式' : '深色模式' }}</span>
        </Transition>
      </button>
      <button type="button" class="nav-item" title="展开/收起" @click="toggleCollapse">
        <span class="nav-icon">
          <ChevronsLeft v-if="!collapsed" :size="18" />
          <ChevronsRight v-else :size="18" />
        </span>
        <Transition name="fade">
          <span v-if="!collapsed" class="nav-label">收起菜单</span>
        </Transition>
      </button>
      <div v-if="!collapsed" class="sidebar-version">v1.0.0</div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Activity,
  BarChart3,
  ChevronsLeft,
  ChevronsRight,
  FileText,
  HeartPulse,
  Home,
  MessageCircle,
  Moon,
  ShieldCheck,
  Sparkles,
  Sun,
  Users,
} from 'lucide-vue-next'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const userStore = useUserStore()

const collapsed = ref(localStorage.getItem('ha_sidebar_collapsed') === '1')
const navRef = ref<HTMLElement>()
const pill = reactive({ top: 8, height: 44, ready: false })

const groups = computed(() => {
  const health = [
    { path: '/dashboard', label: '首页', icon: Home },
    { path: '/members', label: '家庭成员', icon: Users },
    { path: '/reports', label: '体检报告', icon: FileText },
    { path: '/exercise', label: '运动记录', icon: Activity },
    { path: '/charts', label: '数据分析', icon: BarChart3 },
  ]
  const smart = [
    { path: '/suggestions', label: '健康建议', icon: Sparkles },
    { path: '/chat', label: '健康问答', icon: MessageCircle },
  ]
  if (userStore.user?.role === 'ADMIN') {
    smart.push({ path: '/admin', label: '管理后台', icon: ShieldCheck })
  }
  return [
    { title: '健康数据', items: health },
    { title: '智能服务', items: smart },
  ]
})

const pillStyle = computed(() => ({
  transform: `translateY(${pill.top}px)`,
  height: `${pill.height}px`,
}))

function isActive(path: string): boolean {
  if (path === '/dashboard') {
    return route.path === '/dashboard' || route.path === '/'
  }
  return route.path.startsWith(path)
}

function updatePill(): void {
  const nav = navRef.value
  if (!nav) {
    return
  }
  const active = nav.querySelector<HTMLElement>('.nav-item.active')
  if (!active) {
    pill.ready = false
    return
  }
  pill.top = active.offsetTop
  pill.height = active.offsetHeight
  pill.ready = true
}

function toggleCollapse(): void {
  collapsed.value = !collapsed.value
  localStorage.setItem('ha_sidebar_collapsed', collapsed.value ? '1' : '0')
}

function onToggleTheme(event: MouseEvent): void {
  themeStore.toggle({ x: event.clientX, y: event.clientY })
}

watch(
  () => route.path,
  async () => {
    await nextTick()
    updatePill()
  },
)

watch(collapsed, async () => {
  await nextTick()
  updatePill()
})

onMounted(async () => {
  if (window.innerWidth < 1280) {
    collapsed.value = true
  }
  await nextTick()
  updatePill()
})
</script>

<style scoped>
.sidebar {
  position: sticky;
  top: 16px;
  align-self: flex-start;
  height: calc(100vh - 32px);
  width: 236px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  padding: 14px 10px 12px;
  transition: width 0.32s cubic-bezier(0.2, 0, 0, 1);
  overflow: hidden;
}

.sidebar.collapsed {
  width: 76px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px 14px;
  cursor: pointer;
  user-select: none;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  flex: none;
  color: #fff;
  background-image: var(--ac-gradient-primary);
  box-shadow: 0 8px 20px -10px color-mix(in srgb, var(--ac-primary) 80%, transparent);
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.sidebar-nav {
  position: relative;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-top: 8px;
}

.nav-pill {
  position: absolute;
  left: 4px;
  right: 4px;
  top: 0;
  border-radius: 12px;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--ac-primary) 16%, transparent),
    color-mix(in srgb, var(--ac-violet) 12%, transparent)
  );
  border: 1px solid color-mix(in srgb, var(--ac-primary) 22%, transparent);
  transition: transform 0.36s cubic-bezier(0.2, 0, 0, 1), height 0.24s ease, opacity 0.24s ease;
  opacity: 0;
  pointer-events: none;
}

.nav-pill.ready {
  opacity: 1;
}

.nav-group-title {
  padding: 12px 10px 6px;
  font-size: 10.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--ac-text-3);
  white-space: nowrap;
}

.nav-group-gap {
  height: 26px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 44px;
  padding: 0 12px;
  margin-bottom: 8px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--ac-text-2);
  font-size: 13.5px;
  cursor: pointer;
  transition: color 0.2s ease, transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1);
  white-space: nowrap;
  text-align: left;
}

.nav-item:hover {
  color: var(--ac-text-1);
}

.nav-item:active {
  transform: scale(0.98);
}

.nav-item.active {
  color: var(--ac-text-1);
  font-weight: 600;
}

.nav-icon {
  display: grid;
  place-items: center;
  width: 22px;
  flex: none;
}

.sidebar-footer {
  border-top: 1px solid var(--ac-border);
  padding-top: 8px;
}

.sidebar-version {
  padding: 4px 12px 0;
  font-size: 10.5px;
  color: var(--ac-text-3);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 0;
}

@media (max-width: 900px) {
  .sidebar {
    display: none;
  }
}
</style>
