<script setup lang="ts">
import { ref } from 'vue'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import { formatSnapshot } from '@/utils/format'
import {
  INTERFACE_TYPE_OPTIONS,
  INTERFACE_TYPE_TEXT,
  WRITE_STATUS_MAP,
} from '@/constants/integration'
import { statusMapToOptions } from '@/constants/production'
import { getWriteLogPage } from '@/api/integration/log'
import type {
  IntegrationWriteLog,
  IntegrationWriteLogPageParams,
} from '@/api/integration/log'

defineOptions({ name: 'WriteLogList' })

// ---------- 列表 ----------

const filterFields: FilterField[] = [
  {
    prop: 'interfaceType',
    label: '接口类型',
    type: 'select',
    options: INTERFACE_TYPE_OPTIONS,
  },
  { prop: 'sourceSystem', label: '来源系统', type: 'input' },
  { prop: 'businessKey', label: '业务键', type: 'input' },
  {
    prop: 'writeStatus',
    label: '写入状态',
    type: 'select',
    options: statusMapToOptions(WRITE_STATUS_MAP),
  },
]

const columns: ColumnDef<IntegrationWriteLog>[] = [
  {
    prop: 'interfaceType',
    label: '接口类型',
    width: 140,
    formatter: (row) => INTERFACE_TYPE_TEXT[row.interfaceType] ?? row.interfaceType,
  },
  { prop: 'sourceSystem', label: '来源系统', width: 120 },
  { prop: 'businessKey', label: '业务键', minWidth: 150 },
  { prop: 'writeStatus', label: '写入状态', width: 100, statusMap: WRITE_STATUS_MAP },
  {
    prop: 'resultNo',
    label: '业务编号',
    width: 150,
    formatter: (row) => row.resultNo ?? '-',
  },
  {
    prop: 'resultId',
    label: '业务 ID',
    width: 90,
    align: 'right',
    formatter: (row) => (row.resultId !== null ? String(row.resultId) : '-'),
  },
  { prop: 'errorCode', label: '错误码', width: 130, formatter: (row) => row.errorCode ?? '-' },
  {
    prop: 'errorMessage',
    label: '错误信息',
    minWidth: 160,
    showOverflowTooltip: true,
    formatter: (row) => row.errorMessage ?? '-',
  },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const rowActions: RowAction<IntegrationWriteLog>[] = [
  { key: 'snapshot', label: '查看快照' },
]

const { data, loading, pagination, query, reset, onPageChange } = useTable<
  IntegrationWriteLog,
  IntegrationWriteLogPageParams
>({ fetcher: getWriteLogPage })

// ---------- 请求快照抽屉 ----------

const drawerVisible = ref(false)
const current = ref<IntegrationWriteLog>()

function openSnapshot(row: IntegrationWriteLog) {
  current.value = row
  drawerVisible.value = true
}

async function handleRowAction(key: string, row: IntegrationWriteLog) {
  if (key === 'snapshot') openSnapshot(row)
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="外部写入日志"
      description="所有外部标准写入接口的调用留痕：按接口类型/来源系统/业务键/写入状态筛选，行内查看请求 JSON 快照"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="100"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    />

    <!-- 请求快照抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="请求快照"
      size="640px"
      destroy-on-close
    >
      <template v-if="current">
        <el-descriptions :column="1" border size="small" class="snapshot-meta">
          <el-descriptions-item label="接口类型">
            {{ INTERFACE_TYPE_TEXT[current.interfaceType] ?? current.interfaceType }}
          </el-descriptions-item>
          <el-descriptions-item label="来源系统">{{ current.sourceSystem }}</el-descriptions-item>
          <el-descriptions-item label="业务键">{{ current.businessKey }}</el-descriptions-item>
          <el-descriptions-item label="写入状态">
            {{ WRITE_STATUS_MAP[current.writeStatus]?.text ?? current.writeStatus }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="snapshot-title">请求 JSON 快照</div>
        <pre class="snapshot-body">{{ formatSnapshot(current.requestSnapshot) }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.snapshot-meta {
  margin-bottom: 16px;
}

.snapshot-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.snapshot-body {
  max-height: 60vh;
  padding: 12px;
  overflow: auto;
  font-family: 'Cascadia Code', Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
