import type { OptionItem, StatusMap } from '@/types/components'

/**
 * 生产模块常量字典。
 *
 * 状态码与后端 module/production/enums、module/craft/enums 逐一对齐，
 * 改动枚举时两端需同步（后端是唯一事实源）。
 */

/** StatusMap → 下拉选项（value 转回数字，与后端 Integer 参数对齐） */
export function statusMapToOptions(map: StatusMap): OptionItem[] {
  return Object.entries(map).map(([value, meta]) => ({ label: meta.text, value: Number(value) }))
}

// ---------- 角色（与后端 RoleCodeConstants 对齐） ----------

export const ROLES = {
  ADMIN: 'ADMIN',
  PMC: 'PMC',
  WORKSHOP_MANAGER: 'WORKSHOP_MANAGER',
  TEAM_LEADER: 'TEAM_LEADER',
  OPERATOR: 'OPERATOR',
  INSPECTOR: 'INSPECTOR',
  CRAFT_ENGINEER: 'CRAFT_ENGINEER',
} as const

/** V3/V6 迁移种子的角色主键（顺序自增），选人下拉按角色查用户时使用 */
export const ROLE_SEED_IDS = {
  ADMIN: 1,
  PMC: 2,
  WORKSHOP_MANAGER: 3,
  TEAM_LEADER: 4,
  OPERATOR: 5,
  INSPECTOR: 6,
  CRAFT_ENGINEER: 7,
} as const

/** 基础资料查看（Controller 类级 @RequiresRoles） */
export const BASE_DATA_VIEW_ROLES = [
  ROLES.ADMIN, ROLES.PMC, ROLES.WORKSHOP_MANAGER, ROLES.CRAFT_ENGINEER,
]
/** 产品/物料/车间/产线写操作 */
export const BASE_DATA_WRITE_ROLES = [ROLES.ADMIN, ROLES.PMC]
/** BOM 写操作（工艺工程师也可维护） */
export const BOM_WRITE_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.CRAFT_ENGINEER]
/** 工单计划类操作（创建/修改/删除/下达/关闭/作废） */
export const WO_PLAN_ROLES = [ROLES.ADMIN, ROLES.PMC]
/** 工单执行类流转（暂停/恢复/完工） */
export const WO_EXEC_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.WORKSHOP_MANAGER]
/** 派工单创建/修改/取消 */
export const DISPATCH_EDIT_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.WORKSHOP_MANAGER]
/** 派工单审核/下发 */
export const DISPATCH_AUDIT_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]
/** 触发齐套分析 */
export const KIT_ANALYZE_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.WORKSHOP_MANAGER]
/** 欠料处理登记/解决 */
export const SHORTAGE_HANDLE_ROLES = [ROLES.ADMIN, ROLES.PMC]

// ---------- 通用启停状态 ----------

export const ENABLE_STATUS_MAP: StatusMap = {
  1: { type: 'success', text: '启用' },
  0: { type: 'danger', text: '停用' },
}
export const ENABLE_STATUS_OPTIONS: OptionItem[] = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

// ---------- 产品 / 物料 ----------

export const PRODUCT_TYPE_TEXT: Record<number, string> = {
  1: '成品',
  2: '半成品',
}
export const PRODUCT_TYPE_OPTIONS: OptionItem[] = [
  { label: '成品', value: 1 },
  { label: '半成品', value: 2 },
]

export const MATERIAL_TYPE_TEXT: Record<number, string> = {
  1: '球头',
  2: '羽毛',
  3: '胶水',
  4: '线材',
  5: '包装',
  9: '其他',
}
export const MATERIAL_TYPE_OPTIONS: OptionItem[] = Object.entries(MATERIAL_TYPE_TEXT).map(
  ([value, label]) => ({ label, value: Number(value) }),
)

// ---------- BOM ----------

export const BOM_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '草稿' },
  1: { type: 'success', text: '生效' },
  2: { type: 'danger', text: '停用' },
}
export const BOM_STATUS = { DRAFT: 0, EFFECTIVE: 1, DISABLED: 2 } as const

// ---------- 工艺路线 ----------

export const ROUTE_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '草稿' },
  1: { type: 'success', text: '生效' },
  2: { type: 'danger', text: '停用' },
}
export const ROUTE_STATUS = { DRAFT: 0, EFFECTIVE: 1, DISABLED: 2 } as const

// ---------- 生产工单 ----------

