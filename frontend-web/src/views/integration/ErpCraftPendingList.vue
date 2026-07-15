<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { statusMapToOptions } from '@/constants/production'
import {
  ERP_CRAFT_PENDING_STATUS,
  ERP_CRAFT_PENDING_STATUS_MAP,
  ERP_CRAFT_ROLES,
} from '@/constants/integration'
import {
  confirmErpCraftPending,
  getErpCraftPendingPage,
  rejectErpCraftPending,
  syncErpCrafts,
} from '@/api/integration/erp'
import type { ErpCraftPending, ErpCraftPendingPageParams } from '@/api/integration/erp'

defineOptions({ name: 'ErpCraftPendingList' })

// ---------- 待确认列表 ----------

const filterFields: FilterField[] = [
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: statusMapToOptions(ERP_CRAFT_PENDING_STATUS_MAP),
  },
  { prop: 'sourceSystem', label: '来源系统', type: 'input' },
  { prop: 'erpRoutingCode', label: '路线编码', type: 'input' },
]

const columns: ColumnDef<ErpCraftPending>[] = [
  { prop: 'sourceSystem', label: '来源系统', width: 110 },
  { prop: 'erpRoutingCode', label: '路线编码', width: 140 },
  { prop: 'erpRoutingName', label: '路线名称', minWidth: 150 },
  { prop: 'erpRoutingVersion', label: '版本', width: 90 },
  { prop: 'productCode', label: '产品编码', width: 130 },
  { prop: 'status', label: '状态', width: 100, statusMap: ERP_CRAFT_PENDING_STATUS_MAP },
  {
    prop: 'confirmedRouteId',
    label: '生成路线 ID',
    width: 120,
    align: 'right',
    formatter: (row) => (row.confirmedRouteId !== null ? String(row.confirmedRouteId) : '-'),
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

const rowActions: RowAction<ErpCraftPending>[] = [
  {
    key: 'confirm',
    label: '确认',
    type: 'primary',
    roles: ERP_CRAFT_ROLES,
    confirm: '确认后将生成 MES 工艺路线草稿，确认？',
    show: (row) => row.status === ERP_CRAFT_PENDING_STATUS.PENDING,
  },
  {
    key: 'reject',
    label: '驳回',
    type: 'warning',
    roles: ERP_CRAFT_ROLES,
    show: (row) => row.status === ERP_CRAFT_PENDING_STATUS.PENDING,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  ErpCraftPending,
  ErpCraftPendingPageParams
>({ fetcher: getErpCraftPendingPage })

// ---------- 触发工艺同步 ----------

interface SyncForm {
  sourceSystem: string
}

const syncDialog = useFormDialog<SyncForm>(
  () => ({ sourceSystem: '' }),
  {
    titles: { create: '触发 ERP 工艺同步' },
    submit: async (model) => {
      const result = await syncErpCrafts(model.sourceSystem ? { sourceSystem: model.sourceSystem } : {})
      ElMessage.success(
        `同步完成：共 ${result.totalCount} 条，成功 ${result.successCount} / 失败 ${result.failureCount} / 重复 ${result.duplicateCount}`,
      )
    },
    onSuccess: refresh,
  },
)

// ---------- 驳回（必填原因） ----------

interface RejectForm {
  id?: number
  routingText: string
  reason: string
}

const rejectDialog = useFormDialog<RejectForm>(
  () => ({ routingText: '', reason: '' }),
  {
    titles: { edit: '驳回工艺待确认' },
    submit: async (model) => {
      await rejectErpCraftPending(model.id!, model.reason)
      ElMessage.success('已驳回')
    },
    onSuccess: refresh,
  },
)

const rejectRules = {
  reason: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }],
}

// ---------- 行操作 ----------

async function handleRowAction(key: string, row: ErpCraftPending) {
  try {
    if (key === 'confirm') {
      const routeId = await confirmErpCraftPending(row.id)
      ElMessage.success(`已确认，生成工艺路线 #${routeId}，可前往「工艺路线」查看`)
      await refresh()
    } else if (key === 'reject') {
      rejectDialog.open('edit', {
        id: row.id,
        routingText: `${row.erpRoutingCode} ${row.erpRoutingName}`,
        reason: '',
      })
    }
  } catch {
    // 失败提示由拦截器统一弹出
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="ERP 工艺待确认"
      description="读取 ERP 工艺路线并逐条确认生成 MES 工艺路线草稿；异常行展示错误码与原因，可驳回后重新同步"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="160"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="ERP_CRAFT_ROLES" type="primary" @click="syncDialog.open()">
          触发工艺同步
        </PermissionButton>
      </template>
    </FilterTable>

    <!-- 触发同步 -->
    <FormDialog
      v-model:visible="syncDialog.visible.value"
      :title="syncDialog.title.value"
      :model="syncDialog.model.value"
      :submit-loading="syncDialog.submitLoading.value"
      width="480px"
      @submit="syncDialog.handleSubmit"
    >
      <el-form-item label="来源系统" prop="sourceSystem">
        <el-input
          v-model="syncDialog.model.value.sourceSystem"
          maxlength="32"
          placeholder="选填，如 ERP"
        />
      </el-form-item>
    </FormDialog>

    <!-- 驳回 -->
    <FormDialog
      v-model:visible="rejectDialog.visible.value"
      :title="rejectDialog.title.value"
      :model="rejectDialog.model.value"
      :rules="rejectRules"
      :submit-loading="rejectDialog.submitLoading.value"
      width="480px"
      @submit="rejectDialog.handleSubmit"
    >
      <el-form-item label="工艺路线">
        <span>{{ rejectDialog.model.value.routingText }}</span>
      </el-form-item>
      <el-form-item label="驳回原因" prop="reason">
        <el-input
          v-model="rejectDialog.model.value.reason"
          type="textarea"
          :rows="3"
          maxlength="512"
          placeholder="必填，驳回后可重新同步覆盖"
        />
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
