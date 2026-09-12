<template>
  <div
    ref="cardRef"
    class="ac-card ac-glass ac-glass-hover"
    :class="{ 'ac-spotlight': spotlight, 'ac-card-tilt': tilt }"
    :style="tiltStyle"
    @mousemove="onTiltMove"
    @mouseleave="resetTilt"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useSpotlight } from '@/composables/useSpotlight'

const props = withDefaults(
  defineProps<{
    spotlight?: boolean
    tilt?: boolean
    padding?: 'none' | 'sm' | 'md' | 'lg'
  }>(),
  {
    spotlight: false,
    tilt: false,
    padding: 'md',
  },
)

const cardRef = ref<HTMLElement>()
if (props.spotlight) {
  useSpotlight(cardRef)
}

const tiltState = ref({ rx: 0, ry: 0 })

const tiltStyle = computed(() => {
  if (!props.tilt) {
    return undefined
  }
  return {
    transform: `perspective(900px) rotateX(${tiltState.value.rx}deg) rotateY(${tiltState.value.ry}deg)`,
  }
})

function onTiltMove(event: MouseEvent): void {
  if (!props.tilt || !cardRef.value || document.body.classList.contains('ac-lite')) {
    return
  }
  const rect = cardRef.value.getBoundingClientRect()
  const px = (event.clientX - rect.left) / rect.width - 0.5
  const py = (event.clientY - rect.top) / rect.height - 0.5
  tiltState.value = { rx: -py * 6, ry: px * 6 }
}

function resetTilt(): void {
  tiltState.value = { rx: 0, ry: 0 }
}
</script>

<style scoped>
.ac-card {
  border-radius: 20px;
  padding: 20px;
  transition: transform 0.32s cubic-bezier(0.2, 0, 0, 1), border-color 0.24s ease, box-shadow 0.24s ease;
}

.ac-card-tilt {
  transition: transform 0.12s ease-out, border-color 0.24s ease, box-shadow 0.24s ease;
  will-change: transform;
}
</style>
