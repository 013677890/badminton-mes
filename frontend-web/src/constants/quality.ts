import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from './production'

/**
 * 质量管理模块常量字典。
 *
 * 字符串值与质量管理请求 VO 的校验规则逐一对齐，角色与 Controller 权限一致。
 */

/** 质量分类、项目、方案和检验单查询权限。 */
export const QUALITY_VIEW_ROLES = [
  ROLES.ADMIN,
  ROLES.INSPECTOR,
  ROLES.PMC,
  ROLES.WORKSHOP_MANAGER,
  ROLES.TEAM_LEADER,
]

/** 质量分类、项目、方案和检验单写操作权限。 */
export const QUALITY_WRITE_ROLES = [ROLES.ADMIN, ROLES.INSPECTOR]

/** 检验类型。 */
export const INSPECTION_TYPE_TEXT: Record<string, string> = {
  FIRST_ARTICLE: '首件检验',
  LAST_ARTICLE: '末件检验',
  PATROL: '巡检',
  WAREHOUSE_IN: '入库检验',
  SHIPMENT: '发货检验',
}
export const INSPECTION_TYPE_OPTIONS: OptionItem[] = Object.entries(INSPECTION_TYPE_TEXT).map(
  ([value, label]) => ({ label, value }),
)

/** 检验项目值类型。 */
export const INSPECTION_VALUE_TYPE_TEXT: Record<string, string> = {
  NUMERIC: '数值',
  TEXT: '文本',
  BOOLEAN: '布尔',
}
export const INSPECTION_VALUE_TYPE_OPTIONS: OptionItem[] = Object.entries(
  INSPECTION_VALUE_TYPE_TEXT,
).map(([value, label]) => ({ label, value }))

/** 检验项目判定方式。 */
export const JUDGMENT_METHOD_TEXT: Record<string, string> = {
  RANGE: '范围判定',
  STANDARD_VALUE: '标准值判定',
  MANUAL: '人工判定',
}
export const JUDGMENT_METHOD_OPTIONS: OptionItem[] = Object.entries(JUDGMENT_METHOD_TEXT).map(
  ([value, label]) => ({ label, value }),
)

/** 检验方案状态。 */
export const PLAN_STATUS = {
  DRAFT: 'DRAFT',
  EFFECTIVE: 'EFFECTIVE',
  DISABLED: 'DISABLED',
} as const
export const PLAN_STATUS_MAP: StatusMap = {
  DRAFT: { type: 'info', text: '草稿' },
  EFFECTIVE: { type: 'success', text: '生效' },
  DISABLED: { type: 'danger', text: '已停用' },
}
export const PLAN_STATUS_OPTIONS: OptionItem[] = Object.entries(PLAN_STATUS_MAP).map(
  ([value, meta]) => ({ label: meta.text, value }),
)

/** 检验单状态。 */
export const RECORD_STATUS = { DRAFT: 'DRAFT', SUBMITTED: 'SUBMITTED' } as const
export const RECORD_STATUS_MAP: StatusMap = {
  DRAFT: { type: 'info', text: '草稿' },
  SUBMITTED: { type: 'success', text: '已提交' },
}
export const RECORD_STATUS_OPTIONS: OptionItem[] = Object.entries(RECORD_STATUS_MAP).map(
  ([value, meta]) => ({ label: meta.text, value }),
)

/** 检验单结论。 */
export const INSPECTION_CONCLUSION = {
  PASS: 'PASS',
  CONCESSION: 'CONCESSION',
  REWORK: 'REWORK',
  SCRAP: 'SCRAP',
} as const
export const INSPECTION_CONCLUSION_MAP: StatusMap = {
  PASS: { type: 'success', text: '合格' },
  CONCESSION: { type: 'warning', text: '让步接收' },
  REWORK: { type: 'warning', text: '返工' },
  SCRAP: { type: 'danger', text: '报废' },
}
export const INSPECTION_CONCLUSION_OPTIONS: OptionItem[] = Object.entries(
  INSPECTION_CONCLUSION_MAP,
).map(([value, meta]) => ({ label: meta.text, value }))

/** 单项检验结果。 */
export const ITEM_JUDGMENT_RESULT_MAP: StatusMap = {
  PASS: { type: 'success', text: '合格' },
  FAIL: { type: 'danger', text: '不合格' },
}
export const ITEM_JUDGMENT_RESULT_OPTIONS: OptionItem[] = Object.entries(
  ITEM_JUDGMENT_RESULT_MAP,
).map(([value, meta]) => ({ label: meta.text, value }))

/** 检验单放行状态。 */
export const RELEASE_STATUS_MAP: StatusMap = {
  PENDING: { type: 'warning', text: '待判定' },
  RELEASED: { type: 'success', text: '已放行' },
  BLOCKED: { type: 'danger', text: '已阻断' },
}
