import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 车间基础资料接口，对齐后端 WorkshopController（/api/production/workshops） */

export interface Workshop {
  id: number
  workshopCode: string
  workshopName: string
  managerId: number | null
  managerName: string | null
  status: number
  version: number
  createTime: string
  updateTime: string
}

export interface WorkshopSaveParams {
  workshopCode: string
  workshopName: string
  managerId?: number | null
  status: number
}

export interface WorkshopPageParams {
  workshopCode?: string
  workshopName?: string
  managerId?: number
  status?: number
}

export function getWorkshopPage(
  params: WorkshopPageParams & PageParam,
): Promise<PageResult<Workshop>> {
  return get('/production/workshops/page', params)
}

export function getWorkshop(id: number): Promise<Workshop> {
  return get(`/production/workshops/${id}`)
}

export function createWorkshop(data: WorkshopSaveParams): Promise<number> {
  return post('/production/workshops', data)
}

export function updateWorkshop(
  id: number,
  data: WorkshopSaveParams & { version: number },
): Promise<void> {
  return put(`/production/workshops/${id}`, data)
}

/** 仅无产线/工单引用的车间可删，被引用时后端报业务错误 */
export function deleteWorkshop(id: number, version: number): Promise<void> {
  return del(`/production/workshops/${id}`, { version })
}

export function updateWorkshopStatus(id: number, status: number, version: number): Promise<void> {
  return put(`/production/workshops/${id}/status`, { status, version })
}
