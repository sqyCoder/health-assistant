<template>
  <nav class="tabbar ac-glass">
    <button
      v-for="item in items"
      :key="item.path"
      type="button"
      class="tab-item"
      :class="{ active: isActive(item.path) }"
      @click="router.push(item.path)"
    >
      <span class="tab-icon">
        <component :is="item.icon" :size="19" />
      </span>
      <span class="tab-label">{{ item.label }}</span>
    </button>
  </nav>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { Activity, FileText, Home, MessageCircle, Users } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const items = [
  { path: '/dashboard', label: '首页', icon: Home },
  { path: '/reports', label: '报告', icon: FileText },
  { path: '/exercise', label: '运动', icon: Activity },
  { path: '/chat', label: '问答', icon: MessageCircle },
  { path: '/members', label: '成员', icon: Users },
]

function isActive(path: string): boolean {
  if (path === '/dashboard') {
    return route.path === '/dashboard' || route.path === '/'
  }
  return route.path.startsWith(path)
}
</script>

<style scoped>
.tabbar {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: calc(10px + env(safe-area-inset-bottom));
  z-index: 250;
  display: none;
  align-items: center;
  justify-content: space-around;
  padding: 6px;
  border-radius: 20px;
}

.tab-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  flex: 1;
  padding: 6px 0;
  border: none;
  border-radius: 14px;
  background: transparent;
  color: var(--ac-text-3);
  cursor: pointer;
  transition: color 0.2s ease, transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.tab-item.active {
  color: var(--ac-primary);
  transform: translateY(-2px);
}

.tab-item.active::before {
  content: "";
  position: absolute;
  top: -6px;
  width: 22px;
  height: 3px;
  border-radius: 999px;
  background-image: var(--ac-gradient-primary);
}

.tab-label {
  font-size: 10.5px;
  font-weight: 500;
}

@media (max-width: 900px) {
  .tabbar {
    display: flex;
  }
}
</style>
