import { MiniAppLoginResponse } from '../types/api'
import { request } from './http'

/** 使用微信临时登录凭证换取 MES 会话信息，并缓存登录结果。 */
export async function loginWithWechat(): Promise<MiniAppLoginResponse> {
  // wx.login 只产生短期 code，真正的会话创建和用户绑定判断交给 MES 后端。
  const code = await new Promise<string>((resolve, reject) => wx.login({ success: result => resolve(result.code), fail: reject }))
  const result = await request<MiniAppLoginResponse>({ url: '/api/system/mini_app/auth/login', method: 'POST', data: { code } })
  if (result.token) wx.setStorageSync('mes_token', result.token)
  if (result.userId) wx.setStorageSync('mes_user', result)
  return result
}

/** 使用绑定票据和用户凭证完成微信账号与 MES 账号绑定。 */
export async function bindAccount(bindTicket: string, userNo: string, password: string): Promise<MiniAppLoginResponse> {
  // 绑定票据限定本次微信登录上下文，账号密码仅通过 HTTPS 请求交给服务端校验。
  const result = await request<MiniAppLoginResponse>({ url: '/api/system/mini_app/auth/bind', method: 'POST', data: { bindTicket, userNo, password } })
  if (result.token) { wx.setStorageSync('mes_token', result.token); wx.setStorageSync('mes_user', result) }
  return result
}

/** 检查本地会话；无有效会话时发起登录并按绑定状态跳转页面。 */
export async function ensureLogin(): Promise<void> {
  // 已有本地 token 时不重复登录；首次登录根据后端返回的 bindingRequired 决定落地页面。
  if (wx.getStorageSync('mes_token')) return
  const result = await loginWithWechat()
  if (result.bindingRequired) wx.reLaunch({ url: `/pages/bind-account/bind-account?ticket=${result.bindTicket || ''}` })
  else wx.reLaunch({ url: '/pages/dashboard/dashboard' })
}

/** 通知后端注销会话，并清理小程序本地登录状态。 */
export function logout(): Promise<void> {
  // 注销采用“尽力通知后端 + 无论结果清本地”的策略，保证用户能够离开失效会话。
  return request<void>({ url: '/api/system/auth/logout', method: 'POST' }).catch(() => undefined).then(() => { wx.clearStorageSync(); wx.reLaunch({ url: '/pages/login/login' }) })
}
