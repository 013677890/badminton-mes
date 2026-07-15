import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 设备保养记录接口，对齐后端 EquipmentMaintenanceRecordController（/api/equipment/maintenance-records）。 */

export type EquipmentMaintenanceResult = 'NORMAL' | 'ABNORMAL'
export type EquipmentMaintenanceRecordStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'

export interface EquipmentMaintenanceRecord {
  id: number
  recordNo: string | null
  planId: number
  equipmentId: number
  scheduledTime: string
  startTime: string | null
  finishTime: string | null
  executorUserId: number | null
  maintenanceContent: string
  maintenanceResult: EquipmentMaintenanceResult | null
  recordStatus: EquipmentMaintenanceRecordStatus | null
  abnormalDescription: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface EquipmentMaintenanceRecordSaveParams {
  recordNo?: string | null
  planId: number
  scheduledTime: string
  startTime?: string | null
  finishTime?: string | null
  executorUserId?: number | null
  maintenanceContent: string
  maintenanceResult?: EquipmentMaintenanceResult | null
  recordStatus?: EquipmentMaintenanceRecordStatus | null
  abnormalDescription?: string | null
  remark?: string | null
}

export interface EquipmentMaintenanceRecordPageParams {
  keyword?: string
  planId?: number
  equipmentId?: number
  recordStatus?: EquipmentMaintenanceRecordStatus
  maintenanceResult?: EquipmentMaintenanceResult
  scheduledStartTime?: string
  scheduledEndTime?: string
}

/** 分页查询设备保养记录。 */
export function getEquipmentMaintenanceRecordPage(
  params: EquipmentMaintenanceRecordPageParams & PageParam,
): Promise<PageResult<EquipmentMaintenanceRecord>> {
  return get('/equipment/maintenance-records/page', params)
}

/** 查询设备保养记录详情。 */
export function getEquipmentMaintenanceRecord(id: number): Promise<EquipmentMaintenanceRecord> {
  return get(`/equipment/maintenance-records/${id}`)
}

/** 创建设备保养记录。 */
export function createEquipmentMaintenanceRecord(
  data: EquipmentMaintenanceRecordSaveParams,
): Promise<number> {
  return post('/equipment/maintenance-records', data)
}

/** 修改设备保养记录。 */
export function updateEquipmentMaintenanceRecord(
  id: number,
  data: EquipmentMaintenanceRecordSaveParams,
): Promise<void> {
  return put(`/equipment/maintenance-records/${id}`, data)
}

/** 删除设备保养记录。 */
export function deleteEquipmentMaintenanceRecord(id: number): Promise<void> {
  return del(`/equipment/maintenance-records/${id}`)
}
