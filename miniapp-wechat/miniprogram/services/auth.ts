import { AuthProfile, MiniAppLoginResponse, RegisterRequest, RegistrationRole, WechatBindingCode, WechatBindingPreview, WechatBindingStatus } from '../types/api'
import { isMockMode } from './config'
import { request } from './http'
import { mockAccountLogin, mockBindByCode, mockBindingCode, mockBindingPreview, mockBindingStatus, mockLogin, mockProfile, mockRegisterAccount, mockRegistrationRoles } from './mock'
import { clearSession } from './session'

export async function loginWithWechat(): Promise<MiniAppLoginResponse> {
  if (isMockMode()) {
    const result = await mockLogin()
    saveSession(result)
    return result
  }
  const code = await new Promise<string>((resolve, reject) => wx.login({ success: result => resolve(result.code), fail: reject }))
  const result = await request<MiniAppLoginResponse>({ url: '/api/system/mini_app/auth/login', method: 'POST', data: { code } })
  saveSession(result)
  return result
}

function saveSession(result: MiniAppLoginResponse): void {
  if (result.token) wx.setStorageSync('mes_token', result.token)
  if (result.userId || result.userNo) wx.setStorageSync('mes_user', result)
}

export async function loginWithAccount(userNo: string, password: string): Promise<MiniAppLoginResponse> {
  const loginResult = isMockMode()
    ? await mockAccountLogin(userNo, password)
    : await request<MiniAppLoginResponse>({ url: '/api/system/auth/login', method: 'POST', data: { userNo, password } })
  if (loginResult.token) wx.setStorageSync('mes_token', loginResult.token)
  const profile = isMockMode()
    ? await mockProfile(userNo)
    : await request<AuthProfile>({ url: '/api/system/auth/profile', method: 'GET' })
  const result: MiniAppLoginResponse = { bindingRequired: false, token: loginResult.token, ...profile }
  saveSession(result)
  return result
}

export async function getRegistrationRoles(): Promise<RegistrationRole[]> {
  return isMockMode()
    ? mockRegistrationRoles()
    : request<RegistrationRole[]>({ url: '/api/system/auth/registration_roles', method: 'GET' })
}

export async function registerAccount(data: RegisterRequest): Promise<number> {
  if (isMockMode()) return mockRegisterAccount(data)
  return request<number>({ url: '/api/system/auth/register', method: 'POST', data })
}

export async function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  if (!isMockMode()) await request<void>({ url: '/api/system/auth/password', method: 'PUT', data: { oldPassword, newPassword } })
  clearSession()
}

export async function refreshProfile(): Promise<MiniAppLoginResponse> {
  const stored = (wx.getStorageSync('mes_user') || {}) as MiniAppLoginResponse
  const profile = isMockMode() ? await mockProfile(stored.userNo) : await request<AuthProfile>({ url: '/api/system/auth/profile', method: 'GET' })
  const result: MiniAppLoginResponse = { bindingRequired: false, token: wx.getStorageSync('mes_token') || stored.token, ...profile }
  saveSession(result)
  return result
}

export async function createWechatBindingCode(): Promise<WechatBindingCode> {
  return isMockMode() ? mockBindingCode() : request<WechatBindingCode>({ url: '/api/system/mini_app/auth/binding_codes', method: 'POST' })
}

export async function getWechatBindingPreview(ticket: string): Promise<WechatBindingPreview> {
  return isMockMode() ? mockBindingPreview(ticket) : request<WechatBindingPreview>({ url: `/api/system/mini_app/auth/binding_codes/${encodeURIComponent(ticket)}/preview`, method: 'GET' })
}

export async function getWechatBindingStatus(ticket: string): Promise<WechatBindingStatus> {
  return isMockMode() ? mockBindingStatus(ticket) : request<WechatBindingStatus>({ url: `/api/system/mini_app/auth/binding_codes/${encodeURIComponent(ticket)}/status`, method: 'GET' })
}

export async function bindWechatByCode(ticket: string, code: string): Promise<void> {
  if (isMockMode()) await mockBindByCode(ticket)
  else await request<void>({ url: '/api/system/mini_app/auth/bind_by_code', method: 'POST', data: { ticket, code } })
}

export async function unbindAccount(): Promise<void> {
  if (!isMockMode()) await request<void>({ url: '/api/system/mini_app/auth/unbind', method: 'DELETE' })
  clearSession()
}

export async function ensureLogin(): Promise<void> {
  if (wx.getStorageSync('mes_token')) return
  const result = await loginWithWechat()
  if (result.bindingRequired) {
    wx.showModal({ title: '微信尚未绑定', content: '请先使用账户密码登录，再到“我的-账号与安全”生成微信绑定码。', confirmText: '账户登录', cancelText: '稍后再说', confirmColor: '#0f766e', success: modal => { if (modal.confirm) wx.navigateTo({ url: '/pages/account-login/account-login' }) } })
  }
  else wx.reLaunch({ url: '/pages/dashboard/dashboard' })
}

export async function logout(): Promise<void> {
  if (!isMockMode()) await request<void>({ url: '/api/system/auth/logout', method: 'POST' }).catch(() => undefined)
  clearSession()
  wx.reLaunch({ url: '/pages/login/login' })
}
