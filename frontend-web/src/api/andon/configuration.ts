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
  return get(`${BASE_URL}/page`, params)
}

export function getAndonConfiguration(id: number): Promise<AndonConfiguration> {
  return get(`${BASE_URL}/${id}`)
}

export function createAndonConfiguration(data: AndonConfigurationSaveReq): Promise<number> {
  return post(BASE_URL, data)
}

export function updateAndonConfiguration(
  id: number,
  data: AndonConfigurationSaveReq,
): Promise<void> {
  return put(`${BASE_URL}/${id}`, data)
}

export function deleteAndonConfiguration(id: number): Promise<void> {
  return del(`${BASE_URL}/${id}`)
}
