import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { TOKEN_STORAGE_KEY } from '@/utils/request'

const USER_STORAGE_KEY = 'mes_user'

/** 超级管理员角色码：拥有全部权限 */
export const ADMIN_ROLE = 'ADMIN'

export interface LoginPayload {
  userName: string
  roleCodes: string[]
}

interface StoredUser {
  userName: string
  roleCodes: string[]
}

function readStoredUser(): StoredUser | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredUser
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const stored = readStoredUser()
  const token = ref(localStorage.getItem(TOKEN_STORAGE_KEY) ?? '')
  const userName = ref(stored?.userName ?? '')
  const roleCodes = ref<string[]>(stored?.roleCodes ?? [])

  const isLoggedIn = computed(() => token.value !== '')

  /**
   * 演示用本地登录。
   * 接入认证模块（wiki/15）后替换为 POST /api/auth/login，
   * 从响应中取 token 与 roleCodes。
   */
  function login(payload: LoginPayload) {
    token.value = `mock-token-${Date.now()}`
    userName.value = payload.userName
    roleCodes.value = payload.roleCodes
    localStorage.setItem(TOKEN_STORAGE_KEY, token.value)
    localStorage.setItem(
      USER_STORAGE_KEY,
      JSON.stringify({ userName: payload.userName, roleCodes: payload.roleCodes }),
    )
  }

  function logout() {
    token.value = ''
    userName.value = ''
    roleCodes.value = []
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    localStorage.removeItem(USER_STORAGE_KEY)
  }

  return { token, userName, roleCodes, isLoggedIn, login, logout }
})
