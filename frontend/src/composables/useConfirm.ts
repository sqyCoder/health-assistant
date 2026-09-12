import { useUiStore, type ConfirmOptions } from '@/stores/ui'

export function useConfirm() {
  const ui = useUiStore()

  return {
    confirm(options: ConfirmOptions): Promise<boolean> {
      return ui.openConfirm(options)
    },
    danger(title: string, content?: string): Promise<boolean> {
      return ui.openConfirm({ title, content, danger: true, confirmText: '删除' })
    },
  }
}
