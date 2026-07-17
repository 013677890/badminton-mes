import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 产品主档接口，对齐后端 ProductController（/api/production/products） */

export interface Product {
  id: number
  productCode: string
  productName: string
  spec: string | null
  /** 1 成品 2 半成品 */
  productType: number
  grade: string | null
  unitId: number
  /** 1 启用 0 停用 */
  status: number
  /** 乐观锁版本，更新/删除/启停必须回传 */
  version: number
  createTime: string
  updateTime: string
}

export interface ProductSaveParams {
  productCode: string
  productName: string
  spec?: string
  productType: number
  grade?: string
  unitId: number
  status: number
}

export interface ProductPageParams {
  productCode?: string
  productName?: string
  productType?: number
  unitId?: number
  status?: number
}

export function getProductPage(
  params: ProductPageParams & PageParam,
): Promise<PageResult<Product>> {
  // 分页查询由后端按编码、名称、类型、单位和状态过滤。
  return get('/production/products/page', params)
}

export function getProduct(id: number): Promise<Product> {
  // 查询未删除产品主档详情。
  return get(`/production/products/${id}`)
}

export function createProduct(data: ProductSaveParams): Promise<number> {
  // 创建产品时后端锁定并校验计量单位及编码唯一性。
  return post('/production/products', data)
}

export function updateProduct(id: number, data: ProductSaveParams & { version: number }): Promise<void> {
  // 版本字段保护并发更新，产品活动引用约束由后端处理。
  return put(`/production/products/${id}`, data)
}

export function deleteProduct(id: number, version: number): Promise<void> {
  // 仅无历史引用产品可逻辑删除，后端保留引用完整性。
  return del(`/production/products/${id}`, { version })
}

/** 启用/停用（乐观锁校验 version） */
export function updateProductStatus(id: number, status: number, version: number): Promise<void> {
  // 启停状态按乐观锁版本更新，停用前检查工单、BOM、路线及计件规则引用。
  return put(`/production/products/${id}/status`, { status, version })
}
