import { onBeforeUnmount, onMounted, type Ref } from 'vue'

export function useSpotlight(target: Ref<HTMLElement | undefined>) {
  function onMove(event: MouseEvent): void {
    const el = target.value
    if (!el) {
      return
    }
    const rect = el.getBoundingClientRect()
    el.style.setProperty('--mx', `${event.clientX - rect.left}px`)
    el.style.setProperty('--my', `${event.clientY - rect.top}px`)
  }

  onMounted(() => {
    target.value?.addEventListener('mousemove', onMove)
  })

  onBeforeUnmount(() => {
    target.value?.removeEventListener('mousemove', onMove)
  })
}
