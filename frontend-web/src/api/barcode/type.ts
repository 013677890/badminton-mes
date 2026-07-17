import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 条码类型接口，对齐后端 BarcodeTypeController（/api/barcode/types）。 */

/** 条码类型响应。 */
export interface BarcodeType {
  id: number
  typeCode: string
  typeName: string
  applyObject: string | null
  /** 状态：1 启用 / 0 停用 */
  status: number
  createTime: string
  updateTime: string
}

/** 条码类型创建/修改请求。状态由独立启停接口流转，不在此提交。 */
export interface BarcodeTypeSaveReq {
  typeCode: string
  typeName: string
  applyObject?: string | null
}

export interface BarcodeTypePageParams extends PageParam {
  typeCode?: string
  typeName?: string
  status?: number
}

/** 分页查询条码类型。 */
export function getBarcodeTypePage(
  params: BarcodeTypePageParams,
): Promise<PageResult<BarcodeType>> {
  return get('/barcode/types/page', params)
}

/** 查询启用条码类型选项。 */
export function getBarcodeTypeOptions(): Promise<BarcodeType[]> {
  return get('/barcode/types/options')
}

/** 查询条码类型详情。 */
export function getBarcodeType(id: number): Promise<BarcodeType> {
  return get(`/barcode/types/${id}`)
}

/** 新增条码类型。 */
export function createBarcodeType(data: BarcodeTypeSaveReq): Promise<number> {
  return post('/barcode/types', data)
}

/** 修改条码类型。 */
export function updateBarcodeType(id: number, data: BarcodeTypeSaveReq): Promise<void> {
  return put(`/barcode/types/${id}`, data)
}

/** 启用条码类型。 */
export function enableBarcodeType(id: number): Promise<void> {
  return put(`/barcode/types/${id}/enable`)
}

/** 停用条码类型。 */
export function disableBarcodeType(id: number): Promise<void> {
  return put(`/barcode/types/${id}/disable`)
}

/** 删除条码类型。 */
export function deleteBarcodeType(id: number): Promise<void> {
  return del(`/barcode/types/${id}`)
}
