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
  return get('/production/dispatch_orders/page', params)
}

export function getDispatch(id: number): Promise<DispatchOrder> {
  return get(`/production/dispatch_orders/${id}`)
}

export function createDispatch(data: DispatchSaveParams): Promise<number> {
  return post('/production/dispatch_orders', data)
}

/** 待审核/已审核直接改；已下发必填 adjustReason */
export function updateDispatch(id: number, data: DispatchSaveParams): Promise<void> {
  return put(`/production/dispatch_orders/${id}`, data)
}

export function auditDispatch(id: number): Promise<void> {
  return put(`/production/dispatch_orders/${id}/audit`)
}

export function issueDispatch(id: number): Promise<void> {
  return put(`/production/dispatch_orders/${id}/issue`)
}

export function cancelDispatch(id: number, reason: string): Promise<void> {
  return put(`/production/dispatch_orders/${id}/cancel`, { reason })
}

/** 调整日志，最新在前 */
export function getDispatchAdjustLogs(id: number): Promise<DispatchAdjustLog[]> {
  return get(`/production/dispatch_orders/${id}/adjust_logs`)
}

/** 排产建议（只读贪心填充）；注意查询参数为下划线风格 */
export function suggestDispatch(workOrderId: number): Promise<DispatchSuggest[]> {
  return get('/production/dispatch_orders/suggest', { work_order_id: workOrderId })
}

/** 产线排程视图（排除已取消），日期含两端 */
export function getLineSchedule(
  lineId: number,
  startDate: string,
  endDate: string,
): Promise<DispatchOrder[]> {
  return get('/production/dispatch_orders/schedule', {
    line_id: lineId,
    start_date: startDate,
    end_date: endDate,
  })
}
