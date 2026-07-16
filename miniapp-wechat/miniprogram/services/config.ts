export type DataSourceMode = 'mock' | 'api'
export type MockScenario = 'normal' | 'empty' | 'error' | 'unbound' | 'sessionExpired'

const MODE_KEY = 'miniapp_data_source'
const SCENARIO_KEY = 'miniapp_mock_scenario'

export function getDataSourceMode(): DataSourceMode {
  const value = wx.getStorageSync(MODE_KEY) as DataSourceMode
  return value === 'api' ? 'api' : 'mock'
}

export function getMockScenario(): MockScenario {
  const value = wx.getStorageSync(SCENARIO_KEY) as MockScenario
  return ['normal', 'empty', 'error', 'unbound', 'sessionExpired'].includes(value) ? value : 'normal'
}

export function setDataSourceMode(mode: DataSourceMode): void { wx.setStorageSync(MODE_KEY, mode) }
export function setMockScenario(scenario: MockScenario): void { wx.setStorageSync(SCENARIO_KEY, scenario) }
export function isMockMode(): boolean { return getDataSourceMode() === 'mock' }
