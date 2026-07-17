import { CommonResult } from '../types/api'
import { getApiBaseUrl } from './config'
import { handleSessionExpired } from './session'

/**
 * 封装小程序 HTTP 请求，统一补充鉴权信息并处理服务端响应结构。
 *
 * @param options 请求地址、方法、请求体和请求头
 * @return 解包后的业务数据
 */
export function request<T>(options: WechatMiniprogram.RequestOption): Promise<T> {
  // 每次请求读取最新 token，避免用户登录/退出后复用旧闭包中的鉴权信息。
  const token = wx.getStorageSync('mes_token') as string
  return new Promise((resolve, reject) => wx.request({
    ...options,
    // 运行时读取真实或模拟环境对应的服务地址，避免把开发地址固化到请求层。
    url: `${getApiBaseUrl()}${options.url}`,
    // 默认 JSON 请求头与调用方自定义请求头合并，token 存在时追加 Bearer 鉴权。
    header: { 'Content-Type': 'application/json', ...(options.header || {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    success: response => {
      // 同时检查 HTTP 状态与 CommonResult 业务码，并将后端提示转换为 Promise 异常。
      const body = response.data as CommonResult<T>
      if (response.statusCode === 401) { handleSessionExpired(); reject(new Error('登录已失效，请重新登录')); return }
      if (response.statusCode < 200 || response.statusCode >= 300 || !body || body.code !== '00000') { reject(new Error(body?.userTip || body?.message || '请求失败')); return }
      resolve(body.data)
    },
    fail: error => reject(new Error(error.errMsg || '网络连接失败'))
  }))
}
