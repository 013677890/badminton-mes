import { CommonResult } from '../types/api'
import { getApiBaseUrl } from './config'
import { handleSessionExpired } from './session'

function sanitizeRequestData(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(sanitizeRequestData)
  if (Object.prototype.toString.call(value) !== '[object Object]') return value

  const result: Record<string, unknown> = {}
  Object.entries(value as Record<string, unknown>).forEach(([key, item]) => {
    if (item !== undefined) result[key] = sanitizeRequestData(item)
  })
  return result
}

export function request<T>(options: WechatMiniprogram.RequestOption): Promise<T> {
  const token = wx.getStorageSync('mes_token') as string
  return new Promise((resolve, reject) => wx.request({
    ...options,
    url: `${getApiBaseUrl()}${options.url}`,
    data: sanitizeRequestData(options.data) as WechatMiniprogram.RequestOption['data'],
    header: { 'Content-Type': 'application/json', ...(options.header || {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    success: response => {
      const body = response.data as CommonResult<T>
      if (response.statusCode === 401) { handleSessionExpired(); reject(new Error('登录已失效，请重新登录')); return }
      if (response.statusCode < 200 || response.statusCode >= 300 || !body || body.code !== '00000') { reject(new Error(body?.userTip || body?.message || '请求失败')); return }
      resolve(body.data)
    },
    fail: error => reject(new Error(error.errMsg || '网络连接失败'))
  }))
}
