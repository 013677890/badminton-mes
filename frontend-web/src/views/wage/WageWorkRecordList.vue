<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { ColumnDef, FilterField, OptionItem } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import { ROLE_SEED_IDS } from '@/constants/production'
import {
  loadProcessOptions,
  loadProductOptions,
  loadRoleUserOptions,
} from '@/api/production/options'
import { getWorkRecordPage } from '@/api/wage/workRecord'
import type { WageWorkRecord, WageWorkRecordPageParams } from '@/api/wage/workRecord'

defineOptions({ name: 'WageWorkRecordList' })

// ---------- 下拉选项 ----------

const employeeOptions = ref<OptionItem[]>([])
const processOptions = ref<OptionItem[]>([])
const productOptions = ref<OptionItem[]>([])

const filterFields = ref<FilterField[]>([
  { prop: 'employeeId', label: '员工', type: 'select', options: [] },
  { prop: 'processId', label: '工序', type: 'select', options: [] },
  { prop: 'productId', label: '产品', type: 'select', options: [] },
  { prop: 'workOrderId', label: '工单 ID', type: 'input', placeholder: '生产工单主键' },
  { prop: 'workDateRange', label: '作业日期', type: 'dateRange', span: 8 },
])

onMounted(async () => {
  try {
    // 计件员工主要是操作工/班组长，两个角色的用户合并去重
    const [operators, leaders, processes, products] = await Promise.all([
      loadRoleUserOptions(ROLE_SEED_IDS.OPERATOR),
      loadRoleUserOptions(ROLE_SEED_IDS.TEAM_LEADER),
      loadProcessOptions(),
      loadProductOptions(),
    ])
    const merged = new Map<string | number, OptionItem>()
    for (const opt of [...operators, ...leaders]) merged.set(opt.value, opt)
    employeeOptions.value = [...merged.values()]
    processOptions.value = processes
    productOptions.value = products
    const fill = (prop: string, options: OptionItem[]) => {
      const field = filterFields.value.find((item) => item.prop === prop)
      if (field) field.options = options
    }
    fill('employeeId', employeeOptions.value)
    fill('processId', processes)
    fill('productId', products)
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

function optionLabel(options: OptionItem[], value: number): string {
  return String(options.find((opt) => opt.value === value)?.label ?? value)
}

// ---------- 列表（只读） ----------

const columns: ColumnDef<WageWorkRecord>[] = [
  { prop: 'workDate', label: '作业日期', width: 110 },
  {
    prop: 'employeeId',
    label: '员工',
    minWidth: 130,
    formatter: (row) => optionLabel(employeeOptions.value, row.employeeId),
  },
  {
    prop: 'processId',
    label: '工序',
    minWidth: 140,
    formatter: (row) => optionLabel(processOptions.value, row.processId),
  },
  {
    prop: 'productId',
    label: '产品',
    minWidth: 140,
    formatter: (row) => optionLabel(productOptions.value, row.productId),
  },
  { prop: 'workOrderId', label: '工单 ID', width: 90, align: 'right' },
  { prop: 'qualifiedQuantity', label: '合格数', width: 90, align: 'right' },
  { prop: 'defectQuantity', label: '不良数', width: 90, align: 'right' },
  { prop: 'sourceReportId', label: '来源报工 ID', width: 110, align: 'right' },
  { prop: 'sourceAuditTime', label: '报工审核时间', width: 170 },
]

const { data, loading, pagination, query, reset, onPageChange } = useTable<
  WageWorkRecord,
  WageWorkRecordPageParams
>({ fetcher: getWorkRecordPage })

/** dateRange 拆为后端的 begin/end 字段（workDate 为纯日期） */
function handleQuery(params: Record<string, any>) {
  const { workDateRange, ...rest } = params
  if (Array.isArray(workDateRange) && workDateRange.length === 2) {
    rest.workDateBegin = workDateRange[0]
    rest.workDateEnd = workDateRange[1]
  }
  query(rest as WageWorkRecordPageParams)
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="计件作业记录"
      description="现场报工审核后的计件快照（只读）：结算按作业日期与工序/产品匹配单价规则逐条计薪"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      show-index
      @query="handleQuery"
      @reset="reset"
      @page-change="onPageChange"
    />
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
