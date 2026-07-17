import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface DeviceAccessConfig {
  id: number
  configCode: string
  configName: string
  equipmentId: number
  collectionPointCode: string
  processId: number | null
  productionLineId: number | null
  dataSource: string
  countMode: string
  spikeThreshold: number | null
  reportMode: string
  commissioningStatus: string
  enabledStatus: number
  lastCommunicationTime: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface DeviceAccessConfigPageParams {
  keyword?: string
  equipmentId?: number
  processId?: number
  commissioningStatus?: string
  enabledStatus?: number
}

export interface DeviceAccessConfigSaveReq {
  configCode: string
  configName: string
  equipmentId: number
  collectionPointCode: string
  processId?: number
  productionLineId?: number
  countMode?: string
  spikeThreshold?: number
  reportMode?: string
  enabledStatus?: number
  remark?: string
}

const BASE_URL = '/device/access-configs'

export function getDeviceAccessConfigPage(
  params: DeviceAccessConfigPageParams & PageParam,
): Promise<PageResult<DeviceAccessConfig>> {
  return get(`${BASE_URL}/page`, params)
}

export function getDeviceAccessConfig(id: number): Promise<DeviceAccessConfig> {
  return get(`${BASE_URL}/${id}`)
}

export function createDeviceAccessConfig(data: DeviceAccessConfigSaveReq): Promise<number> {
  return post(BASE_URL, data)
}

export function updateDeviceAccessConfig(id: number, data: DeviceAccessConfigSaveReq): Promise<void> {
  return put(`${BASE_URL}/${id}`, data)
}

export function deleteDeviceAccessConfig(id: number): Promise<void> {
  return del(`${BASE_URL}/${id}`)
}

export async function loadDeviceAccessConfigOptions() {
  const page = await getDeviceAccessConfigPage({ pageNo: 1, pageSize: 100 })
  return page.list.map((item) => ({
    label: `${item.configCode} ${item.configName}`,
    value: item.id,
  }))
}
