import type { OptionItem } from '@/types/components'
import { ROLES } from './production'

/**
 * 工艺模块常量字典。
 *
 * 状态码与后端 module/craft/enums 逐一对齐，路线状态复用
 * constants/production.ts 的 ROUTE_STATUS（工单创建也在用）。
 */

/** 工序 / 工艺路线 / SOP / 不良原因写操作（Controller @RequiresRoles） */
export const CRAFT_WRITE_ROLES = [ROLES.ADMIN, ROLES.CRAFT_ENGINEER]

/** 工序类型编码：后端为自由字符串，约定三类；未知编码降级原样展示 */
export const PROCESS_TYPE_TEXT: Record<string, string> = {
  PREPARATION: '准备',
  PROCESSING: '加工',
  INSPECTION: '检验',
}
export const PROCESS_TYPE_OPTIONS: OptionItem[] = Object.entries(PROCESS_TYPE_TEXT).map(
  ([value, label]) => ({ label: `${label}（${value}）`, value }),
)

/** 路线来源（CraftRouteEntity.sourceType） */
export const ROUTE_SOURCE_TEXT: Record<number, string> = {
  1: '本地创建',
  2: 'ERP 读取',
}
export const ROUTE_SOURCE_OPTIONS: OptionItem[] = Object.entries(ROUTE_SOURCE_TEXT).map(
  ([value, label]) => ({ label, value: Number(value) }),
)

/** 工序变更类型（CraftProcessChangeTypeEnum） */
export const PROCESS_CHANGE_TYPE_TEXT: Record<number, string> = {
  1: '创建',
  2: '修改',
  3: '启停',
  4: '删除',
  5: 'SOP 变更',
  6: '不良原因变更',
}

/** 路线变更类型（CraftRouteChangeTypeEnum） */
export const ROUTE_CHANGE_TYPE_TEXT: Record<number, string> = {
  1: '创建',
  2: '修改草稿',
  3: '审核生效',
  4: '停用',
  5: '删除草稿',
  6: '创建新版本',
}

/** 布尔筛选项：后端 @RequestParam Boolean 由 Spring 从字符串转换 */
export const BOOL_OPTIONS: OptionItem[] = [
  { label: '是', value: 'true' },
  { label: '否', value: 'false' },
]
