import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 产线基础资料接口，对齐后端 ProductionLineController（/api/production/production_lines） */

export interface ProductionLine {
  id: number
  lineCode: string
  lineName: string
  workshopId: number
  workshopCode: string | null
  workshopName: string | null
  /** 标准日产能（件/天），排产建议用 */
  standardCapacity: number | null
  status: number
  version: number
  createTime: string
  updateTime: string
}

export interface LineSaveParams {
  lineCode: string
  lineName: string
  workshopId: number
  standardCapacity?: number | null
  status: number
}

export interface LinePageParams {
  lineCode?: string
  lineName?: string
  workshopId?: number
  status?: number
}

export function getLinePage(params: LinePageParams & PageParam): Promise<PageResult<ProductionLine>> {
  return get('/production/production_lines/page', params)
}

export function getLine(id: number): Promise<ProductionLine> {
  return get(`/production/production_lines/${id}`)
}

export function createLine(data: LineSaveParams): Promise<number> {
  return post('/production/production_lines', data)
}

export function updateLine(id: number, data: LineSaveParams & { version: number }): Promise<void> {
  return put(`/production/production_lines/${id}`, data)
}

export function deleteLine(id: number, version: number): Promise<void> {
  return del(`/production/production_lines/${id}`, { version })
}

export function updateLineStatus(id: number, status: number, version: number): Promise<void> {
  return put(`/production/production_lines/${id}/status`, { status, version })
}
