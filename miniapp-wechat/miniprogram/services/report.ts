import { request } from './http'
import { ProductionSummary, RealtimeDashboard, Trace } from '../types/api'

export const getDashboard = (workshopId?: number, lineId?: number) => request<RealtimeDashboard>({ url: '/api/report/mini_app/realtime_dashboard', method: 'GET', data: { workshopId, lineId } })
export const getProductionSummary = (startTime: string, endTime: string, workshopId?: number, lineId?: number) => request<ProductionSummary>({ url: '/api/report/mini_app/production_analysis', method: 'GET', data: { startTime, endTime, workshopId, lineId } })
export const getTrace = (batchCode: string) => request<Trace>({ url: '/api/report/mini_app/product_trace', method: 'GET', data: { batchCode } })
