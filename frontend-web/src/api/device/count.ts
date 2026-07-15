import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface DeviceCountRecord {
  id: number
  accessConfigId: number
  equipmentId: number
  equipmentCode: string
  collectionPointCode: string
  collectedAt: string
  serialNumber: string
  rawCount: number
  incrementCount: number
  runtimeStatus: string | null
  faultStatus: string | null
  productionTaskId: number | null
  processId: number | null
  matchStatus: string
  reportStatus: string
  rawPayload: string | null
  createTime: string
}

export interface DeviceCountRecordPageParams {
  accessConfigId?: number
  equipmentId?: number
  matchStatus?: string
  collectedStartTime?: string
  collectedEndTime?: string
}

export interface DeviceCountReportReq {
  configCode: string
  equipmentCode: string
  collectedAt: string
  serialNumber: string
  countValue: number
  runtimeStatus?: string
  faultStatus?: string
  rawPayload?: string
}

export interface DeviceCountReportResult {
  countRecordId: number
  incrementCount: number
  matchStatus: string
  reportStatus: string
  exceptionType: string | null
  processingMessage: string
}

export interface DeviceCountException {
  id: number
  countRecordId: number
  accessConfigId: number
  equipmentId: number
  exceptionType: string
  exceptionReason: string
  processingStatus: string
  processedBy: number | null
  processedAt: string | null
  processingResult: string | null
  createTime: string
  updateTime: string
}

export interface DeviceCountExceptionPageParams {
  accessConfigId?: number
  equipmentId?: number
  processingStatus?: string
  createStartTime?: string
  createEndTime?: string
}

export function getDeviceCountRecordPage(
  params: DeviceCountRecordPageParams & PageParam,
): Promise<PageResult<DeviceCountRecord>> {
  return get('/device/count-records/page', params)
}

export function getDeviceCountRecord(id: number): Promise<DeviceCountRecord> {
  return get(`/device/count-records/${id}`)
}

export function reportDeviceCount(data: DeviceCountReportReq): Promise<DeviceCountReportResult> {
  return post('/device/count-records/report', data)
}

export function getDeviceCountExceptionPage(
  params: DeviceCountExceptionPageParams & PageParam,
): Promise<PageResult<DeviceCountException>> {
  return get('/device/count-exceptions/page', params)
}

export function getDeviceCountException(id: number): Promise<DeviceCountException> {
  return get(`/device/count-exceptions/${id}`)
}

export function processDeviceCountException(
  id: number,
  data: { processingStatus: 'RESOLVED' | 'IGNORED'; processingResult: string },
): Promise<void> {
  return put(`/device/count-exceptions/${id}/process`, data)
}
