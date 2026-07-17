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
  // 查询设备采集记录分页，采集时间和匹配状态筛选由后端处理。
  return get('/device/count-records/page', params)
}

export function getDeviceCountRecord(id: number): Promise<DeviceCountRecord> {
  // 查询单条采集记录及其匹配、报工处理结果。
  return get(`/device/count-records/${id}`)
}

export function reportDeviceCount(data: DeviceCountReportReq): Promise<DeviceCountReportResult> {
  // 上报接口由后端解析设备绑定、累计值和工单匹配，并返回异常处理状态。
  return post('/device/count-records/report', data)
}

export function getDeviceCountExceptionPage(
  params: DeviceCountExceptionPageParams & PageParam,
): Promise<PageResult<DeviceCountException>> {
  // 按设备、接入配置、处理状态和创建时间查询异常池。
  return get('/device/count-exceptions/page', params)
}

export function getDeviceCountException(id: number): Promise<DeviceCountException> {
  // 读取异常记录详情，供人工处理弹窗回显。
  return get(`/device/count-exceptions/${id}`)
}

export function processDeviceCountException(
  id: number,
  data: { processingStatus: 'RESOLVED' | 'IGNORED'; processingResult: string },
): Promise<void> {
  // 通过异常状态 CAS 完成或忽略异常，避免多人重复处理同一条记录。
  return put(`/device/count-exceptions/${id}/process`, data)
}
