import { onBeforeUnmount, ref, watch, type Ref } from 'vue'

function easeOutExpo(t: number): number {
  return t >= 1 ? 1 : 1 - Math.pow(2, -10 * t)
}

export function useCountUp(source: Ref<number | null | undefined>, duration = 800) {
  const display = ref(0)
  let raf = 0

  function animateTo(target: number): void {
    cancelAnimationFrame(raf)
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      || document.body.classList.contains('ac-lite')
    if (prefersReduced) {
      display.value = target
      return
    }
    const from = display.value
    const delta = target - from
    if (delta === 0) {
      return
    }
    const start = performance.now()
    const tick = (now: number) => {
      const progress = Math.min((now - start) / duration, 1)
      display.value = from + delta * easeOutExpo(progress)
      if (progress < 1) {
        raf = requestAnimationFrame(tick)
      }
    }
    raf = requestAnimationFrame(tick)
  }

  watch(
    source,
    (value) => {
      if (value === null || value === undefined || Number.isNaN(value)) {
        display.value = 0
        return
      }
      animateTo(Number(value))
    },
    { immediate: true },
  )

  onBeforeUnmount(() => cancelAnimationFrame(raf))

  return { display, animateTo }
}
