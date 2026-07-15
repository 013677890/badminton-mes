import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 生产工单接口，对齐后端 WorkOrderController（/api/production/work_orders）。
 * 状态机：已创建0 → 已下达1 → 生产中2 ⇄ 暂停3 → 已完工4 → 已关闭5；
 * 已创建/已下达可作废6。仅"已创建"允许修改与删除。
 */

export interface WorkOrder {
  id: number
  workOrderNo: string
  /** 1 手工 2 导入 3 ERP 4 API */
  sourceType: number
  sourceSystem: string | null
  sourceOrderNo: string | null
  productId: number
  productName: string
  spec: string | null
  unitId: number
  batchNo: string | null
  bomId: number | null
  routingId: number | null
  customerId: number | null
  workshopId: number
  planQuantity: number
  dispatchedQuantity: number
  inputQuantity: number
  finishQuantity: number
  defectQuantity: number
  reworkQuantity: number
  /** 超产比例 %，剩余可派 = FLOOR(计划×(1+比例/100)) − 已派 */
  overRatio: number | null
  /** 1 最高 - 9 最低 */
  priority: number | null
  planStartTime: string
  planEndTime: string
  orderStatus: number
  /** 0 未分析 1 齐套 2 部分齐套 3 欠料 */
  kitStatus: number
  createTime: string
  updateTime: string
}

export interface WorkOrderSaveParams {
  /** 创建可选（后端可生成）；修改时忽略 */
  workOrderNo?: string
  productId: number
  batchNo?: string
  bomId?: number | null
  routingId?: number | null
  customerId?: number | null
  workshopId: number
  planQuantity: number
  overRatio?: number | null
  priority?: number | null
  /** yyyy-MM-dd HH:mm:ss */
  planStartTime: string
  planEndTime: string
  /** 修改已创建工单时的变更原因 */
  changeReason?: string
}

export interface WorkOrderPageParams {
  workOrderNo?: string
  workshopId?: number
  orderStatus?: number
  planEndTimeBegin?: string
  planEndTimeEnd?: string
}

export interface WorkOrderMaterial {
  id: number
  workOrderId: number
  materialId: number
  materialCode: string
  materialName: string
  requireQuantity: number
  issuedQuantity: number
}

export interface WorkOrderStatusLog {
  id: number
  workOrderId: number
  fromStatus: number | null
  toStatus: number
  /** 1 状态流转 2 计划变更 */
  changeType: number
  changeReason: string | null
  operateBy: number
  operateTime: string
}

export interface WorkOrderProgress {
  id: number
  workOrderNo: string
  productName: string
  planQuantity: number
  dispatchedQuantity: number
  inputQuantity: number
  finishQuantity: number
  defectQuantity: number
  reworkQuantity: number
  orderStatus: number
  progressPercent: number
}

export function getWorkOrderPage(
  params: WorkOrderPageParams & PageParam,
): Promise<PageResult<WorkOrder>> {
  return get('/production/work_orders/page', params)
}

export function getWorkOrder(id: number): Promise<WorkOrder> {
  return get(`/production/work_orders/${id}`)
}

export function createWorkOrder(data: WorkOrderSaveParams): Promise<number> {
  return post('/production/work_orders', data)
}

/** 仅"已创建"状态允许修改 */
export function updateWorkOrder(id: number, data: WorkOrderSaveParams): Promise<void> {
  return put(`/production/work_orders/${id}`, data)
}

/** 仅"已创建"状态允许删除（逻辑删除） */
export function deleteWorkOrder(id: number): Promise<void> {
  return del(`/production/work_orders/${id}`)
}

/** 已创建 → 已下达，并按 BOM 生成物料需求 */
export function releaseWorkOrder(id: number): Promise<void> {
  return put(`/production/work_orders/${id}/release`)
}

/** 已下达/生产中 → 暂停，原因必填 */
export function pauseWorkOrder(id: number, reason: string): Promise<void> {
  return put(`/production/work_orders/${id}/pause`, { reason })
}

/** 暂停 → 暂停前状态 */
export function resumeWorkOrder(id: number): Promise<void> {
  return put(`/production/work_orders/${id}/resume`)
}

/** 已下达/生产中 → 已完工 */
export function finishWorkOrder(id: number): Promise<void> {
  return put(`/production/work_orders/${id}/finish`)
}

/** 已完工 → 已关闭 */
export function closeWorkOrder(id: number): Promise<void> {
  return put(`/production/work_orders/${id}/close`)
}

/** 已创建/已下达 → 已作废，原因必填 */
export function cancelWorkOrder(id: number, reason: string): Promise<void> {
  return put(`/production/work_orders/${id}/cancel`, { reason })
}

/** 物料需求明细，未下达时为空集合 */
export function getWorkOrderMaterials(id: number): Promise<WorkOrderMaterial[]> {
  return get(`/production/work_orders/${id}/materials`)
}

/** 状态日志，最新在前 */
export function getWorkOrderStatusLogs(id: number): Promise<WorkOrderStatusLog[]> {
  return get(`/production/work_orders/${id}/status_logs`)
}

/** 批量进度（最多 100 个 id） */
export function getWorkOrderProgress(ids: number[]): Promise<WorkOrderProgress[]> {
  return get('/production/work_orders/progress', { ids: ids.join(',') })
}
