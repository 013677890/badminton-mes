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
  return get('/production/products/page', params)
}

export function getProduct(id: number): Promise<Product> {
  return get(`/production/products/${id}`)
}

export function createProduct(data: ProductSaveParams): Promise<number> {
  return post('/production/products', data)
}

export function updateProduct(id: number, data: ProductSaveParams & { version: number }): Promise<void> {
  return put(`/production/products/${id}`, data)
}

export function deleteProduct(id: number, version: number): Promise<void> {
  return del(`/production/products/${id}`, { version })
}

/** 启用/停用（乐观锁校验 version） */
export function updateProductStatus(id: number, status: number, version: number): Promise<void> {
  return put(`/production/products/${id}/status`, { status, version })
}
