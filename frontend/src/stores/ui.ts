import { defineStore } from 'pinia'

export type ToastType = 'success' | 'error' | 'info' | 'loading'

export interface ToastItem {
  id: number
  type: ToastType
  title: string
  message?: string
  duration: number
}

export interface ConfirmOptions {
  title: string
  content?: string
  danger?: boolean
  confirmText?: string
  cancelText?: string
}

interface ConfirmState extends ConfirmOptions {
  visible: boolean
  resolver?: (value: boolean) => void
}

let toastSeq = 0
const timers = new Map<number, ReturnType<typeof setTimeout>>()

export const useUiStore = defineStore('ui', {
  state: () => ({
    toasts: [] as ToastItem[],
    confirm: {
      visible: false,
      title: '',
      content: '',
      danger: false,
      confirmText: '确定',
      cancelText: '取消',
    } as ConfirmState,
    progress: {
      visible: false,
      value: 0,
    },
    paletteOpen: false,
  }),
  actions: {
    pushToast(input: { type: ToastType; title: string; message?: string; duration?: number }): number {
      const id = ++toastSeq
      const item: ToastItem = {
        id,
        type: input.type,
        title: input.title,
        message: input.message,
        duration: input.duration ?? (input.type === 'error' ? 4600 : 3200),
      }
      this.toasts.push(item)
      if (this.toasts.length > 4) {
        this.removeToast(this.toasts[0].id)
      }
      if (input.type !== 'loading') {
        const timer = setTimeout(() => this.removeToast(id), item.duration)
        timers.set(id, timer)
      }
      return id
    },
    updateToast(id: number, patch: Partial<Omit<ToastItem, 'id'>>): void {
      const target = this.toasts.find((item) => item.id === id)
      if (!target) {
        return
      }
      Object.assign(target, patch)
      const existing = timers.get(id)
      if (existing) {
        clearTimeout(existing)
        timers.delete(id)
      }
      if (target.type !== 'loading') {
        const timer = setTimeout(() => this.removeToast(id), target.duration)
        timers.set(id, timer)
      }
    },
    removeToast(id: number): void {
      const timer = timers.get(id)
      if (timer) {
        clearTimeout(timer)
        timers.delete(id)
      }
      this.toasts = this.toasts.filter((item) => item.id !== id)
    },
    openConfirm(options: ConfirmOptions): Promise<boolean> {
      return new Promise((resolve) => {
        this.confirm = {
          ...options,
          visible: true,
          resolver: resolve,
        }
      })
    },
    resolveConfirm(value: boolean): void {
      this.confirm.resolver?.(value)
      this.confirm.visible = false
      this.confirm.resolver = undefined
    },
    progressStart(): void {
      this.progress.visible = true
      this.progress.value = 8
    },
    progressDone(): void {
      this.progress.value = 100
      setTimeout(() => {
        this.progress.visible = false
        this.progress.value = 0
      }, 320)
    },
    openPalette(): void {
      this.paletteOpen = true
    },
    closePalette(): void {
      this.paletteOpen = false
    },
  },
})
