import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserVO, TeamMemberVO } from '@/types'
import { logout as logoutApi } from '@/api/auth'

export const DEFAULT_SIDEBAR_ACTIVE_PATH = '/'
export const DEFAULT_SIDEBAR_OPENED_MENUS = ['/team', '/template', '/task', '/logs']
export const SIDEBAR_MENU_ROUTE_PATHS = [
  '/',
  '/team',
  '/team/invite',
  '/template',
  '/template/draft',
  '/font',
  '/task/create',
  '/task/execution',
  '/logs/team',
  '/logs/watermark',
  '/logs/points',
  '/logs/tasks'
]

interface SidebarState {
  activePath: string
  openedMenus: string[]
}

const SIDEBAR_OPENED_MENU_SET = new Set(DEFAULT_SIDEBAR_OPENED_MENUS)

function normalizeSidebarState(state?: Partial<SidebarState>): SidebarState {
  const rawActivePath = state?.activePath
  const activePath = rawActivePath && SIDEBAR_MENU_ROUTE_PATHS.includes(rawActivePath)
    ? rawActivePath
    : DEFAULT_SIDEBAR_ACTIVE_PATH

  const rawOpenedMenus = Array.isArray(state?.openedMenus) ? state!.openedMenus : []
  const openedMenus = Array.from(
    new Set(rawOpenedMenus.filter((menu) => SIDEBAR_OPENED_MENU_SET.has(menu)))
  )

  return {
    activePath,
    openedMenus: openedMenus.length > 0 ? openedMenus : [...DEFAULT_SIDEBAR_OPENED_MENUS]
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserVO | null>(null)
  const teamInfo = ref<TeamMemberVO | null>(null)
  const sidebarState = ref<SidebarState>(normalizeSidebarState())

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

  function setSidebarState(state: Partial<SidebarState>) {
    sidebarState.value = normalizeSidebarState({
      ...sidebarState.value,
      ...state
    })
  }

  function getSidebarState() {
    return normalizeSidebarState(sidebarState.value)
  }

  async function logout(skipRequest = false) {
    if (!skipRequest) {
      try {
        await logoutApi()
      } catch (error) {
        console.error('登出请求失败:', error)
      }
    }
    token.value = ''
    userInfo.value = null
    teamInfo.value = null
  }

  return {
    token,
    userInfo,
    teamInfo,
    sidebarState,
    setToken,
    setUserInfo,
    setTeamInfo,
    setSidebarState,
    getSidebarState,
    logout
  }
}, {
  persist: true
})
