import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 设备故障原理接口，对齐后端 EquipmentFaultPrincipleController（/api/equipment/fault-principles）。 */

export type EquipmentFaultLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface EquipmentFaultPrinciple {
  id: number
  faultCode: string
  faultName: string
  categoryId: number | null
  faultLevel: EquipmentFaultLevel
  faultDescription: string | null
  suggestedSolution: string | null
  sortOrder: number
  remark: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface EquipmentFaultPrincipleSaveParams {
  faultCode: string
  faultName: string
  categoryId?: number | null
  faultLevel?: EquipmentFaultLevel | null
  faultDescription?: string | null
  suggestedSolution?: string | null
  sortOrder?: number | null
  remark?: string | null
  status?: number | null
}

export interface EquipmentFaultPrinciplePageParams {
  keyword?: string
  categoryId?: number
  faultLevel?: EquipmentFaultLevel
  status?: number
}

/** 分页查询设备故障原理。 */
export function getEquipmentFaultPrinciplePage(
  params: EquipmentFaultPrinciplePageParams & PageParam,
): Promise<PageResult<EquipmentFaultPrinciple>> {
  return get('/equipment/fault-principles/page', params)
}

/** 查询设备故障原理详情。 */
export function getEquipmentFaultPrinciple(id: number): Promise<EquipmentFaultPrinciple> {
  return get(`/equipment/fault-principles/${id}`)
}

/** 创建设备故障原理。 */
export function createEquipmentFaultPrinciple(
  data: EquipmentFaultPrincipleSaveParams,
): Promise<number> {
  return post('/equipment/fault-principles', data)
}

/** 修改设备故障原理。 */
export function updateEquipmentFaultPrinciple(
  id: number,
  data: EquipmentFaultPrincipleSaveParams,
): Promise<void> {
  return put(`/equipment/fault-principles/${id}`, data)
}

/** 删除设备故障原理。 */
export function deleteEquipmentFaultPrinciple(id: number): Promise<void> {
  return del(`/equipment/fault-principles/${id}`)
}
