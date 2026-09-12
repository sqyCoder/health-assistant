import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark' | 'system'

const THEME_KEY = 'ha_theme_mode'
const LEGACY_KEY = 'ha_theme'

function systemPrefersDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function resolveDark(mode: ThemeMode): boolean {
  if (mode === 'system') {
    return systemPrefersDark()
  }
  return mode === 'dark'
}

export const useThemeStore = defineStore('theme', {
  state: () => {
    const stored = (localStorage.getItem(THEME_KEY) as ThemeMode | null)
      ?? (localStorage.getItem(LEGACY_KEY) === 'dark' ? 'dark' : localStorage.getItem(LEGACY_KEY) === 'light' ? 'light' : 'system')
    return {
      mode: stored as ThemeMode,
      dark: resolveDark(stored as ThemeMode),
    }
  },
  actions: {
    apply(): void {
      this.dark = resolveDark(this.mode)
      document.documentElement.classList.toggle('dark', this.dark)
      localStorage.setItem(THEME_KEY, this.mode)
      localStorage.removeItem(LEGACY_KEY)
    },
    setMode(mode: ThemeMode, origin?: { x: number; y: number }): void {
      this.mode = mode
      if (origin && this.supportsViewTransition()) {
        this.applyWithReveal(origin)
      } else {
        this.apply()
      }
    },
    toggle(origin?: { x: number; y: number }): void {
      this.setMode(this.dark ? 'light' : 'dark', origin)
    },
    followSystem(): void {
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (this.mode === 'system') {
          this.apply()
        }
      })
    },
    supportsViewTransition(): boolean {
      return typeof document.startViewTransition === 'function'
        && !window.matchMedia('(prefers-reduced-motion: reduce)').matches
        && !document.body.classList.contains('ac-lite')
    },
    applyWithReveal(origin: { x: number; y: number }): void {
      const radius = Math.hypot(
        Math.max(origin.x, window.innerWidth - origin.x),
        Math.max(origin.y, window.innerHeight - origin.y),
      )
      document.documentElement.classList.add('ac-theme-transition')
      const transition = document.startViewTransition(() => {
        this.dark = resolveDark(this.mode)
        document.documentElement.classList.toggle('dark', this.dark)
        localStorage.setItem(THEME_KEY, this.mode)
      })
      transition.ready
        .then(() => {
          document.documentElement.animate(
            {
              clipPath: [
                `circle(0px at ${origin.x}px ${origin.y}px)`,
                `circle(${radius}px at ${origin.x}px ${origin.y}px)`,
              ],
            },
            {
              duration: 480,
              easing: 'ease-out',
              pseudoElement: '::view-transition-new(root)',
            },
          )
        })
        .finally(() => {
          setTimeout(() => document.documentElement.classList.remove('ac-theme-transition'), 520)
        })
    },
  },
})
