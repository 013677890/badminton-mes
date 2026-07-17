import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * BOM 版本与明细接口，对齐后端 BomController（/api/production/boms）。
 * 注意乐观锁字段名是 lockVersion（区别于业务版本号 version 字符串）。
 */

export interface BomDetail {
  id: number
  materialId: number
  materialCode: string
  materialName: string
  /** 单件标准用量（4 位小数） */
  quantity: number
  /** 损耗率 %（0-100，2 位小数） */
  lossRate: number
}

export interface Bom {
  id: number
  bomCode: string
  productId: number
  productCode: string
  productName: string
  /** 业务版本号（如 V1.0），非乐观锁 */
  version: string
  /** 0 草稿 1 生效 2 停用 */
  bomStatus: number
  /** 乐观锁版本 */
  lockVersion: number
  details: BomDetail[]
  createTime: string
  updateTime: string
}

export interface BomDetailSaveParams {
  materialId: number
  quantity: number
  lossRate: number
}

export interface BomSaveParams {
  bomCode: string
  productId: number
  version: string
  /** 1-200 行 */
  details: BomDetailSaveParams[]
}

export interface BomPageParams {
  bomCode?: string
  productId?: number
  version?: string
  bomStatus?: number
}

export function getBomPage(params: BomPageParams & PageParam): Promise<PageResult<Bom>> {
  // 分页参数和业务筛选条件一起提交，响应结构由 request 工具统一解包为 PageResult。
  return get('/production/boms/page', params)
}

/** 聚合详情（含明细行） */
export function getBom(id: number): Promise<Bom> {
  // 详情接口一次返回主表及有效明细，页面无需再按明细逐条请求物料。
  return get(`/production/boms/${id}`)
}

export function createBom(data: BomSaveParams): Promise<number> {
  // 创建请求由后端校验产品、物料明细和业务版本唯一性，成功返回主键。
  return post('/production/boms', data)
}

/** 仅草稿可改 */
export function updateBom(id: number, data: BomSaveParams & { lockVersion: number }): Promise<void> {
  // 携带锁版本更新草稿，后端据此拒绝旧页面覆盖最新编辑结果。
  return put(`/production/boms/${id}`, data)
}

/** 仅草稿可删 */
export function deleteBom(id: number, lockVersion: number): Promise<void> {
  // 通过查询参数传递锁版本，后端执行逻辑删除而不是物理删除历史明细。
  return del(`/production/boms/${id}`, { lockVersion })
}

/** 草稿 → 生效；同产品同版本互斥由后端校验 */
export function activateBom(id: number, lockVersion: number): Promise<void> {
  // 生效操作由后端锁定同产品 BOM 集合，保证同一产品最多一个生效版本。
  return put(`/production/boms/${id}/activate`, { lockVersion })
}

/** 生效 → 停用 */
export function disableBom(id: number, lockVersion: number): Promise<void> {
  // 停用只允许当前生效版本，锁版本用于处理并发状态修改。
  return put(`/production/boms/${id}/disable`, { lockVersion })
}

/** 以现有 BOM 为模板复制新版本（草稿态），返回新主键 */
export function createBomNewVersion(
  id: number,
  data: { lockVersion: number; bomCode: string; version: string },
): Promise<number> {
  // 后端复制来源 BOM 的有效明细并创建草稿版本，前端只提供新的编码和业务版本号。
  return post(`/production/boms/${id}/new_version`, data)
}
