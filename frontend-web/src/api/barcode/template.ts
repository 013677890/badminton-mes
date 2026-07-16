import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 条码模板接口，对齐后端 BarcodeTemplateController（/api/barcode/templates）。 */

/** 模板字段配置（响应）。 */
export interface BarcodeTemplateField {
  id: number
  fieldName: string
  /** 字段类型：1 文本 2 条码 3 二维码 */
  fieldType: number
  dataSource: string
  posX: number
  posY: number
  fontSize: number | null
}

/** 模板字段配置（保存请求）。 */
export interface BarcodeTemplateFieldSaveReq {
  fieldName: string
  fieldType: number
  dataSource: string
  posX: number
  posY: number
  fontSize?: number | null
}

/** 条码模板响应，详情含字段配置。 */
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

/** 条码模板创建/修改请求，携带完整字段配置。 */
export interface BarcodeTemplateSaveReq {
  templateCode: string
  templateName: string
  paperWidth: number
  paperHeight: number
  fields: BarcodeTemplateFieldSaveReq[]
}

export interface BarcodeTemplatePageParams extends PageParam {
  templateCode?: string
  templateName?: string
  status?: number
}

/** 预览字段结果。 */
export interface BarcodeTemplatePreviewField {
  fieldName: string
  fieldType: number
  dataSource: string
  posX: number
  posY: number
  fontSize: number | null
  sampleContent: string | null
}

/** 模板预览请求。 */
export interface BarcodeTemplatePreviewReq {
  templateId: number
  sampleBarcodeValue?: string
  sampleData?: Record<string, string>
}

/** 模板预览响应：标签布局与逐字段展示内容。 */
export interface BarcodeTemplatePreviewResp {
  templateId: number
  templateCode: string
  version: string
  paperWidth: number
  paperHeight: number
  fields: BarcodeTemplatePreviewField[]
}

/** 分页查询标签模板（列表不含字段）。 */
export function getBarcodeTemplatePage(
  params: BarcodeTemplatePageParams,
): Promise<PageResult<BarcodeTemplate>> {
  return get('/barcode/templates/page', params)
}

/** 查询模板详情（含字段配置）。 */
export function getBarcodeTemplate(id: number): Promise<BarcodeTemplate> {
  return get(`/barcode/templates/${id}`)
}

/** 新增标签模板及字段配置。 */
export function createBarcodeTemplate(data: BarcodeTemplateSaveReq): Promise<number> {
  return post('/barcode/templates', data)
}

/** 修改标签模板，已被应用规则绑定时保留原版本并生成升版本新行。 */
export function updateBarcodeTemplate(id: number, data: BarcodeTemplateSaveReq): Promise<void> {
  return put(`/barcode/templates/${id}`, data)
}

/** 启用标签模板。 */
export function enableBarcodeTemplate(id: number): Promise<void> {
  return put(`/barcode/templates/${id}/enable`)
}

/** 停用标签模板。 */
export function disableBarcodeTemplate(id: number): Promise<void> {
  return put(`/barcode/templates/${id}/disable`)
}

/** 返回模板打印预览数据，不驱动真实打印机。 */
export function previewBarcodeTemplate(
  data: BarcodeTemplatePreviewReq,
): Promise<BarcodeTemplatePreviewResp> {
  return post('/barcode/templates/preview', data)
}
