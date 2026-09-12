import { get, post, put } from './request'
import type { UserInfo } from '@/types'

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
  user: UserInfo
}

export const authApi = {
  register(data: { username: string; password: string; nickname: string }): Promise<AuthTokens> {
    return post<AuthTokens>('/auth/register', data)
  },
  login(data: { username: string; password: string }): Promise<AuthTokens> {
    return post<AuthTokens>('/auth/login', data)
  },
  logout(refreshToken: string | null): Promise<void> {
    return post<void>('/auth/logout', { refreshToken })
  },
  me(): Promise<UserInfo> {
    return get<UserInfo>('/auth/me')
  },
  changePassword(data: { oldPassword: string; newPassword: string }): Promise<void> {
    return put<void>('/auth/password', data)
  },
  updateProfile(data: { nickname: string; avatarUrl?: string }): Promise<UserInfo> {
    return put<UserInfo>('/auth/profile', data)
  },
}
