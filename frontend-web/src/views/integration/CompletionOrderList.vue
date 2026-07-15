<script setup lang="ts">
import type { ColumnDef, FilterField } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import { COMPLETION_AUDIT_STATUS_MAP } from '@/constants/integration'
import {
  getCompletionOrderPage,
  getCompletionReadLogPage,
} from '@/api/integration/completion'
import type {
  CompletionOrder,
  CompletionOrderPageParams,
  CompletionReadLog,
  CompletionReadLogPageParams,
} from '@/api/integration/completion'

defineOptions({ name: 'CompletionOrderList' })

const DEFAULT_SOURCE_SYSTEM = 'MES'

// ---------- Tab1 已审核完工单 ----------

const orderFilterFields: FilterField[] = [
  {
    prop: 'sourceSystem',
    label: '来源系统',
    type: 'input',
    defaultValue: DEFAULT_SOURCE_SYSTEM,
  },
  { prop: 'completionNo', label: '完工单号', type: 'input' },
  { prop: 'workOrderNo', label: '工单号', type: 'input' },
  { prop: 'timeRange', label: '审核时间', type: 'dateRange', span: 8 },
]

const orderColumns: ColumnDef<CompletionOrder>[] = [
  { prop: 'completionNo', label: '完工单号', width: 150 },
  { prop: 'workOrderNo', label: '工单号', width: 140 },
  { prop: 'productCode', label: '产品编码', width: 130 },
  { prop: 'productName', label: '产品名称', minWidth: 150 },
  { prop: 'batchNo', label: '批次号', width: 120 },
  { prop: 'completionQuantity', label: '完工数', width: 90, align: 'right' },
  { prop: 'goodQuantity', label: '良品数', width: 90, align: 'right' },
  { prop: 'defectQuantity', label: '不良数', width: 90, align: 'right' },
  { prop: 'auditStatus', label: '审核状态', width: 100, statusMap: COMPLETION_AUDIT_STATUS_MAP },
  { prop: 'auditTime', label: '审核时间', width: 170 },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const {
  data: orderData,
  loading: orderLoading,
  pagination: orderPagination,
  query: orderQuery,
  reset: orderReset,
  onPageChange: onOrderPageChange,
} = useTable<CompletionOrder, CompletionOrderPageParams>({
  fetcher: getCompletionOrderPage,
  defaultParams: { sourceSystem: DEFAULT_SOURCE_SYSTEM },
})

/** dateRange 拆为 startTime/endTime；sourceSystem 后端必填，空则回退默认 */
function handleOrderQuery(params: Record<string, any>) {
  const { timeRange, ...rest } = params
  rest.sourceSystem = rest.sourceSystem || DEFAULT_SOURCE_SYSTEM
  if (Array.isArray(timeRange) && timeRange.length === 2) {
    rest.startTime = `${timeRange[0]} 00:00:00`
    rest.endTime = `${timeRange[1]} 23:59:59`
  }
  orderQuery(rest as CompletionOrderPageParams)
}

// ---------- Tab2 读取日志 ----------

const logFilterFields: FilterField[] = [
  { prop: 'sourceSystem', label: '来源系统', type: 'input' },
  { prop: 'completionNo', label: '完工单号', type: 'input' },
  { prop: 'timeRange', label: '读取时间', type: 'dateRange', span: 8 },
]

const logColumns: ColumnDef<CompletionReadLog>[] = [
  { prop: 'completionNo', label: '完工单号', width: 150 },
  { prop: 'workOrderNo', label: '工单号', width: 140 },
  { prop: 'sourceSystem', label: '来源系统', width: 120 },
  {
    prop: 'readBy',
    label: '读取用户 ID',
    width: 120,
    align: 'right',
    formatter: (row) => (row.readBy !== null ? String(row.readBy) : '-'),
  },
  { prop: 'readTime', label: '读取时间', width: 170 },
]

const {
  data: logData,
  loading: logLoading,
  pagination: logPagination,
  query: logQuery,
  reset: logReset,
  onPageChange: onLogPageChange,
} = useTable<CompletionReadLog, CompletionReadLogPageParams>({
  fetcher: getCompletionReadLogPage,
})

function handleLogQuery(params: Record<string, any>) {
  const { timeRange, ...rest } = params
  if (Array.isArray(timeRange) && timeRange.length === 2) {
    rest.startTime = `${timeRange[0]} 00:00:00`
    rest.endTime = `${timeRange[1]} 23:59:59`
  }
  logQuery(rest as CompletionReadLogPageParams)
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="完工单读取"
      description="面向外部系统的已审核完工单查询接口，每次查询都会逐条记录读取日志"
    />
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="该接口面向外部系统，每次查询都会逐条记录读取日志，请按需查询。"
      class="page-alert"
    />
    <el-tabs class="page-tabs">
      <el-tab-pane label="已审核完工单" name="orders">
        <FilterTable
          :filter-fields="orderFilterFields"
          :columns="orderColumns"
          :data="orderData"
          :loading="orderLoading"
          :pagination="orderPagination"
          show-index
          @query="handleOrderQuery"
          @reset="orderReset"
          @page-change="onOrderPageChange"
        />
      </el-tab-pane>
      <el-tab-pane label="读取日志" name="logs">
        <FilterTable
          :filter-fields="logFilterFields"
          :columns="logColumns"
          :data="logData"
          :loading="logLoading"
          :pagination="logPagination"
          show-index
          @query="handleLogQuery"
          @reset="logReset"
          @page-change="onLogPageChange"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.page-alert {
  margin-bottom: 16px;
}

.page-tabs :deep(.el-tabs__content) {
  overflow: visible;
}
</style>
