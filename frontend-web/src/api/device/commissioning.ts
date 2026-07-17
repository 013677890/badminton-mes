import { get, post } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface DeviceCommissioningRecord {
  id: number
  accessConfigId: number
  testTime: string
  testerUserId: number
  communicationResult: string
  dataFormatResult: string
  testResult: string
  issueDescription: string | null
  samplePayload: string | null
  createTime: string
}

export interface DeviceCommissioningPageParams {
  accessConfigId?: number
  testResult?: string
  testStartTime?: string
  testEndTime?: string
}

export interface DeviceCommissioningSaveReq {
  accessConfigId: number
  testTime: string
  communicationResult: string
  dataFormatResult: string
  testResult: string
  issueDescription?: string
  samplePayload?: string
}

const BASE_URL = '/device/commissioning-records'

export function getDeviceCommissioningPage(
  params: DeviceCommissioningPageParams & PageParam,
): Promise<PageResult<DeviceCommissioningRecord>> {
  // 查询联调记录分页，按接入配置、测试结果和测试时间过滤。
  return get(`${BASE_URL}/page`, params)
}

export function getDeviceCommissioningRecord(id: number): Promise<DeviceCommissioningRecord> {
  // 读取单条联调记录及问题描述、样例报文。
  return get(`${BASE_URL}/${id}`)
}

export function createDeviceCommissioningRecord(data: DeviceCommissioningSaveReq): Promise<number> {
  // 提交通信、格式和总体结果，后端负责写入联调审计记录并更新配置状态。
  return post(BASE_URL, data)
}
