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
  // 后端按编码、名称、车间和状态进行分页过滤。
  return get('/production/production_lines/page', params)
}

export function getLine(id: number): Promise<ProductionLine> {
  // 查询产线详情并回填所属车间展示信息。
  return get(`/production/production_lines/${id}`)
}

export function createLine(data: LineSaveParams): Promise<number> {
  // 创建前端产线表单对应的组织主档记录。
  return post('/production/production_lines', data)
}

export function updateLine(id: number, data: LineSaveParams & { version: number }): Promise<void> {
  // 版本字段用于后端乐观锁，所属车间是否可变由后端业务校验。
  return put(`/production/production_lines/${id}`, data)
}

export function deleteLine(id: number, version: number): Promise<void> {
  // 仅无历史引用的产线允许逻辑删除，版本参数防止并发误删。
  return del(`/production/production_lines/${id}`, { version })
}

export function updateLineStatus(id: number, status: number, version: number): Promise<void> {
  // 状态切换由后端检查车间状态和活动派工/用户引用。
  return put(`/production/production_lines/${id}/status`, { status, version })
}
