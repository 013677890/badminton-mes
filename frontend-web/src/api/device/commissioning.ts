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
  return get(`${BASE_URL}/page`, params)
}

export function getDeviceCommissioningRecord(id: number): Promise<DeviceCommissioningRecord> {
  return get(`${BASE_URL}/${id}`)
}

export function createDeviceCommissioningRecord(data: DeviceCommissioningSaveReq): Promise<number> {
  return post(BASE_URL, data)
}
