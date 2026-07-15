import { get } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 工艺路线只读接口，对齐后端 CraftRouteController（/api/craft/routes）。
 * 生产模块只消费查询（工单创建时选路线），维护入口在工艺模块。
 */

export interface CraftRouteStep {
  id: number
  sequenceNo: number
  processId: number
  processCode: string
  processName: string
  stationId: number | null
  equipmentCategoryId: number | null
  keyProcess: boolean
  inspectNode: boolean
  scanRequired: boolean
  pieceRateEnabled: boolean
  sopId: number | null
  qualityPlanId: number | null
}

export interface CraftRouteProduct {
  productId: number
  productCode?: string
  productName?: string
}

export interface CraftRoute {
  id: number
  routingCode: string
  routingName: string
  routingVersion: string
  previousRouteId: number | null
  /** 1 手工 2 同步 */
  sourceType: number
  /** 0 草稿 1 生效 2 停用 */
  routingStatus: number
  auditBy: number | null
  auditTime: string | null
  version: number
  products: CraftRouteProduct[]
  steps: CraftRouteStep[]
  createTime: string
  updateTime: string
}

export interface CraftRoutePageParams {
  routingCode?: string
  routingName?: string
  routingVersion?: string
  sourceType?: number
  routingStatus?: number
}

export function getRoutePage(
  params: CraftRoutePageParams & PageParam,
): Promise<PageResult<CraftRoute>> {
  return get('/craft/routes/page', params)
}

export function getRoute(id: number): Promise<CraftRoute> {
  return get(`/craft/routes/${id}`)
}

/** 产品默认生效路线；未配置时后端报业务错误，调用方需捕获降级 */
export function getDefaultRoute(productId: number): Promise<CraftRoute> {
  return get('/craft/routes/default', { productId })
}
