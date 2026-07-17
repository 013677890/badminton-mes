<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  INSPECTION_ENABLE_STATUS_MAP,
  INSPECTION_ENABLE_STATUS_OPTIONS,
  QUALITY_WRITE_ROLES,
} from '@/constants/quality'
import {
  createInspectionCategory,
  deleteInspectionCategory,
  getInspectionCategoryPage,
  updateInspectionCategory,
} from '@/api/quality/inspectionCategory'
import type {
  InspectionCategory,
  InspectionCategoryPageParams,
  InspectionCategorySaveReq,
} from '@/api/quality/inspectionCategory'

defineOptions({ name: 'QualityInspectionCategoryList' })

// 检验分类是检验项目的上级主档，页面只维护表单和分页状态，引用约束由后端处理。
const filterFields: FilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '编码或名称' },
  {
    prop: 'enabledStatus',
    label: '状态',
    type: 'select',
    options: INSPECTION_ENABLE_STATUS_OPTIONS,
  },
]

const columns: ColumnDef<InspectionCategory>[] = [
  { prop: 'categoryCode', label: '分类编码', width: 140 },
  { prop: 'categoryName', label: '分类名称', minWidth: 160 },
  {
    prop: 'enabledStatus',
    label: '状态',
    width: 80,
    statusMap: INSPECTION_ENABLE_STATUS_MAP,
  },
  { prop: 'remark', label: '备注', minWidth: 200, showOverflowTooltip: true },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<InspectionCategory>[] = [
  { key: 'edit', label: '编辑', roles: QUALITY_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: QUALITY_WRITE_ROLES,
    confirm: '被检验项目引用的分类无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  InspectionCategory,
  InspectionCategoryPageParams
>({ fetcher: getInspectionCategoryPage })

interface CategoryForm {
  id?: number
  categoryCode: string
  categoryName: string
  enabledStatus: number
  remark: string
}

const dialog = useFormDialog<CategoryForm>(
  () => ({ categoryCode: '', categoryName: '', enabledStatus: 1, remark: '' }),
  {
    titles: { create: '新增检验分类', edit: '编辑检验分类' },
    submit: async (model, mode) => {
      const payload: InspectionCategorySaveReq = {
        categoryCode: model.categoryCode,
        categoryName: model.categoryName,
        enabledStatus: model.enabledStatus,
        remark: model.remark || undefined,
      }
      if (mode === 'create') {
        await createInspectionCategory(payload)
        ElMessage.success('检验分类已创建')
      } else {
        await updateInspectionCategory(model.id!, payload)
        ElMessage.success('检验分类已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: InspectionCategory) {
  if (key === 'edit') {
    dialog.open('edit', { ...row, remark: row.remark ?? '' })
  } else if (key === 'delete') {
    await deleteInspectionCategory(row.id)
    ElMessage.success('已删除')
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="检验分类" description="质量检验项目的分类管理，如外观、尺寸、性能等" />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="140"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="QUALITY_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增分类
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
      <el-form-item label="分类编码" prop="categoryCode">
        <el-input
          v-model="dialog.model.value.categoryCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 QC-APPEARANCE"
        />
      </el-form-item>
      <el-form-item label="分类名称" prop="categoryName">
        <el-input v-model="dialog.model.value.categoryName" maxlength="64" />
      </el-form-item>
      <el-form-item label="状态" prop="enabledStatus">
        <el-radio-group v-model="dialog.model.value.enabledStatus">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="dialog.model.value.remark" type="textarea" maxlength="255" />
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
