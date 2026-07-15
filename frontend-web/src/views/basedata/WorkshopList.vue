<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  BASE_DATA_WRITE_ROLES,
  ENABLE_STATUS_MAP,
  ENABLE_STATUS_OPTIONS,
  ROLE_SEED_IDS,
} from '@/constants/production'
import { loadRoleUserOptions } from '@/api/production/options'
import {
  createWorkshop,
  deleteWorkshop,
  getWorkshopPage,
  updateWorkshop,
  updateWorkshopStatus,
} from '@/api/production/workshop'
import type { Workshop, WorkshopPageParams } from '@/api/production/workshop'

defineOptions({ name: 'WorkshopList' })

const filterFields: FilterField[] = [
  { prop: 'workshopCode', label: '车间编码', type: 'input' },
  { prop: 'workshopName', label: '车间名称', type: 'input' },
  { prop: 'status', label: '状态', type: 'select', options: ENABLE_STATUS_OPTIONS },
]

const columns: ColumnDef<Workshop>[] = [
  { prop: 'workshopCode', label: '车间编码', width: 140 },
  { prop: 'workshopName', label: '车间名称', minWidth: 160 },
  { prop: 'managerName', label: '车间主管', width: 120 },
  { prop: 'status', label: '状态', width: 80, statusMap: ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<Workshop>[] = [
  { key: 'edit', label: '编辑', roles: BASE_DATA_WRITE_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '确认启用该车间？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '停用后新建工单不可选择该车间，确认？',
    show: (row) => row.status === 1,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '存在产线或工单引用时无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  Workshop,
  WorkshopPageParams
>({ fetcher: getWorkshopPage })

/** 车间主管候选：WORKSHOP_MANAGER 角色下的启用用户 */
const managerOptions = ref<OptionItem[]>([])
onMounted(async () => {
  try {
    managerOptions.value = await loadRoleUserOptions(ROLE_SEED_IDS.WORKSHOP_MANAGER)
  } catch {
    // 加载失败不阻塞页面，仅主管下拉为空
  }
})

interface WorkshopForm {
  id?: number
  version?: number
  workshopCode: string
  workshopName: string
  managerId: number | null
  status: number
}

const dialog = useFormDialog<WorkshopForm>(
  () => ({ workshopCode: '', workshopName: '', managerId: null, status: 1 }),
  {
    titles: { create: '新增车间', edit: '编辑车间' },
    submit: async (model, mode) => {
      const payload = {
        workshopCode: model.workshopCode,
        workshopName: model.workshopName,
        managerId: model.managerId ?? undefined,
        status: model.status,
      }
      if (mode === 'create') {
        await createWorkshop(payload)
        ElMessage.success('车间已创建')
      } else {
        await updateWorkshop(model.id!, { ...payload, version: model.version! })
        ElMessage.success('车间已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  workshopCode: [
    { required: true, message: '请输入车间编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  workshopName: [{ required: true, message: '请输入车间名称', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: Workshop) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row })
    } else if (key === 'enable' || key === 'disable') {
      await updateWorkshopStatus(row.id, key === 'enable' ? 1 : 0, row.version)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用')
      await refresh()
    } else if (key === 'delete') {
      await deleteWorkshop(row.id, row.version)
      ElMessage.success('已删除')
      await refresh()
    }
  } catch {
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="车间管理" description="生产车间档案，供产线与工单归属" />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="200"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="BASE_DATA_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增车间
        </PermissionButton>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      @submit="dialog.handleSubmit"
    >
      <el-form-item label="车间编码" prop="workshopCode">
        <el-input
          v-model="dialog.model.value.workshopCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 WS-01"
        />
      </el-form-item>
      <el-form-item label="车间名称" prop="workshopName">
        <el-input v-model="dialog.model.value.workshopName" maxlength="64" />
      </el-form-item>
      <el-form-item label="车间主管" prop="managerId">
        <el-select
          v-model="dialog.model.value.managerId"
          clearable
          filterable
          placeholder="选择主管（选填）"
        >
          <el-option
            v-for="opt in managerOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="dialog.model.value.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
