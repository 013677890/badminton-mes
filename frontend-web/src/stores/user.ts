import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { ChangePasswordParams, LoginParams } from '@/api/auth'
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '@/utils/request'

/** 超级管理员角色码：拥有全部权限 */
export const ADMIN_ROLE = 'ADMIN'

interface StoredUser {
  userId: number | null
  userNo: string
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
  const userId = ref<number | null>(stored?.userId ?? null)
  const userNo = ref(stored?.userNo ?? '')
  const userName = ref(stored?.userName ?? '')
  const roleCodes = ref<string[]>(stored?.roleCodes ?? [])

  const isLoggedIn = computed(() => token.value !== '')

  function persist() {
    localStorage.setItem(TOKEN_STORAGE_KEY, token.value)
    localStorage.setItem(
      USER_STORAGE_KEY,
      JSON.stringify({
        userId: userId.value,
        userNo: userNo.value,
        userName: userName.value,
        roleCodes: roleCodes.value,
      } satisfies StoredUser),
    )
  }

  function clear() {
    token.value = ''
    userId.value = null
    userNo.value = ''
    userName.value = ''
    roleCodes.value = []
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    localStorage.removeItem(USER_STORAGE_KEY)
  }

  /** 工号密码登录（POST /api/system/auth/login），token 与角色由后端下发 */
  async function login(params: LoginParams) {
    const result = await authApi.login(params)
    token.value = result.token
    userId.value = result.userId
    userNo.value = result.userNo
    userName.value = result.userName
    roleCodes.value = result.roleCodes
    persist()
  }

  /** 服务端删会话失败也照常清本地（登出必须总能完成） */
  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 网络异常/会话已失效均忽略
    } finally {
      clear()
    }
  }

  /** 改密成功后后端使当前会话失效，调用方应引导重新登录 */
  async function changePassword(params: ChangePasswordParams) {
    await authApi.changePassword(params)
    clear()
  }

  /** 会话有效时拉取最新档案（角色变更后刷新用） */
  async function fetchProfile() {
    const profile = await authApi.getProfile()
    userId.value = profile.userId
    userNo.value = profile.userNo
    userName.value = profile.userName
    roleCodes.value = profile.roleCodes
    persist()
    return profile
  }

  return {
    token,
    userId,
    userNo,
    userName,
    roleCodes,
    isLoggedIn,
    login,
    logout,
    changePassword,
    fetchProfile,
  }
})
