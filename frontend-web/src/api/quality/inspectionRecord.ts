import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 质量检验单创建请求。 */
export interface InspectionRecordCreateReq {
  planId: number
  workOrderId?: number | null
  sourceDocumentId?: number | null
  sourceDocumentNo?: string | null
  productId?: number | null
  customerId?: number | null
  productionLineId?: number | null
  batchNo: string
  sampleQuantity: number
}

/** 单个检验项目实测结果保存请求。 */
export interface InspectionResultSaveReq {
  resultId: number
  measuredValue?: string | null
  judgmentResult?: string | null
  defectDescription?: string | null
}

/** 检验项目实测结果批量保存请求。 */
export interface InspectionResultsSaveReq {
  results: InspectionResultSaveReq[]
}

/** 质量检验单提交请求。 */
export interface InspectionRecordSubmitReq {
  conclusion: string
  nonconformanceDescription?: string | null
  disposition?: string | null
}

/** 质量检验项目结果响应。 */
export interface InspectionResult {
  id: number
  inspectionItemId: number
  itemCode: string
  itemName: string
  valueType: string
  unit: string | null
  requiredFlag: boolean
  standardValue: string | null
  lowerLimit: number | null
  upperLimit: number | null
  judgmentMethod: string
  measuredValue: string | null
  judgmentResult: string | null
  defectDescription: string | null
  sortOrder: number
}

/** 质量检验单响应。 */
export interface InspectionRecord {
  id: number
  inspectionNo: string
  inspectionType: string
  planId: number
  planCode: string
  planVersion: number
  workOrderId: number | null
  sourceDocumentId: number | null
  sourceDocumentNo: string | null
  productId: number | null
  customerId: number | null
  productionLineId: number | null
  batchNo: string
  sampleQuantity: number
  recordStatus: string
  conclusion: string | null
  releaseStatus: string | null
  nonconformanceDescription: string | null
  disposition: string | null
  inspectorId: number | null
  inspectedAt: string | null
  createTime: string
  updateTime: string
  results: InspectionResult[]
}

/** 质量检验单分页查询参数。 */
export interface InspectionRecordPageParams {
  keyword?: string
  inspectionType?: string
  recordStatus?: string
  conclusion?: string
  workOrderId?: number
  productId?: number
  batchNo?: string
}

/** 创建质量检验单并返回检验单主键。 */
export function createInspectionRecord(
  inspectionType: string,
  data: InspectionRecordCreateReq,
): Promise<number> {
  return post('/quality/inspection-records', data, { params: { inspectionType } })
}

/** 批量保存指定检验单的项目实测结果。 */
export function saveInspectionResults(
  id: number,
  data: InspectionResultsSaveReq,
): Promise<void> {
  return put(`/quality/inspection-records/${id}/results`, data)
}

/** 提交指定质量检验单。 */
export function submitInspectionRecord(
  id: number,
  data: InspectionRecordSubmitReq,
): Promise<void> {
  return put(`/quality/inspection-records/${id}/submit`, data)
}

/** 查询指定质量检验单。 */
export function getInspectionRecord(id: number): Promise<InspectionRecord> {
  return get(`/quality/inspection-records/${id}`)
}

/** 分页查询质量检验单。 */
export function getInspectionRecordPage(
  params: InspectionRecordPageParams & PageParam,
): Promise<PageResult<InspectionRecord>> {
  return get('/quality/inspection-records/page', params)
}
