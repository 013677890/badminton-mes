import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from './production'

/**
 * 接口管理（integration）模块常量字典。
 *
 * 枚举值与后端 module/integration/enums 逐一对齐（后端为唯一事实源）。
 * 角色常量与 Controller 的 @RequiresRoles 对齐，用于路由 meta 与按钮显隐。
 */

/** 写入日志 / 异常池 / 完工单查询（IntegrationController 类级 @RequiresRoles） */
export const INTEGRATION_MANAGE_ROLES = [ROLES.ADMIN, ROLES.PMC]
/** ERP 任务同步（ErpSyncController tasks 端点） */
export const ERP_TASK_ROLES = [ROLES.ADMIN, ROLES.PMC]
/** ERP 工艺同步与确认/驳回（ErpSyncController crafts 端点） */
export const ERP_CRAFT_ROLES = [ROLES.ADMIN, ROLES.CRAFT_ENGINEER]
/** 菜单组级角色：各子路由并集，CRAFT_ENGINEER 仅可见工艺待确认 */
export const INTEGRATION_GROUP_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.CRAFT_ENGINEER]

// ---------- 接口类型（IntegrationInterfaceTypeEnum，字符串值） ----------

export const INTERFACE_TYPE_TEXT: Record<string, string> = {
  UNIT_WRITE: '计量单位写入',
  WORK_ORDER_WRITE: '工单写入',
  DISPATCH_ORDER_WRITE: '任务单写入',
  ERP_TASK_SYNC: 'ERP任务同步',
  ERP_CRAFT_SYNC: 'ERP工艺同步',
  DEVICE_COUNT_WRITE: '设备计数写入',
  COMPLETION_ORDER_READ: '完工单读取',
}
export const INTERFACE_TYPE_OPTIONS: OptionItem[] = Object.entries(INTERFACE_TYPE_TEXT).map(
  ([value, label]) => ({ label, value }),
)

// ---------- 写入状态（IntegrationWriteStatusEnum，1/2/3） ----------

export const WRITE_STATUS_MAP: StatusMap = {
  1: { type: 'success', text: '成功' },
  2: { type: 'danger', text: '失败' },
  3: { type: 'warning', text: '重复' },
}
export const WRITE_STATUS = { SUCCESS: 1, FAILED: 2, DUPLICATE: 3 } as const

/** ERP 任务同步明细处理状态（字符串 SUCCESS/FAILED/DUPLICATE） */
export const ERP_TASK_DETAIL_STATUS_TEXT: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  DUPLICATE: '重复',
}
export const ERP_TASK_DETAIL_STATUS_MAP: StatusMap = {
  SUCCESS: { type: 'success', text: '成功' },
  FAILED: { type: 'danger', text: '失败' },
  DUPLICATE: { type: 'warning', text: '重复' },
}

// ---------- ERP 工艺待确认状态（ErpCraftPendingStatusEnum，0/1/2/3） ----------

export const ERP_CRAFT_PENDING_STATUS_MAP: StatusMap = {
  0: { type: 'warning', text: '待确认' },
  1: { type: 'success', text: '已确认' },
  2: { type: 'danger', text: '异常' },
  3: { type: 'info', text: '已驳回' },
}
export const ERP_CRAFT_PENDING_STATUS = {
  PENDING: 0,
  CONFIRMED: 1,
  FAILED: 2,
  REJECTED: 3,
} as const

// ---------- 设备计数异常处理状态（0/1/2） ----------

export const DEVICE_EXCEPTION_HANDLE_STATUS_MAP: StatusMap = {
  0: { type: 'warning', text: '待处理' },
  1: { type: 'success', text: '已处理' },
  2: { type: 'info', text: '已忽略' },
}
export const DEVICE_EXCEPTION_HANDLE_STATUS = {
  PENDING: 0,
  HANDLED: 1,
  IGNORED: 2,
} as const

// ---------- 设备计数异常类型（DeviceCountExceptionTypeEnum，字符串值） ----------

export const EXCEPTION_TYPE_TEXT: Record<string, string> = {
  DISPATCH_NOT_FOUND: '派工单不存在',
  DISPATCH_STATUS_INVALID: '派工单状态不允许计数',
  PROCESS_NOT_FOUND: '工序不存在',
  COUNT_NON_POSITIVE: '计数值必须大于零',
  COUNT_ROLLBACK: '计数值发生倒退',
  EQUIPMENT_NOT_BOUND: '设备不存在或未启用',
  LINE_MISMATCH: '设备与派工产线不匹配',
  PROCESS_MISMATCH: '设备与工序不匹配',
  COUNT_JUMP: '计数增量异常跳变',
}
export const EXCEPTION_TYPE_OPTIONS: OptionItem[] = Object.entries(EXCEPTION_TYPE_TEXT).map(
  ([value, label]) => ({ label, value }),
)

// ---------- 完工单审核状态（CompletionAuditStatusEnum，0/1/2；查询接口只出 1） ----------

export const COMPLETION_AUDIT_STATUS_MAP: StatusMap = {
  0: { type: 'warning', text: '待审核' },
  1: { type: 'success', text: '已审核' },
  2: { type: 'danger', text: '已作废' },
}

/** 写入结果状态（IntegrationWriteResultRespVO.status，字符串） */
export const WRITE_RESULT_STATUS_TEXT: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  DUPLICATE: '重复',
}
