import { onBeforeUnmount, onMounted, ref } from 'vue'

export function useReducedMotion() {
  const reduced = ref(false)
  let query: MediaQueryList | null = null

  function evaluate(): void {
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const lowEnd = (navigator.hardwareConcurrency ?? 8) <= 4
    reduced.value = prefersReduced
    document.body.classList.toggle('ac-lite', prefersReduced || lowEnd)
  }

  onMounted(() => {
    evaluate()
    query = window.matchMedia('(prefers-reduced-motion: reduce)')
    query.addEventListener('change', evaluate)
  })

  onBeforeUnmount(() => {
    query?.removeEventListener('change', evaluate)
  })

  return { reduced, evaluate }
}
