import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface AndonType {
  id: number
  typeCode: string
  typeName: string
  exceptionCategory: string
  handlingMode: string
  responseMinutes: number | null
  responsibleRoleCode: string | null
  notificationChannels: string | null
  lightControlEnabled: boolean
  enabledStatus: number
  remark: string | null
  createTime: string
  updateTime: string
}

export interface AndonTypePageParams {
  keyword?: string
  exceptionCategory?: string
  handlingMode?: string
  enabledStatus?: number
}

export type AndonTypeSaveReq = Omit<AndonType, 'id' | 'createTime' | 'updateTime'>

const BASE_URL = '/andon/types'

export function getAndonTypePage(
  params: AndonTypePageParams & PageParam,
): Promise<PageResult<AndonType>> {
  return get(`${BASE_URL}/page`, params)
}

export function getAndonType(id: number): Promise<AndonType> {
  return get(`${BASE_URL}/${id}`)
}

export function createAndonType(data: AndonTypeSaveReq): Promise<number> {
  return post(BASE_URL, data)
}

export function updateAndonType(id: number, data: AndonTypeSaveReq): Promise<void> {
  return put(`${BASE_URL}/${id}`, data)
}

export function deleteAndonType(id: number): Promise<void> {
  return del(`${BASE_URL}/${id}`)
}

export async function loadAndonTypeOptions() {
  const page = await getAndonTypePage({ pageNo: 1, pageSize: 100, enabledStatus: 1 })
  return page.list.map((item) => ({ label: `${item.typeCode} ${item.typeName}`, value: item.id }))
}
