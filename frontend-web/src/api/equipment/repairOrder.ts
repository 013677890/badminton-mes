import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 设备报修任务接口，对齐后端 EquipmentRepairOrderController（/api/equipment/repair-orders）。 */

export type EquipmentRepairStatus =
  | 'REPORTED'
  | 'ASSIGNED'
  | 'REPAIRING'
  | 'FINISHED'
  | 'CANCELLED'

export interface EquipmentRepairOrder {
  id: number
  repairNo: string
  equipmentId: number
  faultPrincipleId: number | null
  faultDescription: string
  reportTime: string
  reportUserId: number
  repairUserId: number | null
  repairStartTime: string | null
  repairEndTime: string | null
  repairResult: string | null
  repairStatus: EquipmentRepairStatus
  remark: string | null
  createTime: string
  updateTime: string
}

export interface EquipmentRepairOrderSaveParams {
  repairNo?: string | null
  equipmentId: number
  faultPrincipleId?: number | null
  faultDescription: string
  reportTime?: string | null
  reportUserId?: number | null
  repairUserId?: number | null
  repairStartTime?: string | null
  repairEndTime?: string | null
  repairResult?: string | null
  repairStatus?: EquipmentRepairStatus | null
  remark?: string | null
}

export interface EquipmentRepairOrderPageParams {
  keyword?: string
  equipmentId?: number
  faultPrincipleId?: number
  repairStatus?: EquipmentRepairStatus
  reportStartTime?: string
  reportEndTime?: string
}

/** 分页查询设备报修任务。 */
export function getEquipmentRepairOrderPage(
  params: EquipmentRepairOrderPageParams & PageParam,
): Promise<PageResult<EquipmentRepairOrder>> {
  return get('/equipment/repair-orders/page', params)
}

/** 查询设备报修任务详情。 */
export function getEquipmentRepairOrder(id: number): Promise<EquipmentRepairOrder> {
  return get(`/equipment/repair-orders/${id}`)
}

/** 创建设备报修任务。 */
export function createEquipmentRepairOrder(
  data: EquipmentRepairOrderSaveParams,
): Promise<number> {
  return post('/equipment/repair-orders', data)
}

/** 修改设备报修任务。 */
export function updateEquipmentRepairOrder(
  id: number,
  data: EquipmentRepairOrderSaveParams,
): Promise<void> {
  return put(`/equipment/repair-orders/${id}`, data)
}

/** 删除设备报修任务。 */
export function deleteEquipmentRepairOrder(id: number): Promise<void> {
  return del(`/equipment/repair-orders/${id}`)
}
