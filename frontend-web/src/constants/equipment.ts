import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from '@/constants/production'

/**
 * 设备管理常量字典。
 * 字符串枚举与后端 equipment Controller VO 校验规则保持一致。
 */

/** 将字符串状态字典转换为下拉选项，并保留后端字符串枚举值。 */
function stringStatusMapToOptions(statusMap: StatusMap): OptionItem[] {
  return Object.entries(statusMap).map(([value, meta]) => ({ label: meta.text, value }))
}

// ---------- 权限 ----------

/** 设备主档、台账与报修接口当前仅要求登录，所有系统角色均可访问。 */
export const EQUIPMENT_VIEW_ROLES = Object.values(ROLES)
export const EQUIPMENT_COMMON_WRITE_ROLES = Object.values(ROLES)

/** 保养计划查询权限。 */
export const MAINTENANCE_PLAN_VIEW_ROLES = [
  ROLES.ADMIN,
  ROLES.WORKSHOP_MANAGER,
  ROLES.TEAM_LEADER,
  ROLES.OPERATOR,
]

/** 保养计划写操作权限。 */
export const MAINTENANCE_PLAN_WRITE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

/** 保养记录查询、新增和修改权限。 */
export const MAINTENANCE_RECORD_READ_WRITE_ROLES = [
  ROLES.ADMIN,
  ROLES.WORKSHOP_MANAGER,
  ROLES.TEAM_LEADER,
  ROLES.OPERATOR,
]

/** 保养记录删除权限。 */
export const MAINTENANCE_RECORD_DELETE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

// ---------- 通用启停状态 ----------

export const EQUIPMENT_ENABLE_STATUS_MAP: StatusMap = {
  1: { type: 'success', text: '启用' },
  0: { type: 'danger', text: '停用' },
}

export const EQUIPMENT_ENABLE_STATUS_OPTIONS: OptionItem[] = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

// ---------- 故障原理 ----------

export const EQUIPMENT_FAULT_LEVEL_MAP: StatusMap = {
  LOW: { type: 'info', text: '低' },
  MEDIUM: { type: 'primary', text: '中' },
  HIGH: { type: 'warning', text: '高' },
  CRITICAL: { type: 'danger', text: '严重' },
}

export const EQUIPMENT_FAULT_LEVEL_OPTIONS = stringStatusMapToOptions(EQUIPMENT_FAULT_LEVEL_MAP)

// ---------- 设备台账 ----------

export const EQUIPMENT_STATUS_MAP: StatusMap = {
  IDLE: { type: 'info', text: '空闲' },
  RUNNING: { type: 'success', text: '运行中' },
  STOPPED: { type: 'warning', text: '已停机' },
  REPAIRING: { type: 'danger', text: '维修中' },
  MAINTAINING: { type: 'primary', text: '保养中' },
  SCRAPPED: { type: 'info', text: '已报废' },
}

export const EQUIPMENT_STATUS_OPTIONS = stringStatusMapToOptions(EQUIPMENT_STATUS_MAP)

// ---------- 报修任务 ----------

export const EQUIPMENT_REPAIR_STATUS_MAP: StatusMap = {
  REPORTED: { type: 'warning', text: '已报修' },
  ASSIGNED: { type: 'primary', text: '已分派' },
  REPAIRING: { type: 'danger', text: '维修中' },
  FINISHED: { type: 'success', text: '已完成' },
  CANCELLED: { type: 'info', text: '已取消' },
}

export const EQUIPMENT_REPAIR_STATUS_OPTIONS = stringStatusMapToOptions(
  EQUIPMENT_REPAIR_STATUS_MAP,
)

// ---------- 保养计划 ----------

export const EQUIPMENT_MAINTENANCE_TYPE_MAP: StatusMap = {
  ROUTINE: { type: 'info', text: '例行保养' },
  PREVENTIVE: { type: 'success', text: '预防保养' },
  SPECIAL: { type: 'warning', text: '专项保养' },
}

export const EQUIPMENT_MAINTENANCE_TYPE_OPTIONS = stringStatusMapToOptions(
  EQUIPMENT_MAINTENANCE_TYPE_MAP,
)

// ---------- 保养记录 ----------

export const EQUIPMENT_MAINTENANCE_RESULT_MAP: StatusMap = {
  NORMAL: { type: 'success', text: '正常' },
  ABNORMAL: { type: 'danger', text: '异常' },
}

export const EQUIPMENT_MAINTENANCE_RESULT_OPTIONS = stringStatusMapToOptions(
  EQUIPMENT_MAINTENANCE_RESULT_MAP,
)

export const EQUIPMENT_MAINTENANCE_RECORD_STATUS_MAP: StatusMap = {
  PENDING: { type: 'info', text: '待执行' },
  IN_PROGRESS: { type: 'primary', text: '执行中' },
  COMPLETED: { type: 'success', text: '已完成' },
  CANCELLED: { type: 'danger', text: '已取消' },
}

export const EQUIPMENT_MAINTENANCE_RECORD_STATUS_OPTIONS = stringStatusMapToOptions(
  EQUIPMENT_MAINTENANCE_RECORD_STATUS_MAP,
)
