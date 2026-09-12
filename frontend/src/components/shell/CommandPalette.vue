<template>
  <Teleport to="body">
    <Transition name="palette">
      <div v-if="ui.paletteOpen" class="palette-overlay" @click.self="ui.closePalette()">
        <div class="palette ac-glass" role="dialog" aria-modal="true">
          <div class="palette-input-row">
            <Search :size="16" class="palette-search-icon" />
            <input
              ref="inputRef"
              v-model="keyword"
              class="palette-input"
              placeholder="搜索页面、成员或操作…"
              @keydown.down.prevent="move(1)"
              @keydown.up.prevent="move(-1)"
              @keydown.enter.prevent="executeActive"
              @keydown.esc.prevent="ui.closePalette()"
            />
            <span class="palette-key">ESC</span>
          </div>
          <div class="palette-list ac-scroll">
            <template v-for="group in filteredGroups" :key="group.title">
              <div v-if="group.items.length" class="palette-group">
                <div class="palette-group-title">{{ group.title }}</div>
                <button
                  v-for="item in group.items"
                  :key="item.id"
                  type="button"
                  class="palette-item"
                  :class="{ active: flatIndex(item) === activeIndex }"
                  @mouseenter="activeIndex = flatIndex(item)"
                  @click="run(item)"
                >
                  <span class="palette-item-icon">
                    <component :is="item.icon" :size="15" />
                  </span>
                  <span class="palette-item-label">{{ item.label }}</span>
                  <span v-if="item.hint" class="palette-item-hint">{{ item.hint }}</span>
                </button>
              </div>
            </template>
            <div v-if="flatItems.length === 0" class="palette-empty">没有匹配的结果</div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  Activity,
  BarChart3,
  FileText,
  Home,
  LogOut,
  MessageCircle,
  Moon,
  Search,
  Sparkles,
  Sun,
  User,
  Users,
  Wand2,
} from 'lucide-vue-next'
import { useUiStore } from '@/stores/ui'
import { useThemeStore } from '@/stores/theme'
import { useMemberStore } from '@/stores/member'
import { useUserStore } from '@/stores/user'
import { useTour } from '@/composables/useTour'

interface CommandItem {
  id: string
  label: string
  hint?: string
  icon: Component
  action: () => unknown
}

interface CommandGroup {
  title: string
  items: CommandItem[]
}

const router = useRouter()
const ui = useUiStore()
const themeStore = useThemeStore()
const memberStore = useMemberStore()
const userStore = useUserStore()
const { startTour } = useTour()

const keyword = ref('')
const activeIndex = ref(0)
const inputRef = ref<HTMLInputElement>()

const groups = computed<CommandGroup[]>(() => {
  const pages: CommandItem[] = [
    { id: 'page-home', label: '首页', icon: Home, action: () => router.push('/dashboard') },
    { id: 'page-members', label: '家庭成员', icon: Users, action: () => router.push('/members') },
    { id: 'page-reports', label: '体检报告', icon: FileText, action: () => router.push('/reports') },
    { id: 'page-exercise', label: '运动记录', icon: Activity, action: () => router.push('/exercise') },
    { id: 'page-charts', label: '数据分析', icon: BarChart3, action: () => router.push('/charts') },
    { id: 'page-suggestions', label: '健康建议', icon: Sparkles, action: () => router.push('/suggestions') },
    { id: 'page-chat', label: '健康问答', icon: MessageCircle, action: () => router.push('/chat') },
  ]
  if (userStore.user?.role === 'ADMIN') {
    pages.push({ id: 'page-admin', label: '管理后台', icon: Wand2, action: () => router.push('/admin') })
  }

  const memberItems: CommandItem[] = memberStore.members.map((member) => ({
    id: `member-${member.id}`,
    label: member.name,
    hint: '切换成员',
    icon: User,
    action: () => memberStore.setCurrent(member.id),
  }))

  const actions: CommandItem[] = [
    {
      id: 'action-theme',
      label: themeStore.dark ? '切换到浅色模式' : '切换到深色模式',
      icon: themeStore.dark ? Sun : Moon,
      action: () => themeStore.toggle(),
    },
    {
      id: 'action-tour',
      label: '重看新手引导',
      icon: Sparkles,
      action: () => startTour(true),
    },
    {
      id: 'action-logout',
      label: '退出登录',
      icon: LogOut,
      hint: '危险',
      action: async () => {
        const confirmed = await ui.openConfirm({
          title: '退出登录？',
          content: '退出后需要重新登录。',
          danger: true,
          confirmText: '退出',
        })
        if (confirmed) {
          await userStore.logout()
          router.push('/login')
        }
      },
    },
  ]

  return [
    { title: '页面', items: pages },
    { title: '成员', items: memberItems },
    { title: '操作', items: actions },
  ]
})

