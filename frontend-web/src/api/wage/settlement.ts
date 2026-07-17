import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 计件工资结算接口，对齐后端 WageSettlementController（/api/wage/settlements）。
 * 状态机：草稿 0 →（提交）待审核 1 →（通过）已审核 2 /（驳回）已驳回 3；
 * 驳回后可重算再提交。查询限 ADMIN/WORKSHOP_MANAGER/TEAM_LEADER，
 * 计算/重算/审核/调整限 ADMIN/WORKSHOP_MANAGER。
 */

export interface WageSettlement {
  id: number
  settlementNo: string
  periodStart: string
  periodEnd: string
  /** 见 constants/wage.ts SETTLEMENT_STATUS_MAP */
  settlementStatus: number
  totalQualifiedQuantity: number
  totalDefectQuantity: number
  /** 最终金额，单位元 */
  totalAmount: number
  version: number
  submitBy: number | null
  submitTime: string | null
  auditBy: number | null
  auditTime: string | null
  auditReason: string | null
  createTime: string
  updateTime: string
}

export interface WageSettlementPageParams {
  settlementStatus?: number
  periodStartBegin?: string
  periodEndEnd?: string
}

export interface WageSettlementDetail {
  id: number
  settlementId: number
  workRecordId: number
  ruleId: number
  employeeId: number
  workDate: string
  workOrderId: number
  processId: number
  productId: number
  qualifiedQuantity: number
  defectQuantity: number
  unitPrice: number
  defectDeductionRate: number
  /** 系统计算金额 */
  calculatedAmount: number
  /** 人工调整金额，未调整为空 */
  adjustedAmount: number | null
  finalAmount: number
}

export interface WageSettlementAuditLog {
  id: number
  /** 明细级动作（ADJUST）才有值 */
  detailId: number | null
  /** 见 constants/wage.ts SETTLEMENT_ACTION_TEXT */
  actionType: string
  fromStatus: number | null
  toStatus: number | null
  beforeAmount: number | null
  afterAmount: number | null
  actionReason: string | null
  operateBy: number
  operateTime: string
}

export interface EmployeeWageSummary {
  employeeId: number
  employeeNo: string
  employeeName: string
  qualifiedQuantity: number
  defectQuantity: number
  /** 已审核工资金额，单位元 */
  totalAmount: number
}

export interface ProcessWageSummary {
  processId: number
  processCode: string
  processName: string
  qualifiedQuantity: number
  defectQuantity: number
  totalAmount: number
}

// ---------- 查询 ----------

export function getSettlementPage(
  params: WageSettlementPageParams & PageParam,
): Promise<PageResult<WageSettlement>> {
  return get('/wage/settlements/page', params)
}

export function getSettlement(id: number): Promise<WageSettlement> {
  return get(`/wage/settlements/${id}`)
}

/** 当前有效结算明细（驳回重算后旧明细失效不返回） */
export function getSettlementDetailPage(
  id: number,
  params: PageParam,
): Promise<PageResult<WageSettlementDetail>> {
  return get(`/wage/settlements/${id}/details`, params)
}

export function getSettlementAuditLogPage(
  id: number,
  params: PageParam,
): Promise<PageResult<WageSettlementAuditLog>> {
  return get(`/wage/settlements/${id}/audit_logs`, params)
}

/** 按员工汇总已审核工资 */
export function summarizeEmployees(params: {
  periodStart: string
  periodEnd: string
}): Promise<EmployeeWageSummary[]> {
  return get('/wage/settlements/summaries/employees', params)
}

/** 按工序汇总已审核工资 */
export function summarizeProcesses(params: {
  periodStart: string
  periodEnd: string
}): Promise<ProcessWageSummary[]> {
  return get('/wage/settlements/summaries/processes', params)
}

// ---------- 流转 ----------

/** 计算新结算批次，返回批次 id */
export function calculateSettlement(data: {
  periodStart: string
  periodEnd: string
  employeeIds?: number[]
  reason?: string
}): Promise<number> {
  return post('/wage/settlements/calculate', data)
}

/** 按原范围重算（草稿/已驳回） */
export function recalculateSettlement(id: number, version: number): Promise<void> {
  return post(`/wage/settlements/${id}/recalculate`, { version })
}

export function submitSettlement(
  id: number,
  data: { version: number; reason?: string },
): Promise<void> {
  return put(`/wage/settlements/${id}/submit`, data)
}

export function approveSettlement(
  id: number,
  data: { version: number; reason?: string },
): Promise<void> {
  return put(`/wage/settlements/${id}/approve`, data)
}

/** 驳回必须填写意见 */
export function rejectSettlement(
  id: number,
  data: { version: number; reason: string },
): Promise<void> {
  return put(`/wage/settlements/${id}/reject`, data)
}

/** 调整草稿结算明细金额 */
export function adjustSettlementDetail(
  id: number,
  detailId: number,
  data: { settlementVersion: number; adjustedAmount: number; reason: string },
): Promise<void> {
  return put(`/wage/settlements/${id}/details/${detailId}/adjust`, data)
}
