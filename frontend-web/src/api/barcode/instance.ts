import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'
import type { BarcodeTemplatePreviewResp } from './template'

/** 条码实例接口，对齐后端 BarcodeInstanceController（/api/barcode/instances）。 */

/** 条码实例响应。 */
export interface BarcodeInstance {
  id: number
  barcodeValue: string
  barcodeTypeId: number
  /** 条码模式：1 唯一码 2 批次码 */
  barcodeMode: number
  applyRuleId: number | null
  productId: number | null
  materialId: number | null
  batchNo: string | null
  workOrderId: number | null
  taskId: number | null
  /** 来源：1 规则生成 2 传入值 3 外部导入 */
  sourceType: number
  /** 状态：0 未使用 1 已使用 2 已作废 */
  barcodeStatus: number
  createBy: number | null
  createTime: string
  updateTime: string
}

export interface BarcodeInstancePageParams extends PageParam {
  barcodeValue?: string
  batchNo?: string
  barcodeTypeId?: number
  workOrderId?: number
  taskId?: number
  sourceType?: number
  barcodeStatus?: number
}

/** 生成请求：按应用规则生成，传入值来源须提供 inputBarcodeValue。 */
export interface BarcodeGenerateReq {
  applyRuleId: number
  batchNo?: string
  workOrderId?: number
  taskId?: number
  lineCode?: string
  inputBarcodeValue?: string
}

/** 生成响应。 */
export interface BarcodeGenerateResp {
  id: number
  barcodeValue: string
  barcodeTypeId: number
  barcodeMode: number
  batchNo: string | null
  sourceType: number
  barcodeStatus: number
}

/** 批量生成请求，单次上限 500，不支持传入值来源。 */
export interface BarcodeBatchGenerateReq extends BarcodeGenerateReq {
  quantity: number
}

/** 导入明细项。 */
export interface BarcodeImportItem {
  barcodeValue: string
  batchNo?: string
  workOrderId?: number
  taskId?: number
}

/** 外部导入请求，JSON 数组同步导入，单次最多 500 条。 */
export interface BarcodeImportReq {
  applyRuleId: number
  items: BarcodeImportItem[]
}

/** 导入失败明细。 */
export interface BarcodeImportFailure {
  index: number
  barcodeValue: string
  reason: string
}

/** 导入响应：成功数、失败数与逐条失败原因。 */
export interface BarcodeImportResp {
  totalCount: number
  successCount: number
  failCount: number
  failures: BarcodeImportFailure[]
}

/** 解析请求。 */
export interface BarcodeParseReq {
  barcodeValue: string
}

/** 解析响应：条码事实 + 业务对象上下文。 */
export interface BarcodeParseResp {
  id: number
  barcodeValue: string
  barcodeTypeId: number
  barcodeTypeCode: string | null
  barcodeTypeName: string | null
  barcodeMode: number
  batchNo: string | null
  productId: number | null
  productCode: string | null
  productName: string | null
  materialId: number | null
  materialCode: string | null
  materialName: string | null
  workOrderId: number | null
  taskId: number | null
  sourceType: number
  barcodeStatus: number
  createTime: string
}

/** 打印请求，重复打印须填写原因。 */
export interface BarcodePrintReq {
  reason?: string
}

/** 打印响应：打印记录事实 + 预览数据。 */
export interface BarcodePrintResp {
  printRecordId: number
  barcodeId: number
  barcodeValue: string
  templateId: number | null
  templateVersion: string | null
  printCount: number
  printTime: string
  preview: BarcodeTemplatePreviewResp | null
}

/** 作废请求。 */
export interface BarcodeCancelReq {
  reason?: string
}

/** 条码使用记录。 */
export interface BarcodeUseRecord {
  id: number
  barcodeId: number
  taskId: number | null
  processId: number | null
  userId: number | null
  equipmentId: number | null
  /** 使用类型：1 工序开工 2 工序完工 3 报工 4 其他 */
  useType: number
  businessTime: string
  createTime: string
}

/** 分页查询条码实例。 */
export function getBarcodeInstancePage(
  params: BarcodeInstancePageParams,
): Promise<PageResult<BarcodeInstance>> {
  return get('/barcode/instances/page', params)
}

/** 查询条码实例详情。 */
export function getBarcodeInstance(id: number): Promise<BarcodeInstance> {
  return get(`/barcode/instances/${id}`)
}

/** 查询条码扫码使用记录，按业务发生时间倒序。 */
export function getBarcodeUseRecords(id: number): Promise<BarcodeUseRecord[]> {
  return get(`/barcode/instances/${id}/use_records`)
}

/** 生成批次码。 */
export function generateBarcode(data: BarcodeGenerateReq): Promise<BarcodeGenerateResp> {
  return post('/barcode/instances/generate', data)
}

/** 批量生成批次码，单次上限 500。 */
export function batchGenerateBarcodes(
  data: BarcodeBatchGenerateReq,
): Promise<BarcodeGenerateResp[]> {
  return post('/barcode/instances/batch_generate', data)
}

/** 导入外部批次码，部分成功并返回逐条失败原因。 */
export function importBarcodes(data: BarcodeImportReq): Promise<BarcodeImportResp> {
  return post('/barcode/instances/import', data)
}

/** 解析条码值，返回条码事实与业务对象上下文。 */
export function parseBarcode(data: BarcodeParseReq): Promise<BarcodeParseResp> {
  return post('/barcode/instances/parse', data)
}

/** 记录打印动作并返回预览数据，不驱动真实打印机。 */
export function printBarcode(id: number, data: BarcodePrintReq): Promise<BarcodePrintResp> {
  return post(`/barcode/instances/${id}/print`, data)
}

/** 作废未使用条码。 */
export function cancelBarcode(id: number, data: BarcodeCancelReq): Promise<void> {
  return put(`/barcode/instances/${id}/cancel`, data)
}
