import { del, get, post, put } from './request'
import type { ExerciseDaily, PageData, Workout, WorkoutSummary } from '@/types'

export const exerciseApi = {
  getDaily(memberId: string, date: string): Promise<ExerciseDaily | null> {
    return get<ExerciseDaily | null>(`/members/${memberId}/exercise/daily`, { date })
  },
  saveDaily(memberId: string, payload: ExerciseDaily): Promise<ExerciseDaily> {
    return put<ExerciseDaily>(`/members/${memberId}/exercise/daily`, payload)
  },
  listDaily(memberId: string, from?: string, to?: string): Promise<ExerciseDaily[]> {
    return get<ExerciseDaily[]>(`/members/${memberId}/exercise/daily/range`, { from, to })
  },
  removeDaily(memberId: string, id: string): Promise<void> {
    return del<void>(`/members/${memberId}/exercise/daily/${id}`)
  },
  listWorkouts(
    memberId: string,
    params: { workoutType?: string; from?: string; to?: string; pageNum?: number; pageSize?: number },
  ): Promise<PageData<Workout>> {
    return get<PageData<Workout>>(`/members/${memberId}/exercise/workouts`, params)
  },
  addWorkout(memberId: string, payload: Partial<Workout>): Promise<Workout> {
    return post<Workout>(`/members/${memberId}/exercise/workouts`, payload)
  },
  updateWorkout(memberId: string, id: string, payload: Partial<Workout>): Promise<Workout> {
    return put<Workout>(`/members/${memberId}/exercise/workouts/${id}`, payload)
  },
  removeWorkout(memberId: string, id: string): Promise<void> {
    return del<void>(`/members/${memberId}/exercise/workouts/${id}`)
  },
  summary(memberId: string, from?: string, to?: string): Promise<WorkoutSummary> {
    return get<WorkoutSummary>(`/members/${memberId}/exercise/workouts/summary`, { from, to })
  },
}
