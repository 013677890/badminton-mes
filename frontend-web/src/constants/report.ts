import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from './production'

/**
 * 报表分析模块常量字典。
 *
 * 字符串值与后端 report 模块 VO 枚举逐一对齐，导出角色与 Controller
 * 的 @RequiresRoles（ADMIN / PMC / WORKSHOP_MANAGER）一致。
 */

/** 报表查看权限：所有角色可见 */
export const REPORT_VIEW_ROLES = Object.values(ROLES)

/** 报表导出权限：与后端 @RequiresRoles 对齐 */
export const REPORT_EXPORT_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.WORKSHOP_MANAGER]

/** 不良视图类型：来源明细 / 综合归并 */
export const DEFECT_VIEW_TYPE = {
  SOURCE: 'SOURCE',
  COMPREHENSIVE: 'COMPREHENSIVE',
} as const

export const DEFECT_VIEW_OPTIONS: OptionItem[] = [
  { label: '来源明细', value: DEFECT_VIEW_TYPE.SOURCE },
  { label: '综合归并', value: DEFECT_VIEW_TYPE.COMPREHENSIVE },
]

/** 数据完整状态（产品追溯 dataCompleteness、实时生产 dataStatus 共用） */
export const DATA_COMPLETENESS_MAP: StatusMap = {
  COMPLETE: { type: 'success', text: '完整' },
  PARTIAL: { type: 'warning', text: '部分' },
  EMPTY: { type: 'danger', text: '空' },
}

/** 报工记录类型：1 正常发生 / 2 冲销（与后端 SceneWorkReportTransactionalService 对齐） */
export const REPORT_RECORD_TYPE_MAP: StatusMap = {
  1: { type: 'primary', text: '正常' },
  2: { type: 'warning', text: '冲销' },
}

/** 追溯来源类型映射（OptionalSourceItem.sourceType） */
export const TRACE_SOURCE_TYPE_TEXT: Record<string, string> = {
  PACKING: '装箱记录',
  QUALITY_DEFECT: '质量不良',
  REPAIR: '返修记录',
  EQUIPMENT_STATUS: '设备状态',
  ANDON_EXCEPTION: '安灯异常',
}

/** 条码使用类型（BarcodeUse.useType） */
export const BARCODE_USE_TYPE_TEXT: Record<number, string> = {
  1: '投料',
  2: '完工',
  3: '质检',
}

/** 工序履历动作类型（ProcessHistory.actionType） */
export const PROCESS_ACTION_TYPE_TEXT: Record<number, string> = {
  1: '开工',
  2: '完工',
  3: '暂停',
  4: '恢复',
  5: '转序',
}
