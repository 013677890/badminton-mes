import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 计件单价规则接口，对齐后端 PieceRateRuleController（/api/wage/rules）。
 * 查询限 ADMIN/WORKSHOP_MANAGER/TEAM_LEADER，写操作限 ADMIN/WORKSHOP_MANAGER。
 */

export interface PieceRateRule {
  id: number
  processId: number
  /** 空表示工序通用规则 */
  productId: number | null
  /** 单价，单位元，最多 4 位小数 */
  unitPrice: number
  /** 不良扣减率，百分比 */
  defectDeductionRate: number
  effectiveStart: string
  /** 空表示长期有效 */
  effectiveEnd: string | null
  status: number
  version: number
  createTime: string
  updateTime: string
}

export interface PieceRateRulePageParams {
  processId?: number
  productId?: number
  status?: number
  /** 查询指定日期生效的规则，yyyy-MM-dd */
  effectiveDate?: string
}

export interface PieceRateRuleSaveReq {
  processId: number
  productId?: number
  unitPrice: number
  defectDeductionRate: number
  effectiveStart: string
  effectiveEnd?: string
  status: number
  changeReason?: string
}

export interface WageRuleChangeLog {
  id: number
  ruleId: number
  /** 见 constants/wage.ts WAGE_RULE_CHANGE_TYPE_TEXT */
  changeType: string
  beforeSnapshot: string | null
  afterSnapshot: string | null
  changeReason: string | null
  operateBy: number
  operateTime: string
}

export function getRulePage(
  params: PieceRateRulePageParams & PageParam,
): Promise<PageResult<PieceRateRule>> {
  return get('/wage/rules/page', params)
}

export function getRule(id: number): Promise<PieceRateRule> {
  return get(`/wage/rules/${id}`)
}

export function createRule(data: PieceRateRuleSaveReq): Promise<number> {
  return post('/wage/rules', data)
}

export function updateRule(
  id: number,
  data: PieceRateRuleSaveReq & { version: number },
): Promise<void> {
  return put(`/wage/rules/${id}`, data)
}

export function updateRuleStatus(
  id: number,
  data: { version: number; status: number; reason: string },
): Promise<void> {
  return put(`/wage/rules/${id}/status`, data)
}

export function deleteRule(id: number, version: number): Promise<void> {
  return del(`/wage/rules/${id}`, { version })
}

export function getRuleChangeLogPage(
  id: number,
  params: PageParam,
): Promise<PageResult<WageRuleChangeLog>> {
  return get(`/wage/rules/${id}/change_logs`, params)
}
