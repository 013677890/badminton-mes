import { CommonResult } from '../types/api'
import { handleSessionExpired } from './session'

export const BASE_URL = 'http://127.0.0.1:8080'

export function request<T>(options: WechatMiniprogram.RequestOption): Promise<T> {
  const token = wx.getStorageSync('mes_token') as string
  return new Promise((resolve, reject) => wx.request({
    ...options,
    url: `${BASE_URL}${options.url}`,
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
