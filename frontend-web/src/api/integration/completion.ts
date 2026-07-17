import { get } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 完工单读取接口，对齐后端 IntegrationController（/api/integration/completion_orders）。
 * 类级 @RequiresRoles：ADMIN、PMC。每次查询都会逐条落读取日志。
 */

export interface CompletionOrder {
  id: number
  completionNo: string
  /** 现场生产任务主键，历史完工单可空 */
  productionTaskId: number | null
  workOrderNo: string
  productCode: string
  productName: string
  batchNo: string
  completionQuantity: number
  goodQuantity: number
  defectQuantity: number
  /** 固定为 1（已审核） */
  auditStatus: number
  auditTime: string
  createTime: string
  updateTime: string
}

export interface CompletionOrderPageParams {
  /** 读取来源系统，后端 @NotBlank 必填，前端默认 MES */
  sourceSystem: string
  /** yyyy-MM-dd HH:mm:ss */
  startTime?: string
  /** yyyy-MM-dd HH:mm:ss */
  endTime?: string
  completionNo?: string
  workOrderNo?: string
}

export interface CompletionReadLog {
  id: number
  completionOrderId: number
  completionNo: string
  workOrderNo: string
  sourceSystem: string
  /** 调用用户主键 */
  readBy: number
  readTime: string
}

export interface CompletionReadLogPageParams {
  sourceSystem?: string
  completionNo?: string
  /** yyyy-MM-dd HH:mm:ss */
  startTime?: string
  /** yyyy-MM-dd HH:mm:ss */
  endTime?: string
}

/** 分页读取已审核完工单（每次逐条落读取日志） */
export function getCompletionOrderPage(
  params: CompletionOrderPageParams & PageParam,
): Promise<PageResult<CompletionOrder>> {
  return get('/integration/completion_orders', params)
}

/** 分页查询完工单读取日志 */
export function getCompletionReadLogPage(
  params: CompletionReadLogPageParams & PageParam,
): Promise<PageResult<CompletionReadLog>> {
  return get('/integration/completion_orders/read_logs', params)
}
