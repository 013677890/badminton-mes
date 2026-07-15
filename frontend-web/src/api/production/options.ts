import type { OptionItem } from '@/types/components'
import { getRoleUsers } from '@/api/system'
import { BOM_STATUS, ROUTE_STATUS } from '@/constants/production'
import { getBomPage } from './bom'
import { getRoutePage } from './craftRoute'
import { getLinePage } from './line'
import { getMaterialPage } from './material'
import { getProductPage } from './product'
import { getWorkshopPage } from './workshop'

/**
 * 表单下拉选项加载器。
 * 主档量级小（分页上限 100 覆盖演示数据），一次取满映射为 OptionItem；
 * 数据量上来后应换成远程搜索（select filterable remote）。
 */

const FULL_PAGE = { pageNo: 1, pageSize: 100 }

/** 启用产品 → 「编码 名称」 */
export async function loadProductOptions(): Promise<OptionItem[]> {
  const page = await getProductPage({ ...FULL_PAGE, status: 1 })
  return page.list.map((item) => ({
    label: `${item.productCode} ${item.productName}`,
    value: item.id,
  }))
}

/** 启用物料 → 「编码 名称」 */
export async function loadMaterialOptions(): Promise<OptionItem[]> {
  const page = await getMaterialPage({ ...FULL_PAGE, status: 1 })
  return page.list.map((item) => ({
    label: `${item.materialCode} ${item.materialName}`,
    value: item.id,
  }))
}

/** 启用车间 */
export async function loadWorkshopOptions(): Promise<OptionItem[]> {
  const page = await getWorkshopPage({ ...FULL_PAGE, status: 1 })
  return page.list.map((item) => ({
    label: `${item.workshopCode} ${item.workshopName}`,
    value: item.id,
  }))
}

/** 启用产线（可按车间过滤） */
export async function loadLineOptions(workshopId?: number): Promise<OptionItem[]> {
  const page = await getLinePage({ ...FULL_PAGE, status: 1, workshopId })
  return page.list.map((item) => ({
    label: `${item.lineCode} ${item.lineName}`,
    value: item.id,
  }))
}

/** 产品的生效 BOM（工单创建时联动） */
export async function loadEffectiveBomOptions(productId: number): Promise<OptionItem[]> {
  const page = await getBomPage({ ...FULL_PAGE, productId, bomStatus: BOM_STATUS.EFFECTIVE })
  return page.list.map((item) => ({
    label: `${item.bomCode}（${item.version}）`,
    value: item.id,
  }))
}

/** 生效工艺路线 */
export async function loadEffectiveRouteOptions(): Promise<OptionItem[]> {
  const page = await getRoutePage({ ...FULL_PAGE, routingStatus: ROUTE_STATUS.EFFECTIVE })
  return page.list.map((item) => ({
    label: `${item.routingCode} ${item.routingName}（${item.routingVersion}）`,
    value: item.id,
  }))
}

/** 某角色下的用户 → 「工号 姓名」（车间主管、欠料责任人等选人场景） */
export async function loadRoleUserOptions(roleId: number): Promise<OptionItem[]> {
  const users = await getRoleUsers(roleId)
  return users.map((user) => ({
    label: `${user.userNo} ${user.userName}`,
    value: user.userId,
  }))
}
