<template>
  <AuroraBackground />
  <TopProgressBar />
  <RouterView v-slot="{ Component, route }">
    <Transition name="ac-page" mode="out-in">
      <component :is="Component" :key="route.matched[0]?.path ?? route.path" />
    </Transition>
  </RouterView>
  <AcToastHost />
  <AcConfirmHost />
  <CommandPalette />
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import AuroraBackground from '@/components/shell/AuroraBackground.vue'
import TopProgressBar from '@/components/shell/TopProgressBar.vue'
import CommandPalette from '@/components/shell/CommandPalette.vue'
import AcToastHost from '@/components/ui/AcToastHost.vue'
import AcConfirmHost from '@/components/ui/AcConfirmHost.vue'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { useReducedMotion } from '@/composables/useReducedMotion'
import { useTour } from '@/composables/useTour'

const themeStore = useThemeStore()
const userStore = useUserStore()
const { startTour } = useTour()

useReducedMotion()

onMounted(() => {
  themeStore.apply()
  themeStore.followSystem()
})

watch(
  () => userStore.user?.id,
  (id) => {
    if (id) {
      setTimeout(() => {
        void startTour()
      }, 1200)
    }
  },
)
</script>
