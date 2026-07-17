import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface AndonConfiguration {
  id: number
  andonTypeId: number
  andonTypeCode: string
  andonTypeName: string
  productionLineId: number | null
  handlerUserId: number | null
  handlerRoleCode: string | null
  escalationUserId: number | null
  escalationRoleCode: string | null
  responseMinutes: number
  escalationMinutes: number | null
  notificationChannels: string
  enabledStatus: number
  remark: string | null
  createTime: string
  updateTime: string
}

export interface AndonConfigurationPageParams {
  andonTypeId?: number
  productionLineId?: number
  handlerUserId?: number
  enabledStatus?: number
}

export interface AndonConfigurationSaveReq {
  andonTypeId: number
  productionLineId?: number
  handlerUserId?: number
  handlerRoleCode?: string
  escalationUserId?: number
  escalationRoleCode?: string
  responseMinutes: number
  escalationMinutes?: number
  notificationChannels: string
  enabledStatus?: number
  remark?: string
}

const BASE_URL = '/andon/configurations'

export function getAndonConfigurationPage(
  params: AndonConfigurationPageParams & PageParam,
): Promise<PageResult<AndonConfiguration>> {
  // 将分页和配置筛选条件交给后端动态查询，响应由 request 工具解包。
  return get(`${BASE_URL}/page`, params)
}

export function getAndonConfiguration(id: number): Promise<AndonConfiguration> {
  // 查询单条配置详情，包含处理人、升级人和通知通道设置。
  return get(`${BASE_URL}/${id}`)
}

export function createAndonConfiguration(data: AndonConfigurationSaveReq): Promise<number> {
  // 创建配置后返回数据库主键，配置状态和关联校验由后端执行。
  return post(BASE_URL, data)
}

export function updateAndonConfiguration(
  id: number,
  data: AndonConfigurationSaveReq,
): Promise<void> {
  // 更新配置时由后端重新校验响应时长、通知通道及关联对象。
  return put(`${BASE_URL}/${id}`, data)
}

export function deleteAndonConfiguration(id: number): Promise<void> {
  // 删除动作由后端执行逻辑删除并检查是否存在进行中的安灯事件。
  return del(`${BASE_URL}/${id}`)
}
