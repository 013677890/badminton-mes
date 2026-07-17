import { ProductionSummary, RealtimeDashboard, Trace } from '../types/api'
import { isMockMode, getMockScenario } from './config'
import { request } from './http'
import { mockDashboard, mockSummary, mockTrace } from './mock'

// 实时看板只读后端聚合结果，可按车间或产线缩小数据范围。
export const getDashboard = (workshopId?: number, lineId?: number): Promise<RealtimeDashboard> => isMockMode() ? mockDashboard() : request<RealtimeDashboard>({ url: '/api/report/mini_app/realtime_dashboard', method: 'GET', data: { workshopId, lineId } })
// 生产分析按前端选择的时间区间查询，后端负责汇总产量、良率和设备状态。
export const getProductionSummary = (startTime: string, endTime: string, workshopId?: number, lineId?: number, period = '今日'): Promise<ProductionSummary> => isMockMode() ? mockSummary(period) : request<ProductionSummary>({ url: '/api/report/mini_app/production_analysis', method: 'GET', data: { startTime, endTime, workshopId, lineId } })
// 产品追溯以批次码为唯一入口，返回工艺、报工、维修和完整性提示。
export const getTrace = (batchCode: string): Promise<Trace> => isMockMode() ? mockTrace(batchCode) : request<Trace>({ url: '/api/report/mini_app/product_trace', method: 'GET', data: { batchCode } })
// 会话失效模拟场景供页面复用统一的失效态展示逻辑。
export const isSessionExpiredScenario = (): boolean => isMockMode() && getMockScenario() === 'sessionExpired'
