import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 设备制造商接口，对齐后端 EquipmentManufacturerController（/api/equipment/manufacturers）。 */

export interface EquipmentManufacturer {
  id: number
  manufacturerCode: string
  manufacturerName: string
  contactPerson: string | null
  contactPhone: string | null
  contactEmail: string | null
  address: string | null
  website: string | null
  remark: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface EquipmentManufacturerSaveParams {
  manufacturerCode: string
  manufacturerName: string
  contactPerson?: string | null
  contactPhone?: string | null
  contactEmail?: string | null
  address?: string | null
  website?: string | null
  remark?: string | null
  status?: number | null
}

export interface EquipmentManufacturerPageParams extends PageParam {
  keyword?: string
  status?: number
}

/** 分页查询设备制造商。 */
export function getEquipmentManufacturerPage(
  params: EquipmentManufacturerPageParams,
): Promise<PageResult<EquipmentManufacturer>> {
  return get('/equipment/manufacturers/page', params)
}

/** 查询设备制造商详情。 */
export function getEquipmentManufacturer(id: number): Promise<EquipmentManufacturer> {
  return get(`/equipment/manufacturers/${id}`)
}

/** 创建设备制造商。 */
export function createEquipmentManufacturer(
  data: EquipmentManufacturerSaveParams,
): Promise<number> {
  return post('/equipment/manufacturers', data)
}

/** 修改设备制造商。 */
export function updateEquipmentManufacturer(
  id: number,
  data: EquipmentManufacturerSaveParams,
): Promise<void> {
  return put(`/equipment/manufacturers/${id}`, data)
}

/** 删除设备制造商。 */
export function deleteEquipmentManufacturer(id: number): Promise<void> {
  return del(`/equipment/manufacturers/${id}`)
}
