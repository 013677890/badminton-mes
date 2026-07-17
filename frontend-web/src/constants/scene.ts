import { ROLES } from '@/constants/production'
import { statusMapToOptions } from '@/constants/production'
import type { OptionItem, StatusMap } from '@/types/components'

// ---------- 角色（与后端 Controller @RequiresRoles 对齐） ----------

/** 现场执行操作台：报工审核、完工单创建/审核/作废 */
export const SCENE_EXECUTION_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER]
/** 生产任务管理：创建/修改/审核/下达/关闭 */
export const SCENE_TASK_MANAGE_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.WORKSHOP_MANAGER]
/** 生产任务执行：开始/暂停/恢复 */
export const SCENE_TASK_EXEC_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER]
/** 派工管理：生成/确认/取消 */
export const SCENE_DISPATCH_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER]
/** 工序作业：扫码/开工/暂停/完工 */
export const SCENE_OPERATION_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER, ROLES.OPERATOR]
/** 报工提交 */
export const SCENE_REPORT_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER, ROLES.OPERATOR]
/** 报工冲销、设备计数报工 */
export const SCENE_REPORT_AUDIT_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]
/** 完工单创建/修改/提交 */
export const SCENE_COMPLETION_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER]
/** 完工单审核/同步 */
export const SCENE_COMPLETION_AUDIT_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

// ---------- 生产任务状态（SceneTaskStatusEnum 0-7） ----------

export const SCENE_TASK_STATUS = {
  PENDING_AUDIT: 0,
  AUDITED: 1,
  RELEASED: 2,
  IN_PRODUCTION: 3,
  PAUSED: 4,
  FINISHED: 5,
  CLOSED: 6,
  CANCELLED: 7,
} as const

export const SCENE_TASK_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '待审核' },
  1: { type: 'primary', text: '已审核' },
  2: { type: 'warning', text: '已下达' },
  3: { type: 'primary', text: '进行中' },
  4: { type: 'danger', text: '已暂停' },
  5: { type: 'success', text: '已完工' },
  6: { type: 'info', text: '已关闭' },
  7: { type: 'info', text: '已取消' },
}
export const SCENE_TASK_STATUS_OPTIONS: OptionItem[] = statusMapToOptions(SCENE_TASK_STATUS_MAP)

// ---------- 派工状态（SceneDispatchStatusEnum 0-4） ----------

export const SCENE_DISPATCH_STATUS = {
  PENDING_CONFIRM: 0,
  CONFIRMED: 1,
  IN_PROGRESS: 2,
  COMPLETED: 3,
  CANCELLED: 4,
} as const

export const SCENE_DISPATCH_STATUS_MAP: StatusMap = {
  0: { type: 'warning', text: '待确认' },
  1: { type: 'primary', text: '已确认' },
  2: { type: 'primary', text: '进行中' },
  3: { type: 'success', text: '已完成' },
  4: { type: 'info', text: '已取消' },
}
export const SCENE_DISPATCH_STATUS_OPTIONS: OptionItem[] = statusMapToOptions(SCENE_DISPATCH_STATUS_MAP)

// ---------- 工序作业状态（SceneOperationStatusEnum 0-3） ----------

export const SCENE_OPERATION_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '待开始' },
  1: { type: 'primary', text: '进行中' },
  2: { type: 'success', text: '已完工' },
  3: { type: 'danger', text: '异常' },
}
export const SCENE_OPERATION_STATUS_OPTIONS: OptionItem[] = statusMapToOptions(SCENE_OPERATION_STATUS_MAP)

// ---------- 完工单状态（SceneCompletionOrderServiceImpl 内部常量 0-3） ----------

export const SCENE_COMPLETION_STATUS = {
  DRAFT: 0,
  PENDING_AUDIT: 1,
  APPROVED: 2,
  REJECTED: 3,
} as const

export const SCENE_COMPLETION_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '草稿' },
  1: { type: 'warning', text: '待审核' },
  2: { type: 'success', text: '已审核' },
  3: { type: 'danger', text: '已驳回' },
}
export const SCENE_COMPLETION_STATUS_OPTIONS: OptionItem[] = statusMapToOptions(SCENE_COMPLETION_STATUS_MAP)

// ---------- 返修工单状态（SceneRepairStatusEnum，字符串枚举） ----------

export const SCENE_REPAIR_STATUS_MAP: StatusMap = {
  PENDING_ASSIGN: { type: 'warning', text: '待分配' },
  PENDING_REPAIR: { type: 'primary', text: '待返修' },
  REPAIRING: { type: 'primary', text: '返修中' },
  PENDING_RECHECK: { type: 'warning', text: '待复检' },
  RELEASED: { type: 'success', text: '已放行' },
  CONTINUE_REPAIR: { type: 'danger', text: '继续返修' },
  SCRAPPED: { type: 'danger', text: '已报废' },
  CLOSED: { type: 'info', text: '已关闭' },
}
export const SCENE_REPAIR_STATUS_OPTIONS: OptionItem[] = Object.entries(SCENE_REPAIR_STATUS_MAP).map(
  ([value, meta]) => ({ label: meta.text, value }),
)

/** 返修复检结果（SceneRepairRecheckReqVO result 字段取值） */
export const SCENE_REPAIR_RECHECK_OPTIONS: OptionItem[] = [
  { label: '放行', value: 'RELEASED' },
  { label: '继续返修', value: 'CONTINUE_REPAIR' },
  { label: '报废', value: 'SCRAPPED' },
]

// ---------- 产品批次状态（SceneBatchStatusEnum 1-6） ----------

export const SCENE_BATCH_STATUS_MAP: StatusMap = {
  1: { type: 'primary', text: '生产中' },
  2: { type: 'warning', text: '待检验' },
  3: { type: 'danger', text: '返修中' },
  4: { type: 'danger', text: '隔离' },
  5: { type: 'success', text: '已完工' },
  6: { type: 'info', text: '已报废' },
}
export const SCENE_BATCH_STATUS_OPTIONS: OptionItem[] = statusMapToOptions(SCENE_BATCH_STATUS_MAP)

// ---------- 报工类型 ----------

export const SCENE_REPORT_TYPE_TEXT: Record<number, string> = {
  1: '人工报工',
  2: '设备计数报工',
}
export const SCENE_REPORT_TYPE_OPTIONS: OptionItem[] = Object.entries(SCENE_REPORT_TYPE_TEXT).map(
  ([value, label]) => ({ label, value: Number(value) }),
)

// ---------- 生产参数值类型（SceneParameterValueTypeEnum 1-4） ----------

export const SCENE_PARAM_VALUE_TYPE_TEXT: Record<number, string> = {
  1: '开关',
  2: '数值',
  3: '枚举',
  4: '文本',
}
export const SCENE_PARAM_VALUE_TYPE_OPTIONS: OptionItem[] = Object.entries(
  SCENE_PARAM_VALUE_TYPE_TEXT,
).map(([value, label]) => ({ label, value: Number(value) }))

// ---------- 是/否通用选项 ----------

export const YES_NO_OPTIONS: OptionItem[] = [
  { label: '是', value: 'true' },
  { label: '否', value: 'false' },
]
