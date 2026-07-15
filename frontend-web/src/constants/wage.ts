import type { StatusMap } from '@/types/components'
import { ROLES } from './production'

/**
 * 计件工资模块常量字典。
 *
 * 状态码与后端 module/wage/enums 逐一对齐（后端是唯一事实源）。
 */

/** 规则/结算查询（PieceRateRuleController、WageSettlementController 类级） */
export const WAGE_VIEW_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER]
/** 规则写操作、结算计算/重算/审核/驳回/明细调整 */
export const WAGE_MANAGE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]
/** 报工快照查询（WageWorkRecordController 类级） */
export const WAGE_RECORD_VIEW_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

/** 结算状态（WageSettlementStatusEnum） */
export const SETTLEMENT_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '草稿' },
  1: { type: 'warning', text: '待审核' },
  2: { type: 'success', text: '已审核' },
  3: { type: 'danger', text: '已驳回' },
}
export const SETTLEMENT_STATUS = { DRAFT: 0, PENDING: 1, APPROVED: 2, REJECTED: 3 } as const

/** 结算审计动作（WageSettlementActionEnum 枚举名） */
export const SETTLEMENT_ACTION_TEXT: Record<string, string> = {
  CALCULATE: '首次计算',
  RECALCULATE: '重新计算',
  SUBMIT: '提交审核',
  APPROVE: '审核通过',
  REJECT: '审核驳回',
  ADJUST: '调整明细',
}

/** 规则变更类型（WageRuleChangeTypeEnum 枚举名） */
export const WAGE_RULE_CHANGE_TYPE_TEXT: Record<string, string> = {
  CREATE: '创建',
  UPDATE: '修改',
  STATUS: '启停',
  DELETE: '删除',
}
