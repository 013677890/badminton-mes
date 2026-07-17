import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 质量检验项目响应。 */
export interface InspectionItem {
  id: number
  itemCode: string
  itemName: string
  categoryId: number
  categoryCode: string
  categoryName: string
  valueType: string
  unit: string | null
  standardValue: string | null
  lowerLimit: number | null
  upperLimit: number | null
  judgmentMethod: string
  inspectionMethod: string | null
  requiredFlag: boolean
  enabledStatus: number
  remark: string | null
  createTime: string
  updateTime: string
}

/** 质量检验项目创建或修改请求。 */
export interface InspectionItemSaveReq {
  itemCode: string
  itemName: string
  categoryId: number
  valueType: string
  unit?: string | null
  standardValue?: string | null
  lowerLimit?: number | null
  upperLimit?: number | null
  judgmentMethod: string
  inspectionMethod?: string | null
  requiredFlag?: boolean | null
  enabledStatus?: number | null
  remark?: string | null
}

/** 质量检验项目分页查询参数。 */
export interface InspectionItemPageParams {
  keyword?: string
  categoryId?: number
  valueType?: string
  requiredFlag?: boolean
  enabledStatus?: number
}

/** 创建质量检验项目并返回项目主键。 */
export function createInspectionItem(data: InspectionItemSaveReq): Promise<number> {
  return post('/quality/inspection-items', data)
}

/** 修改指定质量检验项目。 */
export function updateInspectionItem(id: number, data: InspectionItemSaveReq): Promise<void> {
  return put(`/quality/inspection-items/${id}`, data)
}

/** 删除指定质量检验项目。 */
export function deleteInspectionItem(id: number): Promise<void> {
  return del(`/quality/inspection-items/${id}`)
}

/** 查询指定质量检验项目。 */
export function getInspectionItem(id: number): Promise<InspectionItem> {
  return get(`/quality/inspection-items/${id}`)
}

/** 分页查询质量检验项目。 */
export function getInspectionItemPage(
  params: InspectionItemPageParams & PageParam,
): Promise<PageResult<InspectionItem>> {
  return get('/quality/inspection-items/page', params)
}
