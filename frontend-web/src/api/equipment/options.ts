import type { OptionItem } from '@/types/components'
import { getEquipmentCategoryPage } from './category'
import { getEquipmentFaultPrinciplePage } from './faultPrinciple'
import { getEquipmentLedgerPage } from './ledger'
import { getEquipmentMaintenancePlanPage } from './maintenancePlan'
import { getEquipmentManufacturerPage } from './manufacturer'

/** 主档量级较小，一次加载 100 条启用数据并映射为通用下拉选项。 */
const FULL_PAGE = { pageNo: 1, pageSize: 100 }

/** 加载启用的设备类别选项。 */
export async function loadEquipmentCategoryOptions(): Promise<OptionItem[]> {
  const page = await getEquipmentCategoryPage({ ...FULL_PAGE, status: 1 })
  return page.list.map((item) => ({
    label: `${item.categoryCode} ${item.categoryName}`,
    value: item.id,
  }))
}

/** 加载启用的设备制造商选项。 */
export async function loadEquipmentManufacturerOptions(): Promise<OptionItem[]> {
  const page = await getEquipmentManufacturerPage({ ...FULL_PAGE, status: 1 })
  return page.list.map((item) => ({
    label: `${item.manufacturerCode} ${item.manufacturerName}`,
    value: item.id,
  }))
}

/** 加载启用的故障原理选项，可按设备类别过滤。 */
export async function loadEquipmentFaultPrincipleOptions(
  categoryId?: number,
): Promise<OptionItem[]> {
  const page = await getEquipmentFaultPrinciplePage({ ...FULL_PAGE, categoryId, status: 1 })
  return page.list.map((item) => ({
    label: `${item.faultCode} ${item.faultName}`,
    value: item.id,
  }))
}

/** 加载启用的设备台账选项，可按设备类别过滤。 */
export async function loadEquipmentLedgerOptions(categoryId?: number): Promise<OptionItem[]> {
  const page = await getEquipmentLedgerPage({ ...FULL_PAGE, categoryId, status: 1 })
  return page.list.map((item) => ({
    label: `${item.equipmentCode} ${item.equipmentName}`,
    value: item.id,
  }))
}

/** 加载启用的保养计划选项，可按设备过滤。 */
export async function loadEquipmentMaintenancePlanOptions(
  equipmentId?: number,
): Promise<OptionItem[]> {
  const page = await getEquipmentMaintenancePlanPage({ ...FULL_PAGE, equipmentId, status: 1 })
  return page.list.map((item) => ({
    label: `${item.planCode} ${item.planName}`,
    value: item.id,
  }))
}