const flatItems = computed(() => groups.value.flatMap((group) => group.items))

const filteredGroups = computed<CommandGroup[]>(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) {
    return groups.value
  }
  return groups.value.map((group) => ({
    title: group.title,
    items: group.items.filter((item) => item.label.toLowerCase().includes(kw)),
  }))
})

const filteredFlat = computed(() => filteredGroups.value.flatMap((group) => group.items))

function flatIndex(item: CommandItem): number {
  return filteredFlat.value.indexOf(item)
}

function move(delta: number): void {
  const total = filteredFlat.value.length
  if (total === 0) {
    return
  }
  activeIndex.value = (activeIndex.value + delta + total) % total
}

async function run(item: CommandItem): Promise<void> {
  ui.closePalette()
  await item.action()
}

function executeActive(): void {
  const target = filteredFlat.value[activeIndex.value]
  if (target) {
    void run(target)
  }
}

function onGlobalKeydown(event: KeyboardEvent): void {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    if (ui.paletteOpen) {
      ui.closePalette()
    } else {
      ui.openPalette()
    }
  }
}

watch(keyword, () => {
  activeIndex.value = 0
})

watch(
  () => ui.paletteOpen,
  async (open) => {
    if (open) {
      keyword.value = ''
      activeIndex.value = 0
      await nextTick()
      inputRef.value?.focus()
    }
  },
)

onMounted(() => window.addEventListener('keydown', onGlobalKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onGlobalKeydown))
</script>

<style scoped>
.palette-overlay {
  position: fixed;
  inset: 0;
  z-index: 600;
  display: flex;
  justify-content: center;
  padding-top: 14vh;
  background: color-mix(in srgb, var(--ac-bg-base) 42%, transparent);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.palette {
  width: min(600px, calc(100vw - 32px));
  height: fit-content;
  max-height: 60vh;
  border-radius: 18px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.palette-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--ac-border);
}

.palette-search-icon {
  color: var(--ac-text-3);
  flex: none;
}

.palette-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14.5px;
  color: var(--ac-text-1);
}

.palette-input::placeholder {
  color: var(--ac-text-3);
}

.palette-key {
  padding: 2px 7px;
  font-size: 10.5px;
  border-radius: 6px;
  color: var(--ac-text-3);
  background: color-mix(in srgb, var(--ac-text-1) 7%, transparent);
  border: 1px solid var(--ac-border);
}

.palette-list {
  padding: 8px;
  overflow-y: auto;
}

.palette-group-title {
  padding: 8px 10px 4px;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ac-text-3);
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--ac-text-1);
  font-size: 13.5px;
  cursor: pointer;
  text-align: left;
}

.palette-item.active {
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.palette-item-icon {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  flex: none;
  color: var(--ac-primary);
  background: color-mix(in srgb, var(--ac-primary) 12%, transparent);
}

.palette-item-label {
  flex: 1;
}

.palette-item-hint {
  font-size: 11px;
  color: var(--ac-text-3);
}

.palette-empty {
  padding: 28px;
  text-align: center;
  color: var(--ac-text-3);
  font-size: 13px;
}

.palette-enter-active {
  transition: opacity 0.24s ease;
}

.palette-enter-active .palette {
  transition: transform 0.32s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.24s ease;
}

.palette-leave-active {
  transition: opacity 0.16s ease;
}

.palette-enter-from,
.palette-leave-to {
  opacity: 0;
}

.palette-enter-from .palette {
  transform: translateY(-10px) scale(0.97);
  opacity: 0;
}
</style>
