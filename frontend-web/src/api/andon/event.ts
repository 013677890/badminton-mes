import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface AndonProcessLog {
  id: number
  actionType: string
  fromStatus: string | null
  toStatus: string
  operatorId: number
  targetUserId: number | null
  targetRoleCode: string | null
  actionContent: string | null
  createTime: string
}

export interface AndonNotificationRecord {
  id: number
  notificationType: string
  channel: string
  receiverUserId: number | null
  receiverRoleCode: string | null
  sendStatus: string
  sendMessage: string | null
  sentAt: string | null
  createTime: string
}

export interface AndonEvent {
  id: number
  eventNo: string
  andonTypeId: number
  andonTypeCode: string
  andonTypeName: string
  reasonId: number | null
  actualReasonId: number | null
  sourceChannel: string
  severity: string
  workshopId: number | null
  productionLineId: number | null
  workOrderId: number | null
  productionTaskId: number | null
  processId: number | null
  equipmentId: number | null
  qualityRecordId: number | null
  batchNo: string | null
  description: string
  attachmentUrls: string | null
  eventStatus: string
  assignedUserId: number | null
  assignedRoleCode: string | null
  responseDeadline: string | null
  escalationDeadline: string | null
  timeoutStatus: string
  lightStatus: string
  lightMessage: string | null
  processingResult: string | null
  impactMinutes: number | null
  affectedQuantity: number | null
  initiatedBy: number
  confirmedBy: number | null
  confirmedAt: string | null
  completedBy: number | null
  completedAt: string | null
  closedBy: number | null
  closedAt: string | null
  createTime: string
  updateTime: string
  processLogs: AndonProcessLog[]
  notificationRecords: AndonNotificationRecord[]
}

export interface AndonEventPageParams {
  keyword?: string
  andonTypeId?: number
  productionLineId?: number
  equipmentId?: number
  initiatedBy?: number
  assignedUserId?: number
  assignedRoleCode?: string
  sourceChannel?: string
  eventStatus?: string
  severity?: string
  timeoutStatus?: string
}

export interface AndonEventCreateReq {
  andonTypeId: number
  reasonId?: number
  sourceChannel: string
  severity?: string
  workshopId?: number
  productionLineId?: number
  workOrderId?: number
  productionTaskId?: number
  processId?: number
  equipmentId?: number
  qualityRecordId?: number
  batchNo?: string
  description: string
  attachmentUrls?: string
}

export interface AndonEventActionReq {
  actualReasonId?: number
  targetUserId?: number
  targetRoleCode?: string
  actionContent?: string
  processingResult?: string
  impactMinutes?: number
  affectedQuantity?: number
}

const BASE_URL = '/andon/events'

export function getAndonEventPage(
  params: AndonEventPageParams & PageParam,
): Promise<PageResult<AndonEvent>> {
  return get(`${BASE_URL}/page`, params)
}

export function getAndonEvent(id: number): Promise<AndonEvent> {
  return get(`${BASE_URL}/${id}`)
}

export function getAndonProcessLogs(id: number): Promise<AndonProcessLog[]> {
  return get(`${BASE_URL}/${id}/process-logs`)
}

export function createAndonEvent(data: AndonEventCreateReq): Promise<number> {
  return post(BASE_URL, data)
}

export function actionAndonEvent(
  id: number,
  action: 'confirm' | 'start-processing' | 'transfer' | 'complete' | 'close' | 'escalate',
  data: AndonEventActionReq,
): Promise<void> {
  return put(`${BASE_URL}/${id}/${action}`, data)
}
