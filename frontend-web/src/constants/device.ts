import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from '@/constants/production'

export const DEVICE_VIEW_ROLES = [ROLES.ADMIN, ROLES.PMC, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER, ROLES.OPERATOR]
export const DEVICE_CONFIG_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]
export const DEVICE_REPORT_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER, ROLES.OPERATOR]
export const DEVICE_EXCEPTION_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER]

export const DEVICE_COMMISSIONING_STATUS_MAP: StatusMap = {
  NOT_TESTED: { type: 'info', text: '未联调' },
  PASSED: { type: 'success', text: '已通过' },
  FAILED: { type: 'danger', text: '未通过' },
}
export const DEVICE_COMMISSIONING_STATUS_OPTIONS: OptionItem[] = [
  { label: '未联调', value: 'NOT_TESTED' },
  { label: '已通过', value: 'PASSED' },
  { label: '未通过', value: 'FAILED' },
]

export const CHECK_RESULT_OPTIONS: OptionItem[] = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
]
export const COMMISSIONING_RESULT_OPTIONS: OptionItem[] = [
  { label: '通过', value: 'PASSED' },
  { label: '失败', value: 'FAILED' },
]

export const COUNT_MODE_OPTIONS: OptionItem[] = [
  { label: '累计值', value: 'CUMULATIVE' },
  { label: '增量值', value: 'INCREMENTAL' },
]
export const REPORT_MODE_OPTIONS: OptionItem[] = [
  { label: '自动报工', value: 'AUTO' },
  { label: '待确认报工', value: 'PENDING_CONFIRMATION' },
  { label: '仅采集', value: 'NONE' },
]

export const MATCH_STATUS_MAP: StatusMap = {
  PENDING: { type: 'warning', text: '待匹配' },
  MATCHED: { type: 'success', text: '已匹配' },
  EXCEPTION: { type: 'danger', text: '异常' },
}
export const MATCH_STATUS_OPTIONS: OptionItem[] = Object.entries(MATCH_STATUS_MAP).map(
  ([value, meta]) => ({ label: meta.text, value }),
)
export const REPORT_STATUS_MAP: StatusMap = {
  NOT_CREATED: { type: 'info', text: '未生成' },
  AUTO_REPORTED: { type: 'success', text: '已自动报工' },
  PENDING_CONFIRMATION: { type: 'warning', text: '待确认' },
}
export const RUNTIME_STATUS_OPTIONS: OptionItem[] = [
  { label: '空闲', value: 'IDLE' },
  { label: '运行', value: 'RUNNING' },
  { label: '停机', value: 'STOPPED' },
]
export const COUNT_EXCEPTION_STATUS_MAP: StatusMap = {
  PENDING: { type: 'warning', text: '待处理' },
  RESOLVED: { type: 'success', text: '已解决' },
  IGNORED: { type: 'info', text: '已忽略' },
}
export const COUNT_EXCEPTION_STATUS_OPTIONS: OptionItem[] = Object.entries(
  COUNT_EXCEPTION_STATUS_MAP,
).map(([value, meta]) => ({ label: meta.text, value }))

export const COUNT_EXCEPTION_TYPE_TEXT: Record<string, string> = {
  EQUIPMENT_DISABLED: '设备已停用',
  EQUIPMENT_STATUS_ABNORMAL: '设备状态异常',
  PROCESS_NOT_CONFIGURED: '未配置工序',
  COUNT_ROLLBACK: '计数回退',
  COUNT_SPIKE: '计数跳变',
}
