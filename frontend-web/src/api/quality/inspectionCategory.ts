import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 质量检验分类响应。 */
export interface InspectionCategory {
  id: number
  categoryCode: string
  categoryName: string
  enabledStatus: number
  remark: string | null
  createTime: string
  updateTime: string
}

/** 质量检验分类创建或修改请求。 */
export interface InspectionCategorySaveReq {
  categoryCode: string
  categoryName: string
  enabledStatus?: number | null
  remark?: string | null
}

/** 质量检验分类分页查询参数。 */
export interface InspectionCategoryPageParams {
  keyword?: string
  enabledStatus?: number
}

/** 创建质量检验分类并返回分类主键。 */
export function createInspectionCategory(data: InspectionCategorySaveReq): Promise<number> {
  return post('/quality/inspection-categories', data)
}

/** 修改指定质量检验分类。 */
export function updateInspectionCategory(
  id: number,
  data: InspectionCategorySaveReq,
): Promise<void> {
  return put(`/quality/inspection-categories/${id}`, data)
}

/** 删除指定质量检验分类。 */
export function deleteInspectionCategory(id: number): Promise<void> {
  return del(`/quality/inspection-categories/${id}`)
}

/** 查询指定质量检验分类。 */
export function getInspectionCategory(id: number): Promise<InspectionCategory> {
  return get(`/quality/inspection-categories/${id}`)
}

/** 分页查询质量检验分类。 */
export function getInspectionCategoryPage(
  params: InspectionCategoryPageParams & PageParam,
): Promise<PageResult<InspectionCategory>> {
  return get('/quality/inspection-categories/page', params)
}
