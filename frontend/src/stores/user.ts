import { defineStore } from 'pinia'
import { authApi, type AuthTokens } from '@/api/auth'
import { tokenStore } from '@/api/request'
import type { UserInfo } from '@/types'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null as UserInfo | null,
    meLoaded: false,
  }),
  getters: {
    isLoggedIn: (): boolean => Boolean(tokenStore.getAccess() || tokenStore.getRefresh()),
    displayName: (state): string => state.user?.nickname || state.user?.username || '',
  },
  actions: {
    applyTokens(tokens: AuthTokens): void {
      tokenStore.set(tokens.accessToken, tokens.refreshToken)
      this.user = tokens.user
      this.meLoaded = true
    },
    async login(username: string, password: string): Promise<void> {
      this.applyTokens(await authApi.login({ username, password }))
    },
    async register(username: string, password: string, nickname: string): Promise<void> {
      this.applyTokens(await authApi.register({ username, password, nickname }))
    },
    async fetchMe(): Promise<void> {
      this.user = await authApi.me()
      this.meLoaded = true
    },
    async logout(): Promise<void> {
      try {
        await authApi.logout(tokenStore.getRefresh())
      } catch {
        // ignore logout failures
      }
      tokenStore.clear()
      this.user = null
      this.meLoaded = false
    },
  },
})
