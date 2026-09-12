import { get } from './request'
import type { TaskInfo } from '@/types'

export const taskApi = {
  get(id: string): Promise<TaskInfo> {
    return get<TaskInfo>(`/tasks/${id}`)
  },
}
