import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from '@/constants/production'

/**
 * 条码管理模块常量字典。
 * 枚举值与后端 barcode Controller VO 校验规则逐一对齐。
 */

/** 条码配置写操作权限（类型/规则/模板/应用规则的增删改启停）。 */
export const BARCODE_CONFIG_ROLES = [ROLES.ADMIN, ROLES.PMC]

/** 条码生成/作废/导入权限。 */
export const BARCODE_GENERATE_ROLES = [ROLES.ADMIN, ROLES.PMC]

/** 条码打印权限（班组长可分发打印）。 */
export const BARCODE_PRINT_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.TEAM_LEADER]

/** 通用启停状态（1 启用 / 0 停用）。 */
export const BARCODE_ENABLE_STATUS_MAP: StatusMap = {
  1: { type: 'success', text: '启用' },
  0: { type: 'danger', text: '停用' },
}
export const BARCODE_ENABLE_STATUS_OPTIONS: OptionItem[] = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

/** 条码模式（1 唯一码 / 2 批次码）。 */
export const BARCODE_MODE_TEXT: Record<number, string> = { 1: '唯一码', 2: '批次码' }
export const BARCODE_MODE_OPTIONS: OptionItem[] = [
  { label: '唯一码', value: 1 },
  { label: '批次码', value: 2 },
]

/** 条码来源类型（1 规则生成 / 2 传入值 / 3 外部导入）。 */
export const BARCODE_SOURCE_TEXT: Record<number, string> = {
  1: '规则生成',
  2: '传入值',
  3: '外部导入',
}
export const BARCODE_SOURCE_OPTIONS: OptionItem[] = [
  { label: '规则生成', value: 1 },
  { label: '传入值', value: 2 },
  { label: '外部导入', value: 3 },
]

/** 条码实例状态（0 未使用 / 1 已使用 / 2 已作废）。 */
export const BARCODE_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '未使用' },
  1: { type: 'success', text: '已使用' },
  2: { type: 'danger', text: '已作废' },
}
export const BARCODE_STATUS_OPTIONS: OptionItem[] = [
  { label: '未使用', value: 0 },
  { label: '已使用', value: 1 },
  { label: '已作废', value: 2 },
]

/** 规则明细段类型（1 常量 / 2 日期 / 3 变量 / 4 流水号）。 */
export const BARCODE_ITEM_TYPE_TEXT: Record<number, string> = {
  1: '常量',
  2: '日期',
  3: '变量',
  4: '流水号',
}
export const BARCODE_ITEM_TYPE_OPTIONS: OptionItem[] = [
  { label: '常量', value: 1 },
  { label: '日期', value: 2 },
  { label: '变量', value: 3 },
  { label: '流水号', value: 4 },
]

/** 流水号重置周期（1 按日 / 2 按月 / 3 不重置）。 */
export const SERIAL_RESET_CYCLE_TEXT: Record<number, string> = {
  1: '按日',
  2: '按月',
  3: '不重置',
}
export const SERIAL_RESET_CYCLE_OPTIONS: OptionItem[] = [
  { label: '按日', value: 1 },
  { label: '按月', value: 2 },
  { label: '不重置', value: 3 },
]

/** 模板字段类型（1 文本 / 2 条码 / 3 二维码）。 */
export const TEMPLATE_FIELD_TYPE_TEXT: Record<number, string> = {
  1: '文本',
  2: '条码',
  3: '二维码',
}
export const TEMPLATE_FIELD_TYPE_OPTIONS: OptionItem[] = [
  { label: '文本', value: 1 },
  { label: '条码', value: 2 },
  { label: '二维码', value: 3 },
]

/** 应用规则对象类型（1 产品 / 2 物料）。 */
export const APPLY_OBJECT_TYPE_TEXT: Record<number, string> = { 1: '产品', 2: '物料' }
export const APPLY_OBJECT_TYPE_OPTIONS: OptionItem[] = [
  { label: '产品', value: 1 },
  { label: '物料', value: 2 },
]

/** 条码使用记录类型（1 工序开工 / 2 工序完工 / 3 报工 / 4 其他）。 */
export const BARCODE_USE_TYPE_TEXT: Record<number, string> = {
  1: '工序开工',
  2: '工序完工',
  3: '报工',
  4: '其他',
}
