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
  BARCODE_CONFIG_ROLES,
  BARCODE_ENABLE_STATUS_MAP,
  BARCODE_ENABLE_STATUS_OPTIONS,
} from '@/constants/barcode'
import {
  createBarcodeType,
  deleteBarcodeType,
  disableBarcodeType,
  enableBarcodeType,
  getBarcodeTypePage,
  updateBarcodeType,
} from '@/api/barcode/type'
import type { BarcodeType, BarcodeTypePageParams } from '@/api/barcode/type'

defineOptions({ name: 'BarcodeTypeList' })

// 条码类型是规则、模板和应用规则的基础主档，停用/删除是否允许由后端按引用关系判断。
const filterFields: FilterField[] = [
  { prop: 'typeCode', label: '类型编码', type: 'input' },
  { prop: 'typeName', label: '类型名称', type: 'input' },
  { prop: 'status', label: '状态', type: 'select', options: BARCODE_ENABLE_STATUS_OPTIONS },
]

const columns: ColumnDef<BarcodeType>[] = [
  { prop: 'typeCode', label: '类型编码', width: 140 },
  { prop: 'typeName', label: '类型名称', minWidth: 140 },
  { prop: 'applyObject', label: '适用对象', minWidth: 120 },
  { prop: 'status', label: '状态', width: 80, statusMap: BARCODE_ENABLE_STATUS_MAP },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const rowActions: RowAction<BarcodeType>[] = [
  { key: 'edit', label: '编辑', roles: BARCODE_CONFIG_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '确认启用该条码类型？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '停用后不允许新建相关应用规则，确认？',
    show: (row) => row.status === 1,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '已被条码规则或应用规则使用时无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  BarcodeType,
  BarcodeTypePageParams
>({ fetcher: getBarcodeTypePage })

interface BarcodeTypeForm {
  id?: number
  typeCode: string
  typeName: string
  applyObject: string
}

const dialog = useFormDialog<BarcodeTypeForm>(
  () => ({ typeCode: '', typeName: '', applyObject: '' }),
  {
    titles: { create: '新增条码类型', edit: '编辑条码类型' },
    submit: async (model, mode) => {
      const payload = {
        typeCode: model.typeCode,
        typeName: model.typeName,
        applyObject: model.applyObject || undefined,
      }
      if (mode === 'create') {
        await createBarcodeType(payload)
        ElMessage.success('条码类型已创建')
      } else {
        await updateBarcodeType(model.id!, payload)
        ElMessage.success('条码类型已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  typeCode: [
    { required: true, message: '请输入类型编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: BarcodeType) {
  try {
    if (key === 'edit') {
      dialog.open('edit', {
        id: row.id,
        typeCode: row.typeCode,
        typeName: row.typeName,
        applyObject: row.applyObject ?? '',
      })
    } else if (key === 'enable' || key === 'disable') {
      if (key === 'enable') await enableBarcodeType(row.id)
      else await disableBarcodeType(row.id)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用')
      await refresh()
    } else if (key === 'delete') {
      await deleteBarcodeType(row.id)
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
    <PageHeader title="条码类型" description="产品码/内外箱码/栈板码等条码类型档案" />
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
        <PermissionButton :roles="BARCODE_CONFIG_ROLES" type="primary" @click="dialog.open()">
          新增类型
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
      <el-form-item label="类型编码" prop="typeCode">
        <el-input
          v-model="dialog.model.value.typeCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 PRODUCT"
        />
      </el-form-item>
      <el-form-item label="类型名称" prop="typeName">
        <el-input v-model="dialog.model.value.typeName" maxlength="64" placeholder="如产品码" />
      </el-form-item>
      <el-form-item label="适用对象" prop="applyObject">
        <el-input v-model="dialog.model.value.applyObject" maxlength="64" placeholder="选填" />
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
