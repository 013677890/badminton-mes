<script setup lang="ts">
import { ref } from 'vue'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import { useTable } from '@/composables/useTable'
import {
  SCENE_BATCH_STATUS_MAP,
  SCENE_BATCH_STATUS_OPTIONS,
  YES_NO_OPTIONS,
} from '@/constants/scene'
import {
  getProductStatusHistories,
  getProductStatusOperationHistories,
  getProductStatusPage,
} from '@/api/scene/management'
import type {
  SceneProductStatus,
  SceneProductStatusPageParams,
  SceneStatusHistory,
  SceneProcessHistory,
} from '@/api/scene/management'
import { PROCESS_ACTION_TYPE_TEXT } from '@/constants/report'

defineOptions({ name: 'SceneProductStatusList' })

const filterFields: FilterField[] = [
  { prop: 'batchNo', label: '批次号', type: 'input' },
  { prop: 'taskId', label: '任务ID', type: 'input' },
  { prop: 'batchStatus', label: '批次状态', type: 'select', options: SCENE_BATCH_STATUS_OPTIONS },
  {
    prop: 'abnormal',
    label: '异常',
    type: 'select',
    options: YES_NO_OPTIONS,
  },
]

const columns: ColumnDef<SceneProductStatus>[] = [
  { prop: 'batchNo', label: '批次号', width: 160 },
  { prop: 'taskId', label: '任务ID', width: 90 },
  { prop: 'currentProcessName', label: '当前工序', minWidth: 120 },
  { prop: 'batchStatus', label: '批次状态', width: 100, statusMap: SCENE_BATCH_STATUS_MAP },
  {
    prop: 'abnormal',
    label: '异常',
    width: 70,
    align: 'center',
    formatter: (row) => (row.abnormal ? '是' : '否'),
  },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<SceneProductStatus>[] = [
  { key: 'histories', label: '状态履历' },
  { key: 'operations', label: '作业履历' },
]

const { data, loading, pagination, query, reset, onPageChange } = useTable<
  SceneProductStatus,
  SceneProductStatusPageParams
>({ fetcher: getProductStatusPage })

/** 状态变更履历弹窗 */
const historyVisible = ref(false)
const historyLoading = ref(false)
const statusHistories = ref<SceneStatusHistory[]>([])

/** 工序作业履历弹窗 */
const operationVisible = ref(false)
const operationLoading = ref(false)
const operationHistories = ref<SceneProcessHistory[]>([])

async function handleRowAction(key: string, row: SceneProductStatus) {
  if (key === 'histories') {
    historyVisible.value = true
    historyLoading.value = true
    try {
      statusHistories.value = await getProductStatusHistories(row.id)
    } finally {
      historyLoading.value = false
    }
  } else if (key === 'operations') {
    operationVisible.value = true
    operationLoading.value = true
    try {
      operationHistories.value = await getProductStatusOperationHistories(row.id)
    } finally {
      operationLoading.value = false
    }
  }
}

function normalizeParams(params: Record<string, any>): SceneProductStatusPageParams {
  const next = { ...params }
  if (next.taskId) next.taskId = Number(next.taskId)
  if (next.abnormal === 'true') next.abnormal = true
  else if (next.abnormal === 'false') next.abnormal = false
  return next as SceneProductStatusPageParams
}
</script>

<template>
  <div class="page">
    <PageHeader title="产品批次状态" description="跟踪产品批次的当前工序位置、批次状态和异常标记" />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="180"
      show-index
      @query="(params) => query(normalizeParams(params))"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    />

    <!-- 状态变更履历 -->
    <el-dialog v-model="historyVisible" title="批次状态变更履历" width="680px" destroy-on-close>
      <el-table v-loading="historyLoading" :data="statusHistories" border size="small">
        <el-table-column label="原状态" prop="fromStatus" width="90" />
        <el-table-column label="新状态" prop="toStatus" width="90" />
        <el-table-column label="工序ID" prop="processId" width="90" />
        <el-table-column label="变更原因" prop="changeReason" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作人" prop="operatorId" width="90" />
        <el-table-column label="操作时间" prop="operateTime" width="170" />
      </el-table>
    </el-dialog>

    <!-- 工序作业履历 -->
    <el-dialog v-model="operationVisible" title="工序作业履历" width="720px" destroy-on-close>
      <el-table v-loading="operationLoading" :data="operationHistories" border size="small">
        <el-table-column label="派工明细ID" prop="dispatchDetailId" width="110" />
        <el-table-column label="工序编码" prop="processCode" width="120" />
        <el-table-column label="工序名称" prop="processName" min-width="120" />
        <el-table-column label="动作类型" width="90">
          <template #default="{ row }">
            {{ PROCESS_ACTION_TYPE_TEXT[row.actionType] ?? row.actionType }}
          </template>
        </el-table-column>
        <el-table-column label="操作人" prop="operatorId" width="90" />
        <el-table-column label="操作原因" prop="actionReason" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作时间" prop="operateTime" width="170" />
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
