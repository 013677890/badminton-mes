import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 设备保养计划接口，对齐后端 EquipmentMaintenancePlanController（/api/equipment/maintenance-plans）。 */

export type EquipmentMaintenanceType = 'ROUTINE' | 'PREVENTIVE' | 'SPECIAL'

export interface EquipmentMaintenancePlan {
  id: number
  planCode: string
  planName: string
  equipmentId: number
  maintenanceType: EquipmentMaintenanceType | null
  cycleDays: number
  maintenanceContent: string
  responsibleUserId: number | null
  lastMaintenanceTime: string | null
  nextMaintenanceTime: string
  remark: string | null
  status: number | null
  createTime: string
  updateTime: string
}

export interface EquipmentMaintenancePlanSaveParams {
  planCode: string
  planName: string
  equipmentId: number
  maintenanceType?: EquipmentMaintenanceType | null
  cycleDays: number
  maintenanceContent: string
  responsibleUserId?: number | null
  nextMaintenanceTime: string
  remark?: string | null
  status?: number | null
}

export interface EquipmentMaintenancePlanPageParams {
  keyword?: string
  equipmentId?: number
  maintenanceType?: EquipmentMaintenanceType
  status?: number
  nextMaintenanceStartTime?: string
  nextMaintenanceEndTime?: string
}

/** 分页查询设备保养计划。 */
export function getEquipmentMaintenancePlanPage(
  params: EquipmentMaintenancePlanPageParams & PageParam,
): Promise<PageResult<EquipmentMaintenancePlan>> {
  return get('/equipment/maintenance-plans/page', params)
}

/** 查询设备保养计划详情。 */
export function getEquipmentMaintenancePlan(id: number): Promise<EquipmentMaintenancePlan> {
  return get(`/equipment/maintenance-plans/${id}`)
}

/** 创建设备保养计划。 */
export function createEquipmentMaintenancePlan(
  data: EquipmentMaintenancePlanSaveParams,
): Promise<number> {
  return post('/equipment/maintenance-plans', data)
}

/** 修改设备保养计划。 */
export function updateEquipmentMaintenancePlan(
  id: number,
  data: EquipmentMaintenancePlanSaveParams,
): Promise<void> {
  return put(`/equipment/maintenance-plans/${id}`, data)
}

/** 删除设备保养计划。 */
export function deleteEquipmentMaintenancePlan(id: number): Promise<void> {
  return del(`/equipment/maintenance-plans/${id}`)
}
