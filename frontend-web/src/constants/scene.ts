import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from '@/constants/production'

export const SCENE_VIEW_ROLES = Object.values(ROLES)
export const SCENE_TASK_PLAN_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.WORKSHOP_MANAGER]
export const SCENE_TASK_EXEC_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER]
export const SCENE_OPERATOR_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER, ROLES.OPERATOR]
export const SCENE_MANAGE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

export const SCENE_TASK_STATUS_MAP: StatusMap = {
  0: { type: 'warning', text: '待审核' }, 1: { type: 'primary', text: '已审核' },
  2: { type: 'primary', text: '已下发' }, 3: { type: 'success', text: '生产中' },
  4: { type: 'warning', text: '已暂停' }, 5: { type: 'success', text: '已完工' },
  6: { type: 'info', text: '已关闭' }, 7: { type: 'danger', text: '已取消' },
}
export const SCENE_TASK_STATUS_OPTIONS: OptionItem[] = Object.entries(SCENE_TASK_STATUS_MAP).map(([value, meta]) => ({ label: meta.text, value: Number(value) }))
export const SCENE_DISPATCH_STATUS_MAP: StatusMap = {
  0: { type: 'warning', text: '待确认' }, 1: { type: 'primary', text: '已确认' },
  2: { type: 'success', text: '执行中' }, 3: { type: 'success', text: '已完成' },
  4: { type: 'danger', text: '已取消' },
}
export const SCENE_OPERATION_STATUS_MAP: StatusMap = {
  0: { type: 'info', text: '待作业' }, 1: { type: 'primary', text: '作业中' },
  2: { type: 'success', text: '已完成' }, 3: { type: 'danger', text: '异常' },
}
export const SCENE_BATCH_STATUS_MAP: StatusMap = {
  1: { type: 'primary', text: '生产中' }, 2: { type: 'warning', text: '待检' },
  3: { type: 'warning', text: '返修中' }, 4: { type: 'danger', text: '已隔离' },
  5: { type: 'success', text: '已完工' }, 6: { type: 'info', text: '已报废' },
}
export const SCENE_PARAMETER_TYPE_OPTIONS: OptionItem[] = [
  { label: '开关', value: 1 }, { label: '数量', value: 2 }, { label: '枚举', value: 3 }, { label: '文本', value: 4 },
]
export const REPAIR_RECHECK_OPTIONS: OptionItem[] = [
  { label: '放行', value: 'RELEASED' }, { label: '继续返修', value: 'CONTINUE_REPAIR' }, { label: '报废', value: 'SCRAPPED' },
]
