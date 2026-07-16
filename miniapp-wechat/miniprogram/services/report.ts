import { ProductionSummary, RealtimeDashboard, Trace } from '../types/api'
import { isMockMode, getMockScenario } from './config'
import { request } from './http'
import { mockDashboard, mockSummary, mockTrace } from './mock'

export const getDashboard = (workshopId?: number, lineId?: number): Promise<RealtimeDashboard> => isMockMode() ? mockDashboard() : request<RealtimeDashboard>({ url: '/api/report/mini_app/realtime_dashboard', method: 'GET', data: { workshopId, lineId } })
export const getProductionSummary = (startTime: string, endTime: string, workshopId?: number, lineId?: number, period = '今日'): Promise<ProductionSummary> => isMockMode() ? mockSummary(period) : request<ProductionSummary>({ url: '/api/report/mini_app/production_analysis', method: 'GET', data: { startTime, endTime, workshopId, lineId } })
export const getTrace = (batchCode: string): Promise<Trace> => isMockMode() ? mockTrace(batchCode) : request<Trace>({ url: '/api/report/mini_app/product_trace', method: 'GET', data: { batchCode } })
export const isSessionExpiredScenario = (): boolean => isMockMode() && getMockScenario() === 'sessionExpired'
