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
} from '@/constants/production'
import { loadWorkshopOptions } from '@/api/production/options'
import {
  createLine,
  deleteLine,
  getLinePage,
  updateLine,
  updateLineStatus,
} from '@/api/production/line'
import type { LinePageParams, ProductionLine } from '@/api/production/line'

defineOptions({ name: 'LineList' })

/** 车间下拉在筛选与表单共用 */
const workshopOptions = ref<OptionItem[]>([])
const filterFields = ref<FilterField[]>([
  { prop: 'lineCode', label: '产线编码', type: 'input' },
  { prop: 'lineName', label: '产线名称', type: 'input' },
  { prop: 'workshopId', label: '所属车间', type: 'select', options: [] },
  { prop: 'status', label: '状态', type: 'select', options: ENABLE_STATUS_OPTIONS },
])

onMounted(async () => {
  try {
    workshopOptions.value = await loadWorkshopOptions()
    const field = filterFields.value.find((item) => item.prop === 'workshopId')
    if (field) field.options = workshopOptions.value
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

const columns: ColumnDef<ProductionLine>[] = [
  { prop: 'lineCode', label: '产线编码', width: 140 },
  { prop: 'lineName', label: '产线名称', minWidth: 150 },
  {
    prop: 'workshopName',
    label: '所属车间',
    minWidth: 140,
    formatter: (row) => (row.workshopCode ? `${row.workshopCode} ${row.workshopName ?? ''}` : '-'),
  },
  {
    prop: 'standardCapacity',
    label: '标准日产能',
    width: 110,
    align: 'right',
    formatter: (row) => (row.standardCapacity == null ? '-' : `${row.standardCapacity} 件/天`),
  },
  { prop: 'status', label: '状态', width: 80, statusMap: ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<ProductionLine>[] = [
  { key: 'edit', label: '编辑', roles: BASE_DATA_WRITE_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '确认启用该产线？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '停用后派工不可选择该产线，确认？',
    show: (row) => row.status === 1,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '被派工单引用的产线无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  ProductionLine,
  LinePageParams
>({ fetcher: getLinePage })

interface LineForm {
  id?: number
  version?: number
  lineCode: string
  lineName: string
  workshopId: number | null
  standardCapacity: number | null
  status: number
}

const dialog = useFormDialog<LineForm>(
  () => ({ lineCode: '', lineName: '', workshopId: null, standardCapacity: null, status: 1 }),
  {
    titles: { create: '新增产线', edit: '编辑产线' },
    submit: async (model, mode) => {
      const payload = {
        lineCode: model.lineCode,
        lineName: model.lineName,
        workshopId: model.workshopId!,
        standardCapacity: model.standardCapacity ?? undefined,
        status: model.status,
      }
      if (mode === 'create') {
        await createLine(payload)
        ElMessage.success('产线已创建')
      } else {
        await updateLine(model.id!, { ...payload, version: model.version! })
        ElMessage.success('产线已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  lineCode: [
    { required: true, message: '请输入产线编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  lineName: [{ required: true, message: '请输入产线名称', trigger: 'blur' }],
  workshopId: [{ required: true, message: '请选择所属车间', trigger: 'change' }],
}

async function handleRowAction(key: string, row: ProductionLine) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row })
    } else if (key === 'enable' || key === 'disable') {
      await updateLineStatus(row.id, key === 'enable' ? 1 : 0, row.version)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用')
      await refresh()
    } else if (key === 'delete') {
      await deleteLine(row.id, row.version)
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
    <PageHeader title="产线管理" description="车间产线档案，标准日产能用于排产建议" />
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
          新增产线
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
      <el-form-item label="产线编码" prop="lineCode">
        <el-input
          v-model="dialog.model.value.lineCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 LINE-A1"
        />
      </el-form-item>
      <el-form-item label="产线名称" prop="lineName">
        <el-input v-model="dialog.model.value.lineName" maxlength="64" />
      </el-form-item>
      <el-form-item label="所属车间" prop="workshopId">
        <el-select v-model="dialog.model.value.workshopId" filterable placeholder="请选择">
          <el-option
            v-for="opt in workshopOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标准日产能" prop="standardCapacity">
        <el-input-number
          v-model="dialog.model.value.standardCapacity"
          :min="1"
          :step="100"
          controls-position="right"
          placeholder="件/天，选填"
          class="full-width"
        />
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

.full-width {
  width: 100%;
}
</style>
