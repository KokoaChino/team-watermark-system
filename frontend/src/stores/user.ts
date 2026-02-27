import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserVO, TeamMemberVO } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserVO | null>(null)
  const teamInfo = ref<TeamMemberVO | null>(null)

  function setToken(newToken: string) {
    token.value = newToken
    if (userInfo.value) {
      userInfo.value.token = newToken
    }
  }

  function setUserInfo(info: UserVO) {
    userInfo.value = info
    if (info.token) {
      token.value = info.token
    }
  }

  function setTeamInfo(info: TeamMemberVO) {
    teamInfo.value = info
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    teamInfo.value = null
  }

  return {
    token,
    userInfo,
    teamInfo,
    setToken,
    setUserInfo,
    setTeamInfo,
    logout
  }
}, {
  persist: true
})
