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

/**
 * 分页查询生产工单，并将筛选条件与分页参数一起提交给后端。
 *
 * @param params 工单筛选条件和页码、页大小
 * @return 工单分页结果
 */
export function getWorkOrderPage(
  params: WorkOrderPageParams & PageParam,
): Promise<PageResult<WorkOrder>> {
  // 分页查询把筛选条件和页码统一交给后端，前端不自行截断结果。
  return get('/production/work_orders/page', params)
}

/**
 * 查询单个工单详情，用于详情页和编辑页回显。
 *
 * @param id 工单主键
 * @return 工单详情
 */
export function getWorkOrder(id: number): Promise<WorkOrder> {
  // 详情由后端优先读取 Redis，缓存未命中时回源数据库。
  return get(`/production/work_orders/${id}`)
}

/**
 * 创建生产工单。
 *
 * @param data 工单计划信息
 * @return 新建工单主键
 */
export function createWorkOrder(data: WorkOrderSaveParams): Promise<number> {
  // 后端按产品档案回填冗余信息，并在事务内写入工单主表。
  return post('/production/work_orders', data)
}

/** 仅"已创建"状态允许修改 */
export function updateWorkOrder(id: number, data: WorkOrderSaveParams): Promise<void> {
  // 只有已创建工单可完整编辑，已下达修改受后端状态和变更原因规则约束。
  return put(`/production/work_orders/${id}`, data)
}

/** 仅"已创建"状态允许删除（逻辑删除） */
export function deleteWorkOrder(id: number): Promise<void> {
  // 逻辑删除工单，后端会校验当前仍处于已创建状态。
  return del(`/production/work_orders/${id}`)
}

/** 已创建 → 已下达，并按 BOM 生成物料需求 */
export function releaseWorkOrder(id: number): Promise<void> {
  // 下达会校验有效 BOM/路线并生成工单物料需求。
  return put(`/production/work_orders/${id}/release`)
}

/** 已下达/生产中 → 暂停，原因必填 */
export function pauseWorkOrder(id: number, reason: string): Promise<void> {
  // 暂停请求携带原因，后端以 CAS 记录真实的暂停前状态。
  return put(`/production/work_orders/${id}/pause`, { reason })
}

/** 暂停 → 暂停前状态 */
export function resumeWorkOrder(id: number): Promise<void> {
  // 后端从最近暂停日志恢复到暂停前状态。
  return put(`/production/work_orders/${id}/resume`)
}

/** 已下达/生产中 → 已完工 */
export function finishWorkOrder(id: number): Promise<void> {
  // 完工状态和允许超产上限由数据库原子条件共同校验。
  return put(`/production/work_orders/${id}/finish`)
}

/** 已完工 → 已关闭 */
export function closeWorkOrder(id: number): Promise<void> {
  // 仅已完工工单允许关闭。
  return put(`/production/work_orders/${id}/close`)
}

/** 已创建/已下达 → 已作废，原因必填 */
export function cancelWorkOrder(id: number, reason: string): Promise<void> {
  // 作废会同步失效工单物料需求，并写入状态变更原因。
  return put(`/production/work_orders/${id}/cancel`, { reason })
}

/** 物料需求明细，未下达时为空集合 */
export function getWorkOrderMaterials(id: number): Promise<WorkOrderMaterial[]> {
  // 返回工单物料需求及批量回填的物料编码、名称。
  return get(`/production/work_orders/${id}/materials`)
}

/** 状态日志，最新在前 */
export function getWorkOrderStatusLogs(id: number): Promise<WorkOrderStatusLog[]> {
  // 返回工单状态和计划变更审计轨迹，最新记录在前。
  return get(`/production/work_orders/${id}/status_logs`)
}

/** 批量进度（最多 100 个 id） */
export function getWorkOrderProgress(ids: number[]): Promise<WorkOrderProgress[]> {
  // 将多个工单 id 编码为逗号分隔查询参数，用于看板批量回填进度。
  return get('/production/work_orders/progress', { ids: ids.join(',') })
}
