import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 派工单接口，对齐后端 DispatchOrderController（/api/production/dispatch_orders）。
 * 状态机：待审核0 → 已审核1 → 已下发2 → 执行中3 → 已完成4；
 * 待审核/已审核/已下发可取消5（回退工单已派数量）。
 * 已下发后修改必须携带调整原因。
 */

export interface DispatchOrder {
  id: number
  dispatchNo: string
  workOrderId: number
  workOrderNo: string
  productName: string
  kitStatus: number
  lineId: number
  lineName: string
  shiftId: number
  shiftName: string
  /** yyyy-MM-dd */
  planDate: string
  planQuantity: number
  planStartTime: string
  planEndTime: string
  /** 1 采纳系统建议创建 */
  suggest: number
  dispatchStatus: number
  auditBy: number | null
  auditTime: string | null
  adjustReason: string | null
  createBy: number
  createTime: string
}

export interface DispatchSaveParams {
  workOrderId: number
  lineId: number
  shiftId: number
  planDate: string
  planQuantity: number
  planStartTime: string
  planEndTime: string
  /** 采纳排产建议时置 true，落审计 */
  suggest?: boolean
  /** 已下发后修改必填 */
  adjustReason?: string
}

export interface DispatchPageParams {
  workOrderId?: number
  lineId?: number
  shiftId?: number
  dispatchStatus?: number
  planDateBegin?: string
  planDateEnd?: string
}

export interface DispatchSuggest {
  workOrderId: number
  kitStatus: number
  lineId: number
  lineName: string
  shiftId: number
  shiftName: string
  planDate: string
  planStartTime: string
  planEndTime: string
  planQuantity: number
  /** 建议整体能否按交期完成 */
  canFinishOnTime: boolean
}

export interface DispatchAdjustLog {
  id: number
  dispatchOrderId: number
  /** 1 系统建议 2 人工创建 3 调整 4 审核 5 下发 6 取消 */
  adjustType: number
  beforeSnapshot: string | null
  afterSnapshot: string | null
  adjustReason: string | null
  operatorId: number
  createTime: string
}

export function getDispatchPage(
  params: DispatchPageParams & PageParam,
): Promise<PageResult<DispatchOrder>> {
  // 列表筛选和分页由后端动态查询完成，页面只维护查询参数状态。
  return get('/production/dispatch_orders/page', params)
}

export function getDispatch(id: number): Promise<DispatchOrder> {
  // 读取派工单详情，响应中的工单、产线和班次名称由后端批量回填。
  return get(`/production/dispatch_orders/${id}`)
}

export function createDispatch(data: DispatchSaveParams): Promise<number> {
  // 创建时由后端锁定工单并校验剩余可派量、工作日和产能。
  return post('/production/dispatch_orders', data)
}

/** 待审核/已审核直接改；已下发必填 adjustReason */
export function updateDispatch(id: number, data: DispatchSaveParams): Promise<void> {
  // 修改后端会按数量差量更新工单已派数量，已下发状态还会要求调整原因。
  return put(`/production/dispatch_orders/${id}`, data)
}

export function auditDispatch(id: number): Promise<void> {
  // 审核接口只触发待审核到已审核的 CAS 状态转换。
  return put(`/production/dispatch_orders/${id}/audit`)
}

export function issueDispatch(id: number): Promise<void> {
  // 下发接口先创建现场任务，再将派工单推进到已下发状态。
  return put(`/production/dispatch_orders/${id}/issue`)
}

export function cancelDispatch(id: number, reason: string): Promise<void> {
  // 取消会由后端回退工单已派数量，并将取消原因写入调整日志。
  return put(`/production/dispatch_orders/${id}/cancel`, { reason })
}

/** 调整日志，最新在前 */
export function getDispatchAdjustLogs(id: number): Promise<DispatchAdjustLog[]> {
  // 返回派工建议、调整、审核、下发和取消的完整审计轨迹。
  return get(`/production/dispatch_orders/${id}/adjust_logs`)
}

/** 排产建议（只读贪心填充）；注意查询参数为下划线风格 */
export function suggestDispatch(workOrderId: number): Promise<DispatchSuggest[]> {
  // 只读计算排产建议，不会创建派工单或占用工单已派数量。
  return get('/production/dispatch_orders/suggest', { work_order_id: workOrderId })
}

/** 产线排程视图（排除已取消），日期含两端 */
export function getLineSchedule(
  lineId: number,
  startDate: string,
  endDate: string,
): Promise<DispatchOrder[]> {
  // 日期区间是闭区间，后端排除已取消派工单后按日期和班次返回排程。
  return get('/production/dispatch_orders/schedule', {
    line_id: lineId,
    start_date: startDate,
    end_date: endDate,
  })
}
