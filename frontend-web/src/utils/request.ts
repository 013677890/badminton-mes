import axios, { AxiosError } from 'axios'
import type { AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 与后端 CommonResult<T> 对齐的统一响应结构。
 * code 为 5 位字符串错误码，成功固定 '00000'。
 */
export interface ApiResult<T = unknown> {
  code: string
  message: string
  userTip: string | null
  data: T
}

/** 与后端 PageResult<T> 对齐 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNo: number
  pageSize: number
}

/** 与后端 PageParam 对齐 */
export interface PageParam {
  pageNo: number
  pageSize: number
}

export const SUCCESS_CODE = '00000'
export const TOKEN_STORAGE_KEY = 'mes_token'

/** 业务错误：携带后端错误码，便于调用方按码分支处理 */
export class ApiError extends Error {
  readonly code: string
  readonly userTip: string | null

  constructor(result: ApiResult) {
    super(result.message)
    this.name = 'ApiError'
    this.code = result.code
    this.userTip = result.userTip
  }
}

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult
    if (body.code !== SUCCESS_CODE) {
      ElMessage.error(body.userTip || body.message || '请求失败')
      return Promise.reject(new ApiError(body))
    }
    // 拦截器直接解包 data，调用方拿到的即业务数据
    return body.data as never
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const tip = status ? `请求失败（HTTP ${status}）` : '网络异常，请检查连接'
    ElMessage.error(tip)
    return Promise.reject(error)
  },
)

export function get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as Promise<T>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as Promise<T>
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as Promise<T>
}

export function del<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return service.delete(url, { params }) as Promise<T>
}

export default service
