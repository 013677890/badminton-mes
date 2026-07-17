import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 条码应用规则接口，对齐后端 BarcodeApplicationRuleController（/api/barcode/application_rules）。 */

/** 条码应用规则响应。 */
export interface BarcodeApplicationRule {
  id: number
  /** 对象类型：1 产品 2 物料 */
  objectType: number
  productId: number | null
  materialId: number | null
  barcodeTypeId: number
  /** 条码模式：1 唯一码 2 批次码 */
  barcodeMode: number
  ruleId: number | null
  templateId: number
  /** 来源：1 规则生成 2 传入值生成 3 外部导入 */
  sourceType: number
  defaultFlag: boolean
  version: string | null
  status: number
  createTime: string
  updateTime: string
}

/** 应用规则创建/修改请求。 */
export interface BarcodeApplicationRuleSaveReq {
  objectType: number
  productId?: number | null
  materialId?: number | null
  barcodeTypeId: number
  barcodeMode: number
  ruleId?: number | null
  templateId: number
  sourceType: number
  defaultFlag?: boolean
}

export interface BarcodeApplicationRulePageParams extends PageParam {
  objectType?: number
  productId?: number
  materialId?: number
  barcodeTypeId?: number
  sourceType?: number
  status?: number
}

/** 选项查询过滤条件：生成条码时按业务对象过滤可用规则。 */
export interface BarcodeApplicationRuleOptionReq {
  objectType?: number
  productId?: number
  materialId?: number
  barcodeTypeId?: number
}

/** 分页查询条码应用规则。 */
export function getBarcodeApplicationRulePage(
  params: BarcodeApplicationRulePageParams,
): Promise<PageResult<BarcodeApplicationRule>> {
  // 查询规则列表，分页和对象类型等条件由后端拼接动态查询。
  return get('/barcode/application_rules/page', params)
}

/** 查询生成条码时可用的启用应用规则选项，默认规则在前。 */
export function getBarcodeApplicationRuleOptions(
  params: BarcodeApplicationRuleOptionReq,
): Promise<BarcodeApplicationRule[]> {
  // 查询生成条码实际可用的启用规则，后端负责默认规则排序和关联状态校验。
  return get('/barcode/application_rules/options', params)
}

/** 查询应用规则详情。 */
export function getBarcodeApplicationRule(id: number): Promise<BarcodeApplicationRule> {
  // 读取单条应用规则，供编辑表单回显。
  return get(`/barcode/application_rules/${id}`)
}

/** 新增条码应用规则。 */
export function createBarcodeApplicationRule(
  data: BarcodeApplicationRuleSaveReq,
): Promise<number> {
  // 创建应用规则，后端校验对象、条码类型、规则和模板之间的兼容性。
  return post('/barcode/application_rules', data)
}

/** 修改条码应用规则。 */
export function updateBarcodeApplicationRule(
  id: number,
  data: BarcodeApplicationRuleSaveReq,
): Promise<void> {
  // 更新规则配置，后端维护并发版本和业务引用约束。
  return put(`/barcode/application_rules/${id}`, data)
}

/** 启用应用规则，启用前校验类型/规则/模板均启用。 */
export function enableBarcodeApplicationRule(id: number): Promise<void> {
  // 启用前后端会再次确认条码类型、规则和模板均可用。
  return put(`/barcode/application_rules/${id}/enable`)
}

/** 停用应用规则。 */
export function disableBarcodeApplicationRule(id: number): Promise<void> {
  // 停用规则后，生成条码选项查询不会再返回该规则。
  return put(`/barcode/application_rules/${id}/disable`)
}

/** 删除未使用的应用规则。 */
export function deleteBarcodeApplicationRule(id: number): Promise<void> {
  // 仅未被业务使用的规则允许逻辑删除，引用检查不在前端重复实现。
  return del(`/barcode/application_rules/${id}`)
}
