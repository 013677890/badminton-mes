import { CommonResult } from '../types/api'

// 本地 Spring Boot 后端：server.port 默认 8080，docker-compose 同样映射到宿主机 8080。
const BASE_URL = 'http://127.0.0.1:8080'

/**
 * 封装小程序 HTTP 请求，统一补充鉴权信息并处理服务端响应结构。
 *
 * @param options 请求地址、方法、请求体和请求头
 * @return 解包后的业务数据
 */
export function request<T>(options: WechatMiniprogram.RequestOption): Promise<T> {
  // 每次请求读取最新 token，避免用户登录/退出后复用旧闭包中的鉴权信息。
  const token = wx.getStorageSync('mes_token') as string
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      url: `${BASE_URL}${options.url}`,
      // 默认 JSON 请求头与调用方自定义请求头合并，token 存在时追加 Bearer 鉴权。
      header: { 'Content-Type': 'application/json', ...(options.header || {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      success: response => {
        // 小程序端统一把 HTTP 状态和后端 CommonResult 业务码转换成 Promise 成功/失败。
        const body = response.data as CommonResult<T>
        if (response.statusCode === 401) { wx.removeStorageSync('mes_token'); wx.reLaunch({ url: '/pages/login/login' }); reject(new Error('登录已失效')); return }
        if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== '00000') { reject(new Error(body.userTip || body.message || '请求失败')); return }
        resolve(body.data)
      },
      fail: reject
    })
  })
}
