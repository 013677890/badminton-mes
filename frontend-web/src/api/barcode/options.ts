import type { OptionItem } from '@/types/components'
import type { BarcodeApplicationRuleOptionReq } from './applicationRule'
import { getBarcodeApplicationRuleOptions } from './applicationRule'
import { getBarcodeRulePage } from './rule'
import { getBarcodeTemplatePage } from './template'
import { getBarcodeTypeOptions } from './type'

/** 主档量级较小，一次加载 100 条启用数据并映射为通用下拉选项。 */
const FULL_PAGE = { pageNo: 1, pageSize: 100 }

/** 加载启用条码类型选项。 */
export async function loadBarcodeTypeOptions(): Promise<OptionItem[]> {
  const list = await getBarcodeTypeOptions()
  return list.map((item) => ({
    label: `${item.typeCode} ${item.typeName}`,
    value: item.id,
  }))
}

/** 加载启用条码规则选项。 */
export async function loadBarcodeRuleOptions(): Promise<OptionItem[]> {
  const page = await getBarcodeRulePage({ ...FULL_PAGE, status: 1 })
  return page.list.map((item) => ({
    label: `${item.ruleCode} ${item.ruleName}`,
    value: item.id,
  }))
}

/** 加载启用条码模板选项。 */
export async function loadBarcodeTemplateOptions(): Promise<OptionItem[]> {
  const page = await getBarcodeTemplatePage({ ...FULL_PAGE, status: 1 })
  return page.list.map((item) => ({
    label: `${item.templateCode} ${item.templateName}`,
    value: item.id,
  }))
}

/** 加载生成条码可用的启用应用规则选项，默认规则在前。 */
export async function loadBarcodeApplicationRuleOptions(
  filters?: BarcodeApplicationRuleOptionReq,
): Promise<OptionItem[]> {
  const list = await getBarcodeApplicationRuleOptions(filters ?? {})
  return list.map((item) => ({
    label: item.defaultFlag ? `【默认】对象${item.objectType}-类型${item.barcodeTypeId}` : `对象${item.objectType}-类型${item.barcodeTypeId}`,
    value: item.id,
  }))
}
