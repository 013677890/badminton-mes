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
  // 查询设备接入配置分页，关键词和联调/启停状态由后端过滤。
  return get(`${BASE_URL}/page`, params)
}

export function getDeviceAccessConfig(id: number): Promise<DeviceAccessConfig> {
  // 读取接入配置详情，供编辑和联调页面回显。
  return get(`${BASE_URL}/${id}`)
}

export function createDeviceAccessConfig(data: DeviceAccessConfigSaveReq): Promise<number> {
  // 创建配置时后端校验设备、采集点、计数模式和报工模式的组合规则。
  return post(BASE_URL, data)
}

export function updateDeviceAccessConfig(id: number, data: DeviceAccessConfigSaveReq): Promise<void> {
  // 更新接入配置；设备已产生采集记录时，关键采集语义由后端限制变更范围。
  return put(`${BASE_URL}/${id}`, data)
}

export function deleteDeviceAccessConfig(id: number): Promise<void> {
  // 删除前端只发起请求，是否存在采集记录等引用由后端决定能否逻辑删除。
  return del(`${BASE_URL}/${id}`)
}

export async function loadDeviceAccessConfigOptions() {
  // 选项场景只读取前 100 条启用配置，转换为下拉控件需要的 label/value 结构。
  const page = await getDeviceAccessConfigPage({ pageNo: 1, pageSize: 100 })
  return page.list.map((item) => ({
    label: `${item.configCode} ${item.configName}`,
    value: item.id,
  }))
}
