import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from '@/constants/production'

export const BARCODE_VIEW_ROLES = Object.values(ROLES)
export const BARCODE_MANAGE_ROLES = [ROLES.ADMIN, ROLES.PMC]
export const BARCODE_PRINT_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.TEAM_LEADER]

export const BARCODE_ENTITY_STATUS_MAP: StatusMap = {
  1: { type: 'success', text: '启用' }, 0: { type: 'info', text: '停用' },
}
export const BARCODE_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '未使用' }, 1: { type: 'success', text: '已使用' }, 2: { type: 'danger', text: '已作废' },
}
export const BARCODE_STATUS_OPTIONS: OptionItem[] = [
  { label: '未使用', value: 0 }, { label: '已使用', value: 1 }, { label: '已作废', value: 2 },
]
export const BARCODE_MODE_OPTIONS: OptionItem[] = [{ label: '唯一码', value: 1 }, { label: '批次码', value: 2 }]
export const BARCODE_MODE_TEXT: Record<number, string> = { 1: '唯一码', 2: '批次码' }
export const BARCODE_SOURCE_OPTIONS: OptionItem[] = [{ label: '规则生成', value: 1 }, { label: '传入值生成', value: 2 }, { label: '外部导入', value: 3 }]
export const BARCODE_SOURCE_TEXT: Record<number, string> = { 1: '规则生成', 2: '传入值生成', 3: '外部导入' }
export const BARCODE_OBJECT_OPTIONS: OptionItem[] = [{ label: '产品', value: 1 }, { label: '物料', value: 2 }]
export const BARCODE_OBJECT_TEXT: Record<number, string> = { 1: '产品', 2: '物料' }
export const SERIAL_RESET_OPTIONS: OptionItem[] = [{ label: '按日', value: 1 }, { label: '按月', value: 2 }, { label: '不重置', value: 3 }]
export const RULE_ITEM_TYPE_OPTIONS: OptionItem[] = [{ label: '常量', value: 1 }, { label: '日期', value: 2 }, { label: '变量', value: 3 }, { label: '流水号', value: 4 }]
export const RULE_ITEM_TYPE_TEXT: Record<number, string> = { 1: '常量', 2: '日期', 3: '变量', 4: '流水号' }
export const TEMPLATE_FIELD_TYPE_OPTIONS: OptionItem[] = [{ label: '文本', value: 1 }, { label: '条码', value: 2 }, { label: '二维码', value: 3 }]
export const TEMPLATE_FIELD_TYPE_TEXT: Record<number, string> = { 1: '文本', 2: '条码', 3: '二维码' }
