import { get, post, put } from '@/utils/request'

/**
 * 齐套分析与欠料处理接口，对齐后端 KitAnalysisController（/api/production/kit_analysis）。
 */

export interface KitAnalysisRow {
  id: number
  workOrderId: number
  materialId: number
  materialCode: string
  materialName: string
  requireQuantity: number
  availableQuantity: number
  transitQuantity: number
  shortageQuantity: number
  /** 行级：1 齐套 3 欠料 */
  kitStatus: number
  analysisTime: string
}

export interface ShortageBoardRow {
  materialId: number
  materialCode: string
  materialName: string
  totalShortage: number
  affectedOrderCount: number
  transitQuantity: number
  expectedArrivalDate: string | null
}

export interface ShortageOrderRow {
  workOrderId: number
  workOrderNo: string
  productName: string
  requireQuantity: number
  availableQuantity: number
  shortageQuantity: number
}

export interface ShortageHandle {
  id: number
  workOrderId: number
  materialId: number
  materialCode: string
  materialName: string
  /** 1 催采购 2 调拨 3 代用料 4 调整排产 */
  handleType: number
  handlerId: number
  expectedArrivalDate: string | null
  handleRemark: string | null
  /** 0 处理中 1 已解决 */
  handleStatus: number
  createTime: string
}

export interface ShortageHandleSaveParams {
  workOrderId: number
  materialId: number
  handleType: number
  handlerId: number
  expectedArrivalDate?: string
  handleRemark?: string
}

/** 执行（重新）齐套分析，返回工单级齐套状态 */
export function analyzeWorkOrder(workOrderId: number): Promise<number> {
  // 后端会锁工单、重建最新分析快照并更新工单级齐套状态。
  return post(`/production/kit_analysis/work_orders/${workOrderId}/analyze`)
}

/** 工单最新逐物料分析结果，未分析时为空集合 */
export function getKitResult(workOrderId: number): Promise<KitAnalysisRow[]> {
  // 读取当前未逻辑删除的分析行，未分析时后端返回空数组。
  return get(`/production/kit_analysis/work_orders/${workOrderId}`)
}

/** 欠料看板汇总（按物料聚合，欠料量降序） */
export function getShortageBoard(): Promise<ShortageBoardRow[]> {
  // 获取数据库聚合后的物料欠料看板，不请求每个工单明细。
  return get('/production/kit_analysis/shortage_board')
}

/** 看板下钻：某物料影响的工单明细 */
export function getShortageOrdersByMaterial(materialId: number): Promise<ShortageOrderRow[]> {
  // 下钻查询指定物料对应的欠料工单，供看板详情展示。
  return get(`/production/kit_analysis/shortage_board/materials/${materialId}/work_orders`)
}

export function createShortageHandle(data: ShortageHandleSaveParams): Promise<number> {
  // 登记催料、调拨、代用或调整排产等处置记录，初始状态为处理中。
  return post('/production/kit_analysis/shortage_handles', data)
}

export function resolveShortageHandle(id: number): Promise<void> {
  // 后端通过状态 CAS 保证重复点击不会重复完成同一处理记录。
  return put(`/production/kit_analysis/shortage_handles/${id}/resolve`)
}

/** 工单的处理记录，最新在前 */
export function getShortageHandles(workOrderId: number): Promise<ShortageHandle[]> {
  // 按工单倒序读取欠料处理轨迹，便于页面显示最近处理动作。
  return get(`/production/kit_analysis/work_orders/${workOrderId}/shortage_handles`)
}
