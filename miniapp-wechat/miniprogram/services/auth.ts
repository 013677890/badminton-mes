import { MiniAppLoginResponse } from '../types/api'
import { request } from './http'

/** 使用微信临时登录凭证换取 MES 会话信息，并缓存登录结果。 */
export async function loginWithWechat(): Promise<MiniAppLoginResponse> {
  const code = await new Promise<string>((resolve, reject) => wx.login({ success: result => resolve(result.code), fail: reject }))
  const result = await request<MiniAppLoginResponse>({ url: '/api/system/mini_app/auth/login', method: 'POST', data: { code } })
  if (result.token) wx.setStorageSync('mes_token', result.token)
  if (result.userId) wx.setStorageSync('mes_user', result)
  return result
}

/** 使用绑定票据和用户凭证完成微信账号与 MES 账号绑定。 */
export async function bindAccount(bindTicket: string, userNo: string, password: string): Promise<MiniAppLoginResponse> {
  const result = await request<MiniAppLoginResponse>({ url: '/api/system/mini_app/auth/bind', method: 'POST', data: { bindTicket, userNo, password } })
  if (result.token) { wx.setStorageSync('mes_token', result.token); wx.setStorageSync('mes_user', result) }
  return result
}

/** 检查本地会话；无有效会话时发起登录并按绑定状态跳转页面。 */
export async function ensureLogin(): Promise<void> {
  if (wx.getStorageSync('mes_token')) return
  const result = await loginWithWechat()
  if (result.bindingRequired) wx.reLaunch({ url: `/pages/bind-account/bind-account?ticket=${result.bindTicket || ''}` })
  else wx.reLaunch({ url: '/pages/dashboard/dashboard' })
}

/** 通知后端注销会话，并清理小程序本地登录状态。 */
export function logout(): Promise<void> {
  return request<void>({ url: '/api/system/auth/logout', method: 'POST' }).catch(() => undefined).then(() => { wx.clearStorageSync(); wx.reLaunch({ url: '/pages/login/login' }) })
}
