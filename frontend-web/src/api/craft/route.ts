import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 工艺路线聚合接口（完整读写），对齐后端 CraftRouteController（/api/craft/routes）。
 * 生产模块的只读消费（api/production/craftRoute.ts）从这里转发导出。
 * 写操作限 ADMIN / CRAFT_ENGINEER；状态机：草稿 0 → 生效 1 → 停用 2。
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
  /** 是否产品当前默认路线 */
  defaultRoute?: boolean
}

export interface CraftRoute {
  id: number
  routingCode: string
  routingName: string
  routingVersion: string
  previousRouteId: number | null
  /** 1 本地创建 2 ERP 读取确认 */
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

export interface CraftRouteStepSaveReq {
  /** 连续顺序号，从 1 开始 */
  sequenceNo: number
  processId: number
  stationId?: number
  /** 为空继承工序设备类别 */
  equipmentCategoryId?: number
  inspectNode: boolean
  sopId?: number
  qualityPlanId?: number
}

export interface CraftRouteSaveReq {
  routingCode: string
  routingName: string
  routingVersion: string
  sourceType: number
  productIds: number[]
  steps: CraftRouteStepSaveReq[]
  changeReason?: string
}

export interface CraftRouteChangeLog {
  id: number
  routeId: number
  /** 见 constants/craft.ts ROUTE_CHANGE_TYPE_TEXT */
  changeType: number
  beforeSnapshot: string | null
  afterSnapshot: string | null
  changeReason: string | null
  operatorId: number
  createTime: string
}

// ---------- 查询 ----------

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

export function getRouteChangeLogPage(
  id: number,
  params: PageParam,
): Promise<PageResult<CraftRouteChangeLog>> {
  return get(`/craft/routes/${id}/change_logs`, params)
}

// ---------- 写操作 ----------

export function createRoute(data: CraftRouteSaveReq): Promise<number> {
  return post('/craft/routes', data)
}

/** 仅草稿可修改 */
export function updateRoute(
  id: number,
  data: CraftRouteSaveReq & { version: number },
): Promise<void> {
  return put(`/craft/routes/${id}`, data)
}

/** 草稿 → 生效（同产品旧默认路线被替代） */
export function approveRoute(
  id: number,
  data: { version: number; reason: string },
): Promise<void> {
  return put(`/craft/routes/${id}/approve`, data)
}

/** 生效 → 停用 */
export function disableRoute(
  id: number,
  data: { version: number; reason: string },
): Promise<void> {
  return put(`/craft/routes/${id}/disable`, data)
}

/** 基于生效路线复制草稿新版本，返回新路线 id */
export function createRouteVersion(
  id: number,
  data: { version: number; newRoutingVersion: string; reason: string },
): Promise<number> {
  return post(`/craft/routes/${id}/versions`, data)
}

/** 仅草稿可删除（逻辑删除） */
export function deleteRoute(id: number, version: number): Promise<void> {
  return del(`/craft/routes/${id}`, { version })
}
