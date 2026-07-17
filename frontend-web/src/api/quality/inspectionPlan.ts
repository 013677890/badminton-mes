import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 检验方案项目明细保存请求。 */
export interface InspectionPlanItemSaveReq {
  inspectionItemId: number
  sortOrder?: number | null
  sampleQuantity: number
  requiredFlag?: boolean | null
  standardValue?: string | null
  lowerLimit?: number | null
  upperLimit?: number | null
  judgmentMethod?: string | null
}

/** 检验标准方案创建或修改请求。 */
export interface InspectionPlanSaveReq {
  planCode: string
  planName: string
  productId?: number | null
  customerId?: number | null
  inspectionType: string
  effectiveDate?: string | null
  defaultFlag?: boolean | null
  remark?: string | null
  items: InspectionPlanItemSaveReq[]
}

/** 检验方案项目明细响应。 */
export interface InspectionPlanItem {
  id: number
  inspectionItemId: number
  itemCode: string
  itemName: string
  valueType: string
  unit: string | null
  sortOrder: number
  sampleQuantity: number
  requiredFlag: boolean
  standardValue: string | null
  lowerLimit: number | null
  upperLimit: number | null
  judgmentMethod: string
}

/** 检验标准方案响应。 */
export interface InspectionPlan {
  id: number
  planCode: string
  planName: string
  productId: number | null
  customerId: number | null
  inspectionType: string
  versionNo: number
  planStatus: string
  effectiveDate: string | null
  defaultFlag: boolean
  remark: string | null
  createBy: number
  auditBy: number | null
  auditTime: string | null
  createTime: string
  updateTime: string
  items: InspectionPlanItem[]
}

/** 检验标准方案分页查询参数。 */
export interface InspectionPlanPageParams {
  keyword?: string
  productId?: number
  customerId?: number
  inspectionType?: string
  planStatus?: string
}

/** 创建检验标准方案草稿并返回方案主键。 */
export function createInspectionPlan(data: InspectionPlanSaveReq): Promise<number> {
  return post('/quality/inspection-plans', data)
}

/** 修改指定检验标准方案草稿。 */
export function updateInspectionPlan(id: number, data: InspectionPlanSaveReq): Promise<void> {
  return put(`/quality/inspection-plans/${id}`, data)
}

/** 删除指定检验标准方案。 */
export function deleteInspectionPlan(id: number): Promise<void> {
  return del(`/quality/inspection-plans/${id}`)
}

/** 审核指定检验标准方案。 */
export function auditInspectionPlan(id: number): Promise<void> {
  return put(`/quality/inspection-plans/${id}/audit`)
}

/** 停用指定检验标准方案。 */
export function disableInspectionPlan(id: number): Promise<void> {
  return put(`/quality/inspection-plans/${id}/disable`)
}

/** 基于指定方案创建新版本并返回新方案主键。 */
export function createInspectionPlanVersion(id: number): Promise<number> {
  return post(`/quality/inspection-plans/${id}/versions`)
}

/** 查询指定检验标准方案。 */
export function getInspectionPlan(id: number): Promise<InspectionPlan> {
  return get(`/quality/inspection-plans/${id}`)
}

/** 分页查询检验标准方案。 */
export function getInspectionPlanPage(
  params: InspectionPlanPageParams & PageParam,
): Promise<PageResult<InspectionPlan>> {
  return get('/quality/inspection-plans/page', params)
}
