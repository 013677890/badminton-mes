import { request } from './http'
import { ProductionSummary, RealtimeDashboard, Trace } from '../types/api'

// 实时看板只读后端聚合结果，可按车间或产线缩小数据范围。
export const getDashboard = (workshopId?: number, lineId?: number) => request<RealtimeDashboard>({ url: '/api/report/mini_app/realtime_dashboard', method: 'GET', data: { workshopId, lineId } })
// 生产分析按前端选择的时间区间查询，后端负责汇总产量、良率和设备状态。
export const getProductionSummary = (startTime: string, endTime: string, workshopId?: number, lineId?: number) => request<ProductionSummary>({ url: '/api/report/mini_app/production_analysis', method: 'GET', data: { startTime, endTime, workshopId, lineId } })
// 产品追溯以批次码为唯一入口，返回工艺、报工、维修和完整性提示。
export const getTrace = (batchCode: string) => request<Trace>({ url: '/api/report/mini_app/product_trace', method: 'GET', data: { batchCode } })
