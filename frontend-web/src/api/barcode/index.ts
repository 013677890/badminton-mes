import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface BarcodeType {
  id: number
  typeCode: string
  typeName: string
  applyObject: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface BarcodeRuleItem {
  id?: number
  seq: number
  itemType: number
  itemValue?: string
  dateFormat?: string
  itemLength?: number
}

export interface BarcodeRule {
  id: number
  ruleCode: string
  ruleName: string
  barcodeTypeId: number
  serialLength: number
  serialResetCycle: number
  status: number
  items: BarcodeRuleItem[]
  createTime: string
  updateTime: string
}

export interface BarcodeTemplateField {
  id?: number
  fieldName: string
  fieldType: number
  dataSource: string
  posX: number
  posY: number
  fontSize?: number
}

export interface BarcodeTemplate {
  id: number
  templateCode: string
  templateName: string
  paperWidth: number
  paperHeight: number
  version: string
  status: number
  fields: BarcodeTemplateField[]
  createTime: string
  updateTime: string
}

export interface BarcodeApplicationRule {
  id: number
  objectType: number
  productId: number | null
  materialId: number | null
  barcodeTypeId: number
  barcodeMode: number
  ruleId: number | null
  templateId: number
  sourceType: number
  defaultFlag: boolean
  version: string
  status: number
  createTime: string
  updateTime: string
}

export interface BarcodeInstance {
  id: number
  barcodeValue: string
  barcodeTypeId: number
  barcodeMode: number
  applyRuleId: number | null
  productId: number | null
  materialId: number | null
  batchNo: string | null
  workOrderId: number | null
  taskId: number | null
  sourceType: number
  barcodeStatus: number
  createBy: number
  createTime: string
  updateTime: string
}

export interface BarcodeUseRecord {
  id: number
  barcodeId: number
  taskId: number | null
  processId: number | null
  userId: number | null
  equipmentId: number | null
  useType: number
  businessTime: string
  createTime: string
}

export interface BarcodeGenerateReq {
  applyRuleId: number
  batchNo?: string
  workOrderId?: number
  taskId?: number
  lineCode?: string
  inputBarcodeValue?: string
}

export interface BarcodeGenerateResult {
  id: number
  barcodeValue: string
  barcodeTypeId: number
  barcodeMode: number
  batchNo: string | null
  sourceType: number
  barcodeStatus: number
}

export interface BarcodeParseResult extends BarcodeGenerateResult {
  barcodeTypeCode: string
  barcodeTypeName: string
  productId: number | null
  productCode: string | null
  productName: string | null
  materialId: number | null
  materialCode: string | null
  materialName: string | null
  workOrderId: number | null
  taskId: number | null
  createTime: string
}

export interface BarcodePrintResult {
  printRecordId: number
  barcodeId: number
  barcodeValue: string
  templateId: number
  templateVersion: string
  printCount: number
  printTime: string
  preview: Record<string, unknown>
}

export interface BarcodeRulePreviewResult {
  barcodeValue: string
  totalLength: number
  serialCapacity: number
  segments: Array<Record<string, unknown>>
}

export interface BarcodeImportResult {
  totalCount: number
  successCount: number
  failCount: number
  failures: Array<Record<string, unknown>>
}

export interface BarcodeTypePageParams { typeCode?: string; typeName?: string; status?: number }
export interface BarcodeRulePageParams { ruleCode?: string; ruleName?: string; barcodeTypeId?: number; status?: number }
export interface BarcodeTemplatePageParams { templateCode?: string; templateName?: string; status?: number }
export interface BarcodeApplicationRulePageParams {
  objectType?: number; productId?: number; materialId?: number; barcodeTypeId?: number
  sourceType?: number; status?: number
}
export interface BarcodeInstancePageParams {
  barcodeValue?: string; batchNo?: string; barcodeTypeId?: number; workOrderId?: number
  taskId?: number; sourceType?: number; barcodeStatus?: number
}

const TYPE_URL = '/barcode/types'
export function getBarcodeTypePage(params: BarcodeTypePageParams & PageParam): Promise<PageResult<BarcodeType>> { return get(`${TYPE_URL}/page`, params) }
export function getBarcodeTypeOptions(): Promise<BarcodeType[]> { return get(`${TYPE_URL}/options`) }
export function createBarcodeType(data: Omit<BarcodeType, 'id' | 'status' | 'createTime' | 'updateTime'>): Promise<number> { return post(TYPE_URL, data) }
export function updateBarcodeType(id: number, data: Pick<BarcodeType, 'typeCode' | 'typeName' | 'applyObject'>): Promise<void> { return put(`${TYPE_URL}/${id}`, data) }
export function changeBarcodeTypeStatus(id: number, enabled: boolean): Promise<void> { return put(`${TYPE_URL}/${id}/${enabled ? 'enable' : 'disable'}`) }
export function deleteBarcodeType(id: number): Promise<void> { return del(`${TYPE_URL}/${id}`) }

const RULE_URL = '/barcode/rules'
export function getBarcodeRulePage(params: BarcodeRulePageParams & PageParam): Promise<PageResult<BarcodeRule>> { return get(`${RULE_URL}/page`, params) }
export function getBarcodeRule(id: number): Promise<BarcodeRule> { return get(`${RULE_URL}/${id}`) }
export function createBarcodeRule(data: Omit<BarcodeRule, 'id' | 'status' | 'createTime' | 'updateTime'>): Promise<number> { return post(RULE_URL, data) }
export function updateBarcodeRule(id: number, data: Omit<BarcodeRule, 'id' | 'status' | 'createTime' | 'updateTime'>): Promise<void> { return put(`${RULE_URL}/${id}`, data) }
export function changeBarcodeRuleStatus(id: number, enabled: boolean): Promise<void> { return put(`${RULE_URL}/${id}/${enabled ? 'enable' : 'disable'}`) }
export function deleteBarcodeRule(id: number): Promise<void> { return del(`${RULE_URL}/${id}`) }
export function previewBarcodeRule(data: { serialLength: number; items: BarcodeRuleItem[]; sampleProductCode?: string; sampleLineCode?: string }): Promise<BarcodeRulePreviewResult> { return post(`${RULE_URL}/preview`, data) }
export function validateBarcodeRule(data: { serialLength: number; items: BarcodeRuleItem[] }): Promise<{ valid: boolean; errors: string[] }> { return post(`${RULE_URL}/validate`, data) }

const TEMPLATE_URL = '/barcode/templates'
export function getBarcodeTemplatePage(params: BarcodeTemplatePageParams & PageParam): Promise<PageResult<BarcodeTemplate>> { return get(`${TEMPLATE_URL}/page`, params) }
export function getBarcodeTemplate(id: number): Promise<BarcodeTemplate> { return get(`${TEMPLATE_URL}/${id}`) }
export function createBarcodeTemplate(data: Omit<BarcodeTemplate, 'id' | 'version' | 'status' | 'createTime' | 'updateTime'>): Promise<number> { return post(TEMPLATE_URL, data) }
export function updateBarcodeTemplate(id: number, data: Omit<BarcodeTemplate, 'id' | 'version' | 'status' | 'createTime' | 'updateTime'>): Promise<void> { return put(`${TEMPLATE_URL}/${id}`, data) }
export function changeBarcodeTemplateStatus(id: number, enabled: boolean): Promise<void> { return put(`${TEMPLATE_URL}/${id}/${enabled ? 'enable' : 'disable'}`) }
export function previewBarcodeTemplate(templateId: number, sampleBarcodeValue: string, sampleData: Record<string, string> = {}): Promise<Record<string, unknown>> { return post(`${TEMPLATE_URL}/preview`, { templateId, sampleBarcodeValue, sampleData }) }

const APPLICATION_URL = '/barcode/application_rules'
export function getBarcodeApplicationRulePage(params: BarcodeApplicationRulePageParams & PageParam): Promise<PageResult<BarcodeApplicationRule>> { return get(`${APPLICATION_URL}/page`, params) }
export function getBarcodeApplicationRuleOptions(params?: BarcodeApplicationRulePageParams): Promise<BarcodeApplicationRule[]> { return get(`${APPLICATION_URL}/options`, params) }
export function getBarcodeApplicationRule(id: number): Promise<BarcodeApplicationRule> { return get(`${APPLICATION_URL}/${id}`) }
export function createBarcodeApplicationRule(data: Omit<BarcodeApplicationRule, 'id' | 'version' | 'status' | 'createTime' | 'updateTime'>): Promise<number> { return post(APPLICATION_URL, data) }
export function updateBarcodeApplicationRule(id: number, data: Omit<BarcodeApplicationRule, 'id' | 'version' | 'status' | 'createTime' | 'updateTime'>): Promise<void> { return put(`${APPLICATION_URL}/${id}`, data) }
export function changeBarcodeApplicationRuleStatus(id: number, enabled: boolean): Promise<void> { return put(`${APPLICATION_URL}/${id}/${enabled ? 'enable' : 'disable'}`) }
export function deleteBarcodeApplicationRule(id: number): Promise<void> { return del(`${APPLICATION_URL}/${id}`) }

const INSTANCE_URL = '/barcode/instances'
export function getBarcodeInstancePage(params: BarcodeInstancePageParams & PageParam): Promise<PageResult<BarcodeInstance>> { return get(`${INSTANCE_URL}/page`, params) }
export function getBarcodeInstance(id: number): Promise<BarcodeInstance> { return get(`${INSTANCE_URL}/${id}`) }
export function getBarcodeUseRecords(id: number): Promise<BarcodeUseRecord[]> { return get(`${INSTANCE_URL}/${id}/use_records`) }
export function generateBarcode(data: BarcodeGenerateReq): Promise<BarcodeGenerateResult> { return post(`${INSTANCE_URL}/generate`, data) }
export function batchGenerateBarcodes(data: BarcodeGenerateReq & { quantity: number }): Promise<BarcodeGenerateResult[]> { return post(`${INSTANCE_URL}/batch_generate`, data) }
export function importBarcodes(data: { applyRuleId: number; items: Array<{ barcodeValue: string; batchNo?: string; workOrderId?: number; taskId?: number }> }): Promise<BarcodeImportResult> { return post(`${INSTANCE_URL}/import`, data) }
export function parseBarcode(barcodeValue: string): Promise<BarcodeParseResult> { return post(`${INSTANCE_URL}/parse`, { barcodeValue }) }
export function printBarcode(id: number, reason: string): Promise<BarcodePrintResult> { return post(`${INSTANCE_URL}/${id}/print`, { reason }) }
export function cancelBarcode(id: number, reason: string): Promise<void> { return put(`${INSTANCE_URL}/${id}/cancel`, { reason }) }
