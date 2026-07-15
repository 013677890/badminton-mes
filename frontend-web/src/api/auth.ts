import { get, post, put } from '@/utils/request'

/**
 * 认证接口，对齐后端 AuthController（/api/system/auth）。
 * 登录后 token 以 Authorization: Bearer 传递（request.ts 拦截器统一附加）。
 */

export interface LoginParams {
  userNo: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  userNo: string
  userName: string
  roleCodes: string[]
}

export interface AuthProfile {
  userId: number
  userNo: string
  userName: string
  /** 后端已脱敏 */
  mobile: string | null
  workshopId: number | null
  lineId: number | null
  roleCodes: string[]
  roleNames: string[]
}

export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
}

export function login(params: LoginParams): Promise<LoginResult> {
  return post('/system/auth/login', params)
}

/** 幂等；服务端删除会话 */
export function logout(): Promise<void> {
  return post('/system/auth/logout')
}

export function getProfile(): Promise<AuthProfile> {
  return get('/system/auth/profile')
}

/** 成功后当前会话失效，需重新登录 */
export function changePassword(params: ChangePasswordParams): Promise<void> {
  return put('/system/auth/password', params)
}
