import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { OptionItem } from '@/types/components'
import { useTable } from './useTable'
import type { PageParam, PageResult } from '@/utils/request'
import {
  loadLineOptions,
  loadProductOptions,
  loadWorkshopOptions,
} from '@/api/production/options'
import type {
  ProductionReportDetail,
  ProductionReportSummary,
  ReportQueryReq,
} from '@/api/report'

/**
 * 产量报表与车间时段报表共享的查询/汇总/明细/导出逻辑。
 * 两者查询条件、汇总口径与明细结构完全一致，仅后端接口不同，故抽公共 composable。
 */

interface ProductionStyleReportApi {
  summary: (params: ReportQueryReq) => Promise<ProductionReportSummary>
  details: (params: ReportQueryReq & PageParam) => Promise<PageResult<ProductionReportDetail>>
  exportFile: (params: ReportQueryReq) => Promise<void>
}

/** 查询表单模型 */
export interface ReportQueryForm {
  startTime: string
  endTime: string
  workshopId?: number
  lineId?: number
  productId?: number
  batchNo?: string
}

export function useProductionStyleReport(api: ProductionStyleReportApi) {
  const queryParams = reactive<ReportQueryForm>({
    startTime: '',
    endTime: '',
    workshopId: undefined,
    lineId: undefined,
    productId: undefined,
    batchNo: '',
  })

  const workshopOptions = ref<OptionItem[]>([])
  const lineOptions = ref<OptionItem[]>([])
  const productOptions = ref<OptionItem[]>([])

  onMounted(async () => {
    const [workshops, products] = await Promise.all([
      loadWorkshopOptions(),
      loadProductOptions(),
    ])
    workshopOptions.value = workshops
    productOptions.value = products
  })

  /** 车间变更联动产线选项 */
  async function onWorkshopChange() {
    queryParams.lineId = undefined
    lineOptions.value = await loadLineOptions(queryParams.workshopId)
  }

  const summary = ref<ProductionReportSummary | null>(null)
  const summaryLoading = ref(false)

  const {
    data,
    loading,
    pagination,
    query,
    onPageChange,
  } = useTable<ProductionReportDetail, ReportQueryReq>({
    fetcher: (params) => api.details(params),
    immediate: false,
  })

  /** 剔除空值，构造后端查询条件 */
  function buildQuery(): ReportQueryReq {
    return {
      startTime: queryParams.startTime,
      endTime: queryParams.endTime,
      workshopId: queryParams.workshopId || undefined,
      lineId: queryParams.lineId || undefined,
      productId: queryParams.productId || undefined,
      batchNo: queryParams.batchNo?.trim() || undefined,
    }
  }

  function validateTimeRange(): boolean {
    if (!queryParams.startTime || !queryParams.endTime) {
      ElMessage.warning('请选择开始时间和结束时间')
      return false
    }
    if (queryParams.startTime > queryParams.endTime) {
      ElMessage.warning('开始时间不能晚于结束时间')
      return false
    }
    return true
  }

  /** 查询：先校验，再拉汇总 + 明细分页 */
  async function handleQuery() {
    if (!validateTimeRange()) return
    const req = buildQuery()
    summaryLoading.value = true
    try {
      summary.value = await api.summary(req)
    } finally {
      summaryLoading.value = false
    }
    await query(req)
  }

  /** 重置：清空表单与结果，不发请求 */
  function handleReset() {
    queryParams.startTime = ''
    queryParams.endTime = ''
    queryParams.workshopId = undefined
    queryParams.lineId = undefined
    queryParams.productId = undefined
    queryParams.batchNo = ''
    summary.value = null
    data.value = []
    pagination.value = { pageNo: 1, pageSize: pagination.value.pageSize, total: 0 }
  }

  const exporting = ref(false)
  async function handleExport() {
    if (!validateTimeRange()) return
    exporting.value = true
    try {
      await api.exportFile(buildQuery())
      ElMessage.success('导出成功')
    } finally {
      exporting.value = false
    }
  }

  return {
    queryParams,
    workshopOptions,
    lineOptions,
    productOptions,
    onWorkshopChange,
    summary,
    summaryLoading,
    data,
    loading,
    pagination,
    onPageChange,
    exporting,
    handleQuery,
    handleReset,
    handleExport,
  }
}

/** 汇总卡片展示项（供视图构建卡片列表复用） */
export interface SummaryMetric {
  label: string
  value: number | string
  unit?: string
}

/** 汇总指标卡片列表（产量/时段报表通用） */
export function buildProductionSummaryMetrics(
  summary: ProductionReportSummary,
): SummaryMetric[] {
  return [
    { label: '计划数量', value: summary.planQuantity },
    { label: '投入数量', value: summary.inputQuantity },
    { label: '合格数量', value: summary.goodQuantity },
    { label: '不良数量', value: summary.defectQuantity },
    { label: '返工数量', value: summary.reworkQuantity },
    { label: '完工数量', value: summary.finishQuantity },
    { label: '完成率', value: formatRate(summary.completionRate), unit: '%' },
    { label: '不良率', value: formatRate(summary.defectRate), unit: '%' },
  ]
}

/** BigDecimal 比率保留两位小数展示 */
function formatRate(rate: number): string {
  if (rate === null || rate === undefined || Number.isNaN(rate)) return '--'
  return (rate * 100).toFixed(2)
}
