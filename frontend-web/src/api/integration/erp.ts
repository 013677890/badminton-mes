import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'
import type { IntegrationWriteLog, IntegrationWriteLogPageParams } from './log'

/**
 * ERP 同步接口，对齐后端 ErpSyncController（/api/integration/erp）。
 * 任务同步限 ADMIN、PMC；工艺同步/确认/驳回限 ADMIN、CRAFT_ENGINEER。
 */

// ---------- 任务同步 ----------

export interface ErpTaskSyncReq {
  sourceSystem?: string
  erpOrderNo?: string
  /** yyyy-MM-dd HH:mm:ss */
  startTime?: string
  /** yyyy-MM-dd HH:mm:ss */
  endTime?: string
}

export interface ErpTaskSyncDetail {
  erpOrderNo: string
  /** SUCCESS / FAILED / DUPLICATE */
  status: string
  /** 成功时生成的 MES 工单主键 */
  workOrderId: number | null
  /** 成功时生成的 MES 工单号 */
  workOrderNo: string | null
  errorCode: string | null
  errorMessage: string | null
}

export interface ErpTaskSyncResp {
  sourceSystem: string
  totalCount: number
  successCount: number
  failureCount: number
  duplicateCount: number
  details: ErpTaskSyncDetail[]
}

/** ERP 同步日志分页参数（固定查 interfaceType=ERP_TASK_SYNC，行结构同写入日志） */
export type ErpSyncLogPageParams = Pick<
  IntegrationWriteLogPageParams,
  'sourceSystem' | 'businessKey' | 'writeStatus'
>

// ---------- 工艺同步 ----------

export interface ErpCraftSyncReq {
  sourceSystem?: string
}

export interface ErpCraftPending {
  id: number
  sourceSystem: string
  erpRoutingCode: string
  erpRoutingName: string
  erpRoutingVersion: string
  productCode: string
  /** 见 constants/integration.ts ERP_CRAFT_PENDING_STATUS_MAP */
  status: number
  /** 确认后生成的工艺路线主键 */
  confirmedRouteId: number | null
  errorCode: string | null
  errorMessage: string | null
  createTime: string
}

export interface ErpCraftSyncResp {
  sourceSystem: string
  totalCount: number
  successCount: number
  failureCount: number
  duplicateCount: number
  pendingItems: ErpCraftPending[]
}

export interface ErpCraftPendingPageParams {
  status?: number
  sourceSystem?: string
  erpRoutingCode?: string
}

// ---------- 任务同步 ----------

/** 触发 ERP 生产任务单同步，返回逐条处理结果 */
export function syncErpTasks(data?: ErpTaskSyncReq): Promise<ErpTaskSyncResp> {
  return post('/integration/erp/tasks/sync', data ?? {})
}

/** 分页查询 ERP 任务同步日志（行结构同写入日志） */
export function getErpTaskSyncLogPage(
  params: ErpSyncLogPageParams & PageParam,
): Promise<PageResult<IntegrationWriteLog>> {
  return get('/integration/erp/tasks/sync_logs', params)
}

// ---------- 工艺同步 ----------

/** 触发 ERP 工艺数据同步，返回计数与待确认列表 */
export function syncErpCrafts(data?: ErpCraftSyncReq): Promise<ErpCraftSyncResp> {
  return post('/integration/erp/crafts/sync', data ?? {})
}

/** 分页查询待确认/已确认/异常/已驳回工艺数据 */
export function getErpCraftPendingPage(
  params: ErpCraftPendingPageParams & PageParam,
): Promise<PageResult<ErpCraftPending>> {
  return get('/integration/erp/crafts/pending', params)
}

/** 确认待确认工艺数据，返回新生成的工艺路线主键 */
export function confirmErpCraftPending(id: number): Promise<number> {
  return put(`/integration/erp/crafts/pending/${id}/confirm`)
}

/** 驳回待确认工艺数据，reason 必填 */
export function rejectErpCraftPending(id: number, reason: string): Promise<boolean> {
  return put(`/integration/erp/crafts/pending/${id}/reject`, { reason })
}
