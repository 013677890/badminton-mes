import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface AndonReason {
  id: number
  reasonCode: string
  reasonName: string
  andonTypeId: number
  andonTypeCode: string
  andonTypeName: string
  reasonDescription: string | null
  enabledStatus: number
  createTime: string
  updateTime: string
}

export interface AndonReasonPageParams {
  keyword?: string
  andonTypeId?: number
  enabledStatus?: number
}

export interface AndonReasonSaveReq {
  reasonCode: string
  reasonName: string
  andonTypeId: number
  reasonDescription?: string
  enabledStatus?: number
}

const BASE_URL = '/andon/reasons'

export function getAndonReasonPage(
  params: AndonReasonPageParams & PageParam,
): Promise<PageResult<AndonReason>> {
  return get(`${BASE_URL}/page`, params)
}

export function getAndonReason(id: number): Promise<AndonReason> {
  return get(`${BASE_URL}/${id}`)
}

export function createAndonReason(data: AndonReasonSaveReq): Promise<number> {
  return post(BASE_URL, data)
}

export function updateAndonReason(id: number, data: AndonReasonSaveReq): Promise<void> {
  return put(`${BASE_URL}/${id}`, data)
}

export function deleteAndonReason(id: number): Promise<void> {
  return del(`${BASE_URL}/${id}`)
}

export async function loadAndonReasonOptions(andonTypeId?: number) {
  const page = await getAndonReasonPage({ pageNo: 1, pageSize: 100, enabledStatus: 1, andonTypeId })
  return page.list.map((item) => ({ label: `${item.reasonCode} ${item.reasonName}`, value: item.id }))
}
