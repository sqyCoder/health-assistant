import { useUiStore } from '@/stores/ui'

export function useToast() {
  const ui = useUiStore()

  return {
    success(title: string, message?: string): number {
      return ui.pushToast({ type: 'success', title, message })
    },
    error(title: string, message?: string): number {
      return ui.pushToast({ type: 'error', title, message })
    },
    info(title: string, message?: string): number {
      return ui.pushToast({ type: 'info', title, message })
    },
    loading(title: string, message?: string): number {
      return ui.pushToast({ type: 'loading', title, message })
    },
    async promise<T>(
      task: Promise<T>,
      messages: { loading: string; success: string; error: string },
    ): Promise<T> {
      const id = ui.pushToast({ type: 'loading', title: messages.loading })
      try {
        const result = await task
        ui.updateToast(id, { type: 'success', title: messages.success, duration: 3200 })
        return result
      } catch (error) {
        ui.updateToast(id, {
          type: 'error',
          title: messages.error,
          message: error instanceof Error ? error.message : undefined,
          duration: 4600,
        })
        throw error
      }
    },
    dismiss(id: number): void {
      ui.removeToast(id)
    },
  }
}
