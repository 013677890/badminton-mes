import type { OptionItem, StatusMap } from '@/types/components'
import { ROLES } from '@/constants/production'

export const ANDON_VIEW_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER, ROLES.OPERATOR, ROLES.INSPECTOR]
export const ANDON_MANAGE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]
export const ANDON_CLOSE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER, ROLES.TEAM_LEADER]

export const EXCEPTION_CATEGORY_OPTIONS: OptionItem[] = [
  { label: '生产异常', value: 'PRODUCTION' },
  { label: '设备异常', value: 'EQUIPMENT' },
  { label: '质量异常', value: 'QUALITY' },
  { label: '物料异常', value: 'MATERIAL' },
  { label: '非生产异常', value: 'NON_PRODUCTION' },
]
export const EXCEPTION_CATEGORY_TEXT = Object.fromEntries(EXCEPTION_CATEGORY_OPTIONS.map((item) => [item.value, item.label])) as Record<string, string>

export const HANDLING_MODE_OPTIONS: OptionItem[] = [
  { label: '无需处理', value: 'NO_ACTION' },
  { label: '自行处理', value: 'SELF_HANDLE' },
  { label: '协同处理', value: 'ASSISTANCE' },
]
export const HANDLING_MODE_TEXT = Object.fromEntries(HANDLING_MODE_OPTIONS.map((item) => [item.value, item.label])) as Record<string, string>

export const ROLE_OPTIONS: OptionItem[] = [
  { label: '管理员', value: ROLES.ADMIN },
  { label: 'PMC 计划员', value: ROLES.PMC },
  { label: '车间主管', value: ROLES.WORKSHOP_MANAGER },
  { label: '班组长', value: ROLES.TEAM_LEADER },
  { label: '操作工', value: ROLES.OPERATOR },
  { label: '质检员', value: ROLES.INSPECTOR },
  { label: '工艺工程师', value: ROLES.CRAFT_ENGINEER },
]
export const NOTIFICATION_CHANNEL_OPTIONS: OptionItem[] = [
  { label: '站内信', value: 'IN_APP' },
  { label: '短信', value: 'SMS' },
  { label: '微信', value: 'WECHAT' },
]

export const EVENT_STATUS_MAP: StatusMap = {
  PENDING_CONFIRMATION: { type: 'warning', text: '待确认' },
  CONFIRMED: { type: 'primary', text: '已确认' },
  PROCESSING: { type: 'primary', text: '处理中' },
  WAITING_CLOSE: { type: 'warning', text: '待关闭' },
  CLOSED: { type: 'success', text: '已关闭' },
}
export const EVENT_STATUS_OPTIONS: OptionItem[] = Object.entries(EVENT_STATUS_MAP).map(
  ([value, meta]) => ({ label: meta.text, value }),
)
export const SEVERITY_MAP: StatusMap = {
  NORMAL: { type: 'info', text: '一般' },
  MAJOR: { type: 'warning', text: '重大' },
  CRITICAL: { type: 'danger', text: '紧急' },
}
export const SEVERITY_OPTIONS: OptionItem[] = Object.entries(SEVERITY_MAP).map(
  ([value, meta]) => ({ label: meta.text, value }),
)
export const TIMEOUT_STATUS_MAP: StatusMap = {
  NORMAL: { type: 'success', text: '正常' },
  RESPONSE_OVERDUE: { type: 'warning', text: '响应超时' },
  ESCALATED: { type: 'danger', text: '已升级' },
}
export const SOURCE_CHANNEL_TEXT: Record<string, string> = {
  WEB: '管理端', TABLET: '平板端', MOBILE: '移动端', SYSTEM: '系统',
}
export const SOURCE_CHANNEL_OPTIONS: OptionItem[] = Object.entries(SOURCE_CHANNEL_TEXT).map(
  ([value, label]) => ({ label, value }),
)
export const LIGHT_STATUS_MAP: StatusMap = {
  NOT_REQUIRED: { type: 'info', text: '无需灯控' },
  ON: { type: 'danger', text: '已亮灯' },
  OFF: { type: 'success', text: '已熄灯' },
  FAILED: { type: 'warning', text: '灯控失败' },
}
export const ACTION_TYPE_TEXT: Record<string, string> = {
  INITIATE: '发起', CONFIRM: '确认', START_PROCESS: '开始处理', TRANSFER: '转派',
  COMPLETE: '处理完成', CLOSE: '关闭', ESCALATE: '升级',
}
