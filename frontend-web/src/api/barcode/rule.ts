import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 条码规则接口，对齐后端 BarcodeRuleController（/api/barcode/rules）。 */

/** 规则组成明细项（响应）。 */
export interface BarcodeRuleItem {
  id: number
  /** 组成顺序 */
  seq: number
  /** 组成类型：1 常量 2 日期 3 变量 4 流水号 */
  itemType: number
  /** 常量值或变量名 */
  itemValue: string | null
  /** 日期格式（类型=日期时） */
  dateFormat: string | null
  /** 该段长度 */
  itemLength: number | null
}

/** 规则组成明细项（保存请求）。 */
export interface BarcodeRuleItemSaveReq {
  seq: number
  itemType: number
  itemValue?: string | null
  dateFormat?: string | null
  itemLength?: number | null
}

/** 条码规则响应，详情含组成明细。 */
export interface BarcodeRule {
  id: number
  ruleCode: string
  ruleName: string
  barcodeTypeId: number
  serialLength: number
  /** 流水号重置周期：1 按日 2 按月 3 不重置 */
  serialResetCycle: number
  status: number
  items: BarcodeRuleItem[]
  createTime: string
  updateTime: string
}

/** 条码规则创建/修改请求，携带完整明细，修改时整体重写明细。 */
export interface BarcodeRuleSaveReq {
  ruleCode: string
  ruleName: string
  barcodeTypeId: number
  serialLength: number
  serialResetCycle: number
  items: BarcodeRuleItemSaveReq[]
}

export interface BarcodeRulePageParams extends PageParam {
  ruleCode?: string
  ruleName?: string
  barcodeTypeId?: number
  status?: number
}

/** 预览分段试算结果。 */
export interface BarcodeRulePreviewSegment {
  seq: number
  itemType: number
  content: string | null
}

/** 规则预览请求：直接携带配置，支持保存前预览。 */
export interface BarcodeRulePreviewReq {
  serialLength: number
  items: BarcodeRuleItemSaveReq[]
  sampleProductCode?: string
  sampleLineCode?: string
}

/** 规则预览响应。 */
export interface BarcodeRulePreviewResp {
  barcodeValue: string
  totalLength: number
  serialCapacity: number
  segments: BarcodeRulePreviewSegment[]
}

/** 规则校验请求。 */
export interface BarcodeRuleValidateReq {
  serialLength: number
  items: BarcodeRuleItemSaveReq[]
}

/** 规则校验响应。 */
export interface BarcodeRuleValidateResp {
  valid: boolean
  errors: string[]
}

/** 分页查询条码规则（列表不含明细）。 */
export function getBarcodeRulePage(
  params: BarcodeRulePageParams,
): Promise<PageResult<BarcodeRule>> {
  return get('/barcode/rules/page', params)
}

/** 查询条码规则详情（含组成明细）。 */
export function getBarcodeRule(id: number): Promise<BarcodeRule> {
  return get(`/barcode/rules/${id}`)
}

/** 新增条码规则及组成明细。 */
export function createBarcodeRule(data: BarcodeRuleSaveReq): Promise<number> {
  return post('/barcode/rules', data)
}

/** 修改条码规则，整体重写明细，只影响新生成条码。 */
export function updateBarcodeRule(id: number, data: BarcodeRuleSaveReq): Promise<void> {
  return put(`/barcode/rules/${id}`, data)
}

/** 启用条码规则。 */
export function enableBarcodeRule(id: number): Promise<void> {
  return put(`/barcode/rules/${id}/enable`)
}

/** 停用条码规则。 */
export function disableBarcodeRule(id: number): Promise<void> {
  return put(`/barcode/rules/${id}/disable`)
}

/** 删除未使用的条码规则。 */
export function deleteBarcodeRule(id: number): Promise<void> {
  return del(`/barcode/rules/${id}`)
}

/** 预览规则生成结果，不落库、不消耗真实流水。 */
export function previewBarcodeRule(data: BarcodeRulePreviewReq): Promise<BarcodeRulePreviewResp> {
  return post('/barcode/rules/preview', data)
}

/** 校验规则配置合法性，返回逐条错误说明。 */
export function validateBarcodeRule(data: BarcodeRuleValidateReq): Promise<BarcodeRuleValidateResp> {
  return post('/barcode/rules/validate', data)
}
