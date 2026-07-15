import { MiniAppLoginResponse } from '../types/api'
import { request } from './http'

export async function loginWithWechat(): Promise<MiniAppLoginResponse> {
  const code = await new Promise<string>((resolve, reject) => wx.login({ success: result => resolve(result.code), fail: reject }))
  const result = await request<MiniAppLoginResponse>({ url: '/api/system/mini_app/auth/login', method: 'POST', data: { code } })
  if (result.token) wx.setStorageSync('mes_token', result.token)
  if (result.userId) wx.setStorageSync('mes_user', result)
  return result
}

export async function bindAccount(bindTicket: string, userNo: string, password: string): Promise<MiniAppLoginResponse> {
  const result = await request<MiniAppLoginResponse>({ url: '/api/system/mini_app/auth/bind', method: 'POST', data: { bindTicket, userNo, password } })
  if (result.token) { wx.setStorageSync('mes_token', result.token); wx.setStorageSync('mes_user', result) }
  return result
}

export async function ensureLogin(): Promise<void> {
  if (wx.getStorageSync('mes_token')) return
  const result = await loginWithWechat()
  if (result.bindingRequired) wx.reLaunch({ url: `/pages/bind-account/bind-account?ticket=${result.bindTicket || ''}` })
  else wx.reLaunch({ url: '/pages/dashboard/dashboard' })
}

export function logout(): Promise<void> {
  return request<void>({ url: '/api/system/auth/logout', method: 'POST' }).catch(() => undefined).then(() => { wx.clearStorageSync(); wx.reLaunch({ url: '/pages/login/login' }) })
}
