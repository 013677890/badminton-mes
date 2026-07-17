export type DataSourceMode = 'mock' | 'api'
export type MockScenario = 'normal' | 'empty' | 'error' | 'unbound' | 'sessionExpired'

const MODE_KEY = 'miniapp_data_source'
const SCENARIO_KEY = 'miniapp_mock_scenario'
const API_BASE_URL_KEY = 'miniapp_api_base_url'

export const DEFAULT_API_BASE_URL = 'http://172.25.96.19:8080'
export const DEVTOOLS_API_BASE_URL = 'http://127.0.0.1:8080'
const OBSOLETE_API_BASE_URLS = new Set([
  'http://172.25.123.116:8080',
  'http://192.168.219.117:8080'
])

function isWechatDevtools(): boolean {
  try {
    if (typeof wx.getDeviceInfo === 'function') return wx.getDeviceInfo().platform === 'devtools'
    return wx.getSystemInfoSync().platform === 'devtools'
  } catch (_) {
    return false
  }
}

export function getDataSourceMode(): DataSourceMode {
  const value = wx.getStorageSync(MODE_KEY) as DataSourceMode
  return value === 'mock' ? 'mock' : 'api'
}

export function getApiBaseUrl(): string {
  const value = String(wx.getStorageSync(API_BASE_URL_KEY) || '').trim().replace(/\/+$/, '')
  if (OBSOLETE_API_BASE_URLS.has(value)) {
    wx.removeStorageSync(API_BASE_URL_KEY)
    return isWechatDevtools() ? DEVTOOLS_API_BASE_URL : DEFAULT_API_BASE_URL
  }
  if (/^https?:\/\//.test(value)) return value
  return isWechatDevtools() ? DEVTOOLS_API_BASE_URL : DEFAULT_API_BASE_URL
}

export function getMockScenario(): MockScenario {
  const value = wx.getStorageSync(SCENARIO_KEY) as MockScenario
  return ['normal', 'empty', 'error', 'unbound', 'sessionExpired'].includes(value) ? value : 'normal'
}

export function setDataSourceMode(mode: DataSourceMode): void { wx.setStorageSync(MODE_KEY, mode) }
export function setMockScenario(scenario: MockScenario): void { wx.setStorageSync(SCENARIO_KEY, scenario) }
export function setApiBaseUrl(url: string): void {
  const value = url.trim().replace(/\/+$/, '')
  if (!value) { wx.removeStorageSync(API_BASE_URL_KEY); return }
  if (!/^https?:\/\//.test(value)) throw new Error('API 地址必须以 http:// 或 https:// 开头')
  wx.setStorageSync(API_BASE_URL_KEY, value)
}
export function isMockMode(): boolean { return getDataSourceMode() === 'mock' }
