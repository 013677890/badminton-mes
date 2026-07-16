import axios, { AxiosError } from 'axios'
import type { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
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

// ---------- 演示模式（不连后端浏览页面） ----------

/** 演示会话的本地 token 标记，由 stores/user.ts 用演示账号登录时写入 */
export const MOCK_TOKEN = 'mock-demo-session'

export function isMockSession(): boolean {
  return localStorage.getItem(TOKEN_STORAGE_KEY) === MOCK_TOKEN
}

/**
 * 演示会话离线适配器：请求不出网。
 * GET 返回空数据（分页给空 PageResult，其余给空数组——数组方法可用、
 * 字段访问得 undefined，列表/详情页均可安全渲染）；写操作返回统一业务错误。
 */
function mockAdapter(config: InternalAxiosRequestConfig): Promise<AxiosResponse> {
  // 演示模式导出返回空文件，避免下载逻辑报错
  if (config.responseType === 'blob') {
    return Promise.resolve({
      data: new Blob([''], { type: 'application/octet-stream' }),
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    })
  }
  const method = (config.method ?? 'get').toLowerCase()
  let body: ApiResult
  if (method === 'get') {
    const params = (config.params ?? {}) as Record<string, unknown>
    const isPage = (config.url ?? '').includes('/page') || params.pageNo !== undefined
    body = {
      code: SUCCESS_CODE,
      message: 'ok',
      userTip: null,
      data: isPage
        ? { list: [], total: 0, pageNo: params.pageNo ?? 1, pageSize: params.pageSize ?? 10 }
        : [],
    }
  } else {
    body = {
      code: 'B0001',
      message: 'demo mode',
      userTip: '演示模式：未连接后端，仅支持浏览页面',
      data: null,
    }
  }
  return Promise.resolve({ data: body, status: 200, statusText: 'OK', headers: {}, config })
}

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
  if (isMockSession()) {
    config.adapter = mockAdapter
    return config
  }
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
    // 文件导出等二进制流：返回完整响应，供 download 读取响应头与文件名
    if (response.config.responseType === 'blob') {
      return response as never
    }
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

/** 从 Content-Disposition 解析下载文件名，解析失败回退 fallback */
function parseFileName(disposition: string | undefined, fallback: string): string {
  if (!disposition) return fallback
  const star = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (star?.[1]) return decodeURIComponent(star[1])
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain?.[1] ?? fallback
}

/**
 * 文件导出下载：以 blob 接收二进制流并触发浏览器下载。
 * 后端导出异常时仍以 JSON CommonResult 返回，此处解析后走统一错误提示。
 */
export async function download(
  url: string,
  params?: object,
  fallbackFileName = 'export',
): Promise<void> {
  const response = await service.get(url, { params, responseType: 'blob' })
  const blob = response.data as Blob
  // 业务异常（鉴权/校验失败）以 JSON 返回，需解析为 ApiError
  if (blob.type.includes('application/json')) {
    const result = JSON.parse(await blob.text()) as ApiResult
    ElMessage.error(result.userTip || result.message || '导出失败')
    throw new ApiError(result)
  }
  const fileName = parseFileName(
    response.headers['content-disposition'] as string | undefined,
    fallbackFileName,
  )
  const href = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = href
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(href)
}

export default service
