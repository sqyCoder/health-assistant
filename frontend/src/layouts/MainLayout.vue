<template>
  <div class="shell">
    <AppSidebar />
    <div class="shell-main">
      <AppTopbar @profile="profileVisible = true" @password="passwordVisible = true" @logout="onLogout" />
      <main class="shell-content">
        <RouterView v-slot="{ Component, route }">
          <Transition name="ac-page" mode="out-in">
            <component :is="Component" :key="route.path" />
          </Transition>
        </RouterView>
      </main>
    </div>
    <MobileTabbar />
    <ProfileDialogs v-model:profile-visible="profileVisible" v-model:password-visible="passwordVisible" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/shell/AppSidebar.vue'
import AppTopbar from '@/components/shell/AppTopbar.vue'
import MobileTabbar from '@/components/shell/MobileTabbar.vue'
import ProfileDialogs from '@/components/ProfileDialogs.vue'
import { useUserStore } from '@/stores/user'
import { useMemberStore } from '@/stores/member'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const userStore = useUserStore()
const memberStore = useMemberStore()
const ui = useUiStore()
const profileVisible = ref(false)
const passwordVisible = ref(false)

onMounted(async () => {
  if (!userStore.meLoaded) {
    try {
      await userStore.fetchMe()
    } catch {
      // token invalid, interceptor will redirect
    }
  }
  if (!memberStore.loaded) {
    try {
      await memberStore.loadMembers()
    } catch {
      // ignore
    }
  }
})

async function onLogout(): Promise<void> {
  const confirmed = await ui.openConfirm({
    title: '退出登录？',
    content: '退出后需要重新登录。',
    danger: true,
    confirmText: '退出',
  })
  if (!confirmed) {
    return
  }
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.shell {
  display: flex;
  gap: 20px;
  max-width: 1480px;
  margin: 0 auto;
  padding: 16px 20px 28px;
  min-height: 100vh;
}

.shell-main {
  flex: 1;
  min-width: 0;
}

.shell-content {
  padding-top: 14px;
}

@media (max-width: 900px) {
  .shell {
    padding: 10px 12px calc(86px + env(safe-area-inset-bottom));
    gap: 0;
  }
}
</style>
