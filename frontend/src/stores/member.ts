import { defineStore } from 'pinia'
import { memberApi } from '@/api/member'
import type { Member } from '@/types'

const CURRENT_MEMBER_KEY = 'ha_current_member'

export const useMemberStore = defineStore('member', {
  state: () => ({
    members: [] as Member[],
    currentMemberId: localStorage.getItem(CURRENT_MEMBER_KEY),
    loaded: false,
  }),
  getters: {
    currentMember(state): Member | null {
      return state.members.find((item) => item.id === state.currentMemberId) ?? null
    },
  },
  actions: {
    async loadMembers(): Promise<void> {
      this.members = await memberApi.list()
      if (!this.currentMemberId || !this.members.some((item) => item.id === this.currentMemberId)) {
        this.setCurrent(this.members[0]?.id ?? null)
      }
      this.loaded = true
    },
    setCurrent(id: string | null): void {
      this.currentMemberId = id
      if (id) {
        localStorage.setItem(CURRENT_MEMBER_KEY, id)
      } else {
        localStorage.removeItem(CURRENT_MEMBER_KEY)
      }
    },
  },
})
