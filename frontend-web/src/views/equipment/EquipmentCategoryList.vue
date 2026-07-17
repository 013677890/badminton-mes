<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { ROLES } from '@/constants/production'
import {
  EQUIPMENT_ENABLE_STATUS_MAP,
  EQUIPMENT_ENABLE_STATUS_OPTIONS,
} from '@/constants/equipment'
import {
  createEquipmentCategory,
  deleteEquipmentCategory,
  getEquipmentCategoryPage,
  updateEquipmentCategory,
} from '@/api/equipment/category'
import type {
  EquipmentCategory,
  EquipmentCategoryPageParams,
  EquipmentCategorySaveParams,
} from '@/api/equipment/category'

defineOptions({ name: 'EquipmentCategoryList' })

// 设备类别支持父子层级和排序，页面提交原始层级字段，后端负责循环引用及台账引用校验。
const CATEGORY_WRITE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

const filterFields: FilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '编码或名称' },
  { prop: 'status', label: '状态', type: 'select', options: EQUIPMENT_ENABLE_STATUS_OPTIONS },
]

const columns: ColumnDef<EquipmentCategory>[] = [
  { prop: 'categoryCode', label: '类别编码', width: 140 },
  { prop: 'categoryName', label: '类别名称', minWidth: 160 },
  { prop: 'parentId', label: '父类别ID', width: 100, align: 'center' },
  { prop: 'sortOrder', label: '排序', width: 80, align: 'center' },
  { prop: 'status', label: '状态', width: 80, statusMap: EQUIPMENT_ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<EquipmentCategory>[] = [
  { key: 'edit', label: '编辑', roles: CATEGORY_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: CATEGORY_WRITE_ROLES,
    confirm: '确认删除该设备类别？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  EquipmentCategory,
  Omit<EquipmentCategoryPageParams, 'pageNo' | 'pageSize'>
>({ fetcher: getEquipmentCategoryPage })

interface EquipmentCategoryForm {
  id?: number
  categoryCode: string
  categoryName: string
  parentId: number | null
  sortOrder: number
  remark: string | null
  status: number
}

const dialog = useFormDialog<EquipmentCategoryForm>(
  () => ({
    categoryCode: '',
    categoryName: '',
    parentId: null,
    sortOrder: 0,
    remark: null,
    status: 1,
  }),
  {
    titles: { create: '新增设备类别', edit: '编辑设备类别' },
    submit: async (model, mode) => {
      const payload: EquipmentCategorySaveParams = {
        categoryCode: model.categoryCode,
        categoryName: model.categoryName,
        parentId: model.parentId,
        sortOrder: model.sortOrder,
        remark: model.remark,
        status: model.status,
      }
      if (mode === 'create') {
        await createEquipmentCategory(payload)
        ElMessage.success('设备类别已创建')
      } else {
        await updateEquipmentCategory(model.id!, payload)
        ElMessage.success('设备类别已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  categoryCode: [{ required: true, message: '请输入类别编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入类别名称', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: EquipmentCategory) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row })
    } else if (key === 'delete') {
      await deleteEquipmentCategory(row.id)
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
    <PageHeader title="设备类别" description="设备分类档案，支持层级结构与排序管理" />
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
        <PermissionButton :roles="CATEGORY_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增类别
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
      <el-form-item label="类别编码" prop="categoryCode">
        <el-input
          v-model="dialog.model.value.categoryCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 EC-01"
        />
      </el-form-item>
      <el-form-item label="类别名称" prop="categoryName">
        <el-input v-model="dialog.model.value.categoryName" maxlength="64" />
      </el-form-item>
      <el-form-item label="父类别ID" prop="parentId">
        <el-input-number
          v-model="dialog.model.value.parentId"
          :min="1"
          controls-position="right"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="排序" prop="sortOrder">
        <el-input-number v-model="dialog.model.value.sortOrder" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="dialog.model.value.remark" type="textarea" :rows="2" maxlength="255" />
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
