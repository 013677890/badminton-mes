import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 设备台账接口，对齐后端 EquipmentLedgerController（/api/equipment/ledgers）。 */

export type EquipmentStatus =
  | 'IDLE'
  | 'RUNNING'
  | 'STOPPED'
  | 'REPAIRING'
  | 'MAINTAINING'
  | 'SCRAPPED'

export interface EquipmentLedger {
  id: number
  equipmentCode: string
  equipmentName: string
  categoryId: number
  manufacturerId: number | null
  equipmentModel: string | null
  serialNumber: string | null
  workshopId: number | null
  productionLineId: number | null
  installationLocation: string | null
  purchaseDate: string | null
  commissioningDate: string | null
  equipmentStatus: EquipmentStatus
  responsiblePerson: string | null
  remark: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface EquipmentLedgerSaveParams {
  equipmentCode: string
  equipmentName: string
  categoryId: number
  manufacturerId?: number | null
  equipmentModel?: string | null
  serialNumber?: string | null
  workshopId?: number | null
  productionLineId?: number | null
  installationLocation?: string | null
  purchaseDate?: string | null
  commissioningDate?: string | null
  equipmentStatus?: EquipmentStatus | null
  responsiblePerson?: string | null
  remark?: string | null
  status?: number | null
}

export interface EquipmentLedgerPageParams extends PageParam {
  keyword?: string
  categoryId?: number
  manufacturerId?: number
  equipmentStatus?: EquipmentStatus
  workshopId?: number
  productionLineId?: number
  status?: number
}

/** 分页查询设备台账。 */
export function getEquipmentLedgerPage(
  params: EquipmentLedgerPageParams,
): Promise<PageResult<EquipmentLedger>> {
  return get('/equipment/ledgers/page', params)
}

/** 查询设备台账详情。 */
export function getEquipmentLedger(id: number): Promise<EquipmentLedger> {
  return get(`/equipment/ledgers/${id}`)
}

/** 创建设备台账。 */
export function createEquipmentLedger(data: EquipmentLedgerSaveParams): Promise<number> {
  return post('/equipment/ledgers', data)
}

/** 修改设备台账。 */
export function updateEquipmentLedger(
  id: number,
  data: EquipmentLedgerSaveParams,
): Promise<void> {
  return put(`/equipment/ledgers/${id}`, data)
}

/** 删除设备台账。 */
export function deleteEquipmentLedger(id: number): Promise<void> {
  return del(`/equipment/ledgers/${id}`)
}
