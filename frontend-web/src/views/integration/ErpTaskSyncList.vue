<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { statusMapToOptions } from '@/constants/production'
import {
  ERP_TASK_DETAIL_STATUS_MAP,
  ERP_TASK_ROLES,
  WRITE_STATUS_MAP,
} from '@/constants/integration'
import { getErpTaskSyncLogPage, syncErpTasks } from '@/api/integration/erp'
import type { ErpTaskSyncReq, ErpTaskSyncResp } from '@/api/integration/erp'
import type { ErpSyncLogPageParams } from '@/api/integration/erp'
import type { IntegrationWriteLog } from '@/api/integration/log'

defineOptions({ name: 'ErpTaskSyncList' })

// ---------- 同步日志列表 ----------

const filterFields: FilterField[] = [
  { prop: 'sourceSystem', label: '来源系统', type: 'input' },
  { prop: 'businessKey', label: '业务单号', type: 'input' },
  {
    prop: 'writeStatus',
    label: '写入状态',
    type: 'select',
    options: statusMapToOptions(WRITE_STATUS_MAP),
  },
]

const columns: ColumnDef<IntegrationWriteLog>[] = [
  { prop: 'businessKey', label: 'ERP 任务单号', minWidth: 150 },
  { prop: 'sourceSystem', label: '来源系统', width: 120 },
  { prop: 'writeStatus', label: '写入状态', width: 100, statusMap: WRITE_STATUS_MAP },
  {
    prop: 'resultNo',
    label: 'MES 工单号',
    width: 150,
    formatter: (row) => row.resultNo ?? '-',
  },
  {
    prop: 'resultId',
    label: '工单 ID',
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
  { prop: 'createTime', label: '同步时间', width: 170 },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  IntegrationWriteLog,
  ErpSyncLogPageParams
>({ fetcher: getErpTaskSyncLogPage })

// ---------- 触发同步 ----------

interface SyncForm {
  sourceSystem: string
  erpOrderNo: string
  timeRange: [string, string] | null
}

const syncDialog = useFormDialog<SyncForm>(
  () => ({ sourceSystem: '', erpOrderNo: '', timeRange: null }),
  {
    titles: { create: '触发 ERP 任务同步' },
    submit: async (model) => {
      const params: ErpTaskSyncReq = {}
      if (model.sourceSystem) params.sourceSystem = model.sourceSystem
      if (model.erpOrderNo) params.erpOrderNo = model.erpOrderNo
      if (Array.isArray(model.timeRange) && model.timeRange.length === 2) {
        params.startTime = `${model.timeRange[0]} 00:00:00`
        params.endTime = `${model.timeRange[1]} 23:59:59`
      }
      const result = await syncErpTasks(params)
      syncResult.value = result
      resultVisible.value = true
      ElMessage.success(
        `同步完成：成功 ${result.successCount} / 失败 ${result.failureCount} / 重复 ${result.duplicateCount}`,
      )
    },
    onSuccess: refresh,
  },
)

// ---------- 同步结果对话框 ----------

const resultVisible = ref(false)
const syncResult = ref<ErpTaskSyncResp>()
</script>

<template>
  <div class="page">
    <PageHeader
      title="ERP 任务同步"
      description="触发 ERP 生产任务单同步并查看逐条处理结果；同步日志按来源系统/业务单号/写入状态筛选"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
    >
      <template #toolbar>
        <PermissionButton :roles="ERP_TASK_ROLES" type="primary" @click="syncDialog.open()">
          触发同步
        </PermissionButton>
      </template>
    </FilterTable>

    <!-- 触发同步表单 -->
    <FormDialog
      v-model:visible="syncDialog.visible.value"
      :title="syncDialog.title.value"
      :model="syncDialog.model.value"
      :submit-loading="syncDialog.submitLoading.value"
      width="520px"
      @submit="syncDialog.handleSubmit"
    >
      <el-form-item label="来源系统" prop="sourceSystem">
        <el-input
          v-model="syncDialog.model.value.sourceSystem"
          maxlength="32"
          placeholder="选填，如 ERP"
        />
      </el-form-item>
      <el-form-item label="ERP 单号" prop="erpOrderNo">
        <el-input
          v-model="syncDialog.model.value.erpOrderNo"
          maxlength="64"
          placeholder="选填，指定单号时仅同步该单"
        />
      </el-form-item>
      <el-form-item label="时间范围" prop="timeRange">
        <el-date-picker
          v-model="syncDialog.model.value.timeRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="full-width"
        />
      </el-form-item>
    </FormDialog>

    <!-- 同步结果 -->
    <el-dialog
      v-model="resultVisible"
      title="同步结果"
      width="780px"
      destroy-on-close
      append-to-body
    >
      <template v-if="syncResult">
        <el-row :gutter="16" class="sync-summary">
          <el-col :span="6">
            <div class="sync-summary__item">
              <span class="sync-summary__label">来源系统</span>
              <span class="sync-summary__value">{{ syncResult.sourceSystem || '-' }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="sync-summary__item">
              <span class="sync-summary__label">总数</span>
              <span class="sync-summary__value">{{ syncResult.totalCount }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="sync-summary__item">
              <span class="sync-summary__label">成功</span>
              <span class="sync-summary__value sync-summary__value--success">
                {{ syncResult.successCount }}
              </span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="sync-summary__item">
              <span class="sync-summary__label">失败/重复</span>
              <span class="sync-summary__value">
                {{ syncResult.failureCount }} / {{ syncResult.duplicateCount }}
              </span>
            </div>
          </el-col>
        </el-row>
        <el-table :data="syncResult.details" border size="small" max-height="360">
          <el-table-column prop="erpOrderNo" label="ERP 任务单号" min-width="150" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <StatusTag :status="row.status" :status-map="ERP_TASK_DETAIL_STATUS_MAP" />
            </template>
          </el-table-column>
          <el-table-column prop="workOrderNo" label="生成工单号" width="150">
            <template #default="{ row }">{{ row.workOrderNo ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="workOrderId" label="工单 ID" width="90" align="right">
            <template #default="{ row }">
              {{ row.workOrderId !== null ? row.workOrderId : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="errorCode" label="错误码" width="130">
            <template #default="{ row }">{{ row.errorCode ?? '-' }}</template>
          </el-table-column>
          <el-table-column
            prop="errorMessage"
            label="错误信息"
            min-width="160"
            show-overflow-tooltip
          >
            <template #default="{ row }">{{ row.errorMessage ?? '-' }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}

.sync-summary {
  margin-bottom: 16px;
}

.sync-summary__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sync-summary__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.sync-summary__value {
  font-size: 18px;
  font-weight: 600;
}

.sync-summary__value--success {
  color: var(--el-color-success);
}
</style>
