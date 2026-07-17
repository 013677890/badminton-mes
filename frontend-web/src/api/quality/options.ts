import type { OptionItem } from '@/types/components'
import { PLAN_STATUS } from '@/constants/quality'
import { getInspectionCategoryPage } from './inspectionCategory'
import { getInspectionItemPage } from './inspectionItem'
import { getInspectionPlanPage } from './inspectionPlan'

/** 质量基础数据量较小，按后端分页上限一次加载表单选项。 */
const FULL_PAGE = { pageNo: 1, pageSize: 100 }

/** 加载启用的检验分类选项。 */
export async function loadInspectionCategoryOptions(): Promise<OptionItem[]> {
  const page = await getInspectionCategoryPage({ ...FULL_PAGE, enabledStatus: 1 })
  return page.list.map((category) => ({
    label: `${category.categoryCode} ${category.categoryName}`,
    value: category.id,
  }))
}

/** 加载启用的检验项目选项，可按检验分类过滤。 */
export async function loadInspectionItemOptions(categoryId?: number): Promise<OptionItem[]> {
  const page = await getInspectionItemPage({ ...FULL_PAGE, categoryId, enabledStatus: 1 })
  return page.list.map((item) => ({
    label: `${item.itemCode} ${item.itemName}`,
    value: item.id,
  }))
}

/** 加载生效的检验方案选项，可按检验类型、产品和客户过滤。 */
export async function loadEffectiveInspectionPlanOptions(filters: {
  inspectionType?: string
  productId?: number
  customerId?: number
} = {}): Promise<OptionItem[]> {
  const page = await getInspectionPlanPage({
    ...FULL_PAGE,
    ...filters,
    planStatus: PLAN_STATUS.EFFECTIVE,
  })
  return page.list.map((plan) => ({
    label: `${plan.planCode} ${plan.planName}（V${plan.versionNo}）`,
    value: plan.id,
  }))
}
