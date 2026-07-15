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
export const USER_STORAGE_KEY = 'mes_user'

/** 登录失效（A0230，HTTP 401）；权限不足（A0301，HTTP 403） */
export const UNAUTHORIZED_CODE = 'A0230'
export const FORBIDDEN_CODE = 'A0301'

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

/** 并发请求同时 401 时只跳一次登录 */
let redirectingToLogin = false

/**
 * 会话失效处理：清本地凭证后整页跳登录。
 * 用 location 而非 router，避免 request → router → store → request 循环依赖，
 * 且整页刷新可一并重置 Pinia 内存态。
 */
function redirectToLogin() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  localStorage.removeItem(USER_STORAGE_KEY)
  const { pathname, search } = window.location
  const redirect = pathname === '/login' ? '' : `?redirect=${encodeURIComponent(pathname + search)}`
  window.location.href = `/login${redirect}`
}

function isApiResult(data: unknown): data is ApiResult {
  return typeof data === 'object' && data !== null && 'code' in data && 'message' in data
}

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
    const body = error.response?.data

    // 后端 GlobalExceptionHandler 对 4xx/5xx 也返回 CommonResult 四要素
    if (isApiResult(body)) {
      if (status === 401 || body.code === UNAUTHORIZED_CODE) {
        ElMessage.error(body.userTip || '登录状态已失效，请重新登录')
        redirectToLogin()
      } else {
        ElMessage.error(body.userTip || body.message || `请求失败（HTTP ${status}）`)
      }
      return Promise.reject(new ApiError(body))
    }

    if (status === 401) {
      ElMessage.error('登录状态已失效，请重新登录')
      redirectToLogin()
    } else {
      ElMessage.error(status ? `请求失败（HTTP ${status}）` : '网络异常，请检查连接')
    }
    return Promise.reject(error)
  },
)

/** params 接受任意对象（接口层的具名参数类型无索引签名，交给 axios 序列化） */
export function get<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as Promise<T>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as Promise<T>
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as Promise<T>
}

export function del<T>(url: string, params?: object): Promise<T> {
  return service.delete(url, { params }) as Promise<T>
}

export default service