export const WORK_ORDER_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '已创建' },
  1: { type: 'primary', text: '已下达' },
  2: { type: 'warning', text: '生产中' },
  3: { type: 'danger', text: '暂停' },
  4: { type: 'success', text: '已完工' },
  5: { type: 'info', text: '已关闭' },
  6: { type: 'danger', text: '已作废' },
}
export const WO_STATUS = {
  CREATED: 0,
  RELEASED: 1,
  IN_PRODUCTION: 2,
  PAUSED: 3,
  FINISHED: 4,
  CLOSED: 5,
  CANCELLED: 6,
} as const

export const WORK_ORDER_SOURCE_TEXT: Record<number, string> = {
  1: '手工录入',
  2: '导入',
  3: 'ERP同步',
  4: 'API写入',
}

export const WORK_ORDER_CHANGE_TYPE_TEXT: Record<number, string> = {
  1: '状态流转',
  2: '计划变更',
}

/** 优先级 1 最高 - 9 最低，默认 5（与 t_work_order.priority 注释对齐） */
export const PRIORITY_OPTIONS: OptionItem[] = Array.from({ length: 9 }, (_, i) => {
  const value = i + 1
  const suffix = value === 1 ? '（最高）' : value === 5 ? '（默认）' : value === 9 ? '（最低）' : ''
  return { label: `${value}${suffix}`, value }
})

// ---------- 齐套 / 欠料 ----------

export const KIT_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '未分析' },
  1: { type: 'success', text: '齐套' },
  2: { type: 'warning', text: '部分齐套' },
  3: { type: 'danger', text: '欠料' },
}
export const KIT_STATUS = { NOT_ANALYZED: 0, COMPLETE: 1, PARTIAL: 2, SHORTAGE: 3 } as const

export const SHORTAGE_HANDLE_TYPE_TEXT: Record<number, string> = {
  1: '催采购',
  2: '调拨',
  3: '代用料',
  4: '调整排产',
}
export const SHORTAGE_HANDLE_TYPE_OPTIONS: OptionItem[] = Object.entries(
  SHORTAGE_HANDLE_TYPE_TEXT,
).map(([value, label]) => ({ label, value: Number(value) }))

export const SHORTAGE_HANDLE_STATUS_MAP: StatusMap = {
  0: { type: 'warning', text: '处理中' },
  1: { type: 'success', text: '已解决' },
}

// ---------- 派工单 ----------

export const DISPATCH_STATUS_MAP: StatusMap = {
  0: { type: 'warning', text: '待审核' },
  1: { type: 'primary', text: '已审核' },
  2: { type: 'success', text: '已下发' },
  3: { type: 'primary', text: '执行中' },
  4: { type: 'info', text: '已完成' },
  5: { type: 'danger', text: '已取消' },
}
export const DISPATCH_STATUS = {
  PENDING_AUDIT: 0,
  AUDITED: 1,
  ISSUED: 2,
  EXECUTING: 3,
  FINISHED: 4,
  CANCELLED: 5,
} as const

export const DISPATCH_ADJUST_TYPE_TEXT: Record<number, string> = {
  1: '系统建议',
  2: '人工创建',
  3: '调整',
  4: '审核',
  5: '下发',
  6: '取消',
}

// ---------- 静态种子字典（base_unit / base_shift，未提供查询接口） ----------

/** V8 迁移种子的计量单位 */
export const UNIT_OPTIONS: OptionItem[] = [{ label: '个', value: 1 }]

export interface ShiftSeed {
  id: number
  code: string
  name: string
  /** HH:mm:ss */
  startTime: string
  endTime: string
  /** 结束时间跨天（夜班） */
  crossDay: boolean
}

/** V5 迁移种子的班次；派工创建时按班次预填计划起止时间 */
export const SHIFT_SEEDS: ShiftSeed[] = [
  { id: 1, code: 'DAY', name: '白班', startTime: '08:00:00', endTime: '20:00:00', crossDay: false },
  { id: 2, code: 'NIGHT', name: '夜班', startTime: '20:00:00', endTime: '08:00:00', crossDay: true },
]

export const SHIFT_OPTIONS: OptionItem[] = SHIFT_SEEDS.map((shift) => ({
  label: `${shift.name}（${shift.startTime.slice(0, 5)}-${shift.endTime.slice(0, 5)}）`,
  value: shift.id,
}))

/**
 * 工单剩余可派数量，口径与后端 DispatchOrderServiceImpl 一致：
 * FLOOR(计划数量 × (1 + 超产比例/100)) − 已派数量。
 */
export function remainingDispatchQuantity(order: {
  planQuantity: number
  dispatchedQuantity: number
  overRatio?: number | null
}): number {
  const ratio = order.overRatio ?? 0
  return Math.floor(order.planQuantity * (1 + ratio / 100)) - order.dispatchedQuantity
}
