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
  BASE_DATA_WRITE_ROLES,
  ENABLE_STATUS_MAP,
  ENABLE_STATUS_OPTIONS,
  PRODUCT_TYPE_OPTIONS,
  PRODUCT_TYPE_TEXT,
  UNIT_OPTIONS,
} from '@/constants/production'
import {
  createProduct,
  deleteProduct,
  getProductPage,
  updateProduct,
  updateProductStatus,
} from '@/api/production/product'
import type { Product, ProductPageParams } from '@/api/production/product'

defineOptions({ name: 'ProductList' })

const filterFields: FilterField[] = [
  { prop: 'productCode', label: '产品编码', type: 'input' },
  { prop: 'productName', label: '产品名称', type: 'input' },
  { prop: 'productType', label: '产品类型', type: 'select', options: PRODUCT_TYPE_OPTIONS },
  { prop: 'status', label: '状态', type: 'select', options: ENABLE_STATUS_OPTIONS },
]

const unitText = (unitId: number) =>
  String(UNIT_OPTIONS.find((opt) => opt.value === unitId)?.label ?? unitId)

const columns: ColumnDef<Product>[] = [
  { prop: 'productCode', label: '产品编码', width: 140 },
  { prop: 'productName', label: '产品名称', minWidth: 160 },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'productType', label: '类型', width: 90, formatter: (row) => PRODUCT_TYPE_TEXT[row.productType] ?? String(row.productType) },
  { prop: 'grade', label: '等级', width: 90 },
  { prop: 'unitId', label: '单位', width: 70, formatter: (row) => unitText(row.unitId) },
  { prop: 'status', label: '状态', width: 80, statusMap: ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<Product>[] = [
  { key: 'edit', label: '编辑', roles: BASE_DATA_WRITE_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '确认启用该产品？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '停用后不可用于新建 BOM 与工单，确认？',
    show: (row) => row.status === 1,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '被 BOM/工单引用的产品无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  Product,
  ProductPageParams
>({ fetcher: getProductPage })

interface ProductForm {
  id?: number
  version?: number
  productCode: string
  productName: string
  spec: string
  productType: number
  grade: string
  unitId: number
  status: number
}

const dialog = useFormDialog<ProductForm>(
  () => ({
    productCode: '',
    productName: '',
    spec: '',
    productType: 1,
    grade: '',
    unitId: 1,
    status: 1,
  }),
  {
    titles: { create: '新增产品', edit: '编辑产品' },
    submit: async (model, mode) => {
      const payload = {
        productCode: model.productCode,
        productName: model.productName,
        spec: model.spec || undefined,
        productType: model.productType,
        grade: model.grade || undefined,
        unitId: model.unitId,
        status: model.status,
      }
      if (mode === 'create') {
        await createProduct(payload)
        ElMessage.success('产品已创建')
      } else {
        await updateProduct(model.id!, { ...payload, version: model.version! })
        ElMessage.success('产品已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  productCode: [
    { required: true, message: '请输入产品编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productType: [{ required: true, message: '请选择产品类型', trigger: 'change' }],
  unitId: [{ required: true, message: '请选择计量单位', trigger: 'change' }],
}

async function handleRowAction(key: string, row: Product) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row, spec: row.spec ?? '', grade: row.grade ?? '' })
    } else if (key === 'enable' || key === 'disable') {
      await updateProductStatus(row.id, key === 'enable' ? 1 : 0, row.version)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用')
      await refresh()
    } else if (key === 'delete') {
      await deleteProduct(row.id, row.version)
      ElMessage.success('已删除')
      await refresh()
    }
  } catch {
    // 失败提示由拦截器统一弹出；乐观锁冲突时刷新最新数据
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="产品主档" description="羽毛球成品/半成品档案，供 BOM 与工单引用" />
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
          新增产品
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
      <el-form-item label="产品编码" prop="productCode">
        <el-input
          v-model="dialog.model.value.productCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 SC-A1"
        />
      </el-form-item>
      <el-form-item label="产品名称" prop="productName">
        <el-input v-model="dialog.model.value.productName" maxlength="128" />
      </el-form-item>
      <el-form-item label="规格型号" prop="spec">
        <el-input v-model="dialog.model.value.spec" maxlength="128" placeholder="选填" />
      </el-form-item>
      <el-form-item label="产品类型" prop="productType">
        <el-radio-group v-model="dialog.model.value.productType">
          <el-radio v-for="opt in PRODUCT_TYPE_OPTIONS" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="产品等级" prop="grade">
        <el-input v-model="dialog.model.value.grade" maxlength="32" placeholder="如 A 级，选填" />
      </el-form-item>
      <el-form-item label="计量单位" prop="unitId">
        <el-select v-model="dialog.model.value.unitId">
          <el-option
            v-for="opt in UNIT_OPTIONS"
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
