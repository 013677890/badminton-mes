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
  MATERIAL_TYPE_OPTIONS,
  MATERIAL_TYPE_TEXT,
  UNIT_OPTIONS,
} from '@/constants/production'
import {
  createMaterial,
  deleteMaterial,
  getMaterialPage,
  updateMaterial,
  updateMaterialStatus,
} from '@/api/production/material'
import type { Material, MaterialPageParams } from '@/api/production/material'

defineOptions({ name: 'MaterialList' })

const filterFields: FilterField[] = [
  { prop: 'materialCode', label: '物料编码', type: 'input' },
  { prop: 'materialName', label: '物料名称', type: 'input' },
  { prop: 'materialType', label: '物料类型', type: 'select', options: MATERIAL_TYPE_OPTIONS },
  {
    prop: 'keyMaterial',
    label: '关键物料',
    type: 'select',
    options: [
      { label: '是', value: 'true' },
      { label: '否', value: 'false' },
    ],
  },
  { prop: 'status', label: '状态', type: 'select', options: ENABLE_STATUS_OPTIONS },
]

const columns: ColumnDef<Material>[] = [
  { prop: 'materialCode', label: '物料编码', width: 140 },
  { prop: 'materialName', label: '物料名称', minWidth: 160 },
  { prop: 'spec', label: '规格型号', minWidth: 120 },
  { prop: 'materialType', label: '类型', width: 90, formatter: (row) => MATERIAL_TYPE_TEXT[row.materialType] ?? String(row.materialType) },
  {
    prop: 'keyMaterial',
    label: '关键物料',
    width: 90,
    align: 'center',
    formatter: (row) => (row.keyMaterial ? '是' : '否'),
  },
  {
    prop: 'unitId',
    label: '单位',
    width: 70,
    formatter: (row) => String(UNIT_OPTIONS.find((opt) => opt.value === row.unitId)?.label ?? row.unitId),
  },
  { prop: 'status', label: '状态', width: 80, statusMap: ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<Material>[] = [
  { key: 'edit', label: '编辑', roles: BASE_DATA_WRITE_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '确认启用该物料？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '停用后不可用于新建 BOM，确认？',
    show: (row) => row.status === 1,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: BASE_DATA_WRITE_ROLES,
    confirm: '被 BOM 引用的物料无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  Material,
  MaterialPageParams
>({ fetcher: getMaterialPage })

interface MaterialForm {
  id?: number
  version?: number
  materialCode: string
  materialName: string
  spec: string
  materialType: number
  unitId: number
  keyMaterial: boolean
  status: number
}

const dialog = useFormDialog<MaterialForm>(
  () => ({
    materialCode: '',
    materialName: '',
    spec: '',
    materialType: 1,
    unitId: 1,
    keyMaterial: false,
    status: 1,
  }),
  {
    titles: { create: '新增物料', edit: '编辑物料' },
    submit: async (model, mode) => {
      const payload = {
        materialCode: model.materialCode,
        materialName: model.materialName,
        spec: model.spec || undefined,
        materialType: model.materialType,
        unitId: model.unitId,
        keyMaterial: model.keyMaterial,
        status: model.status,
      }
      if (mode === 'create') {
        await createMaterial(payload)
        ElMessage.success('物料已创建')
      } else {
        await updateMaterial(model.id!, { ...payload, version: model.version! })
        ElMessage.success('物料已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  materialCode: [
    { required: true, message: '请输入物料编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  materialName: [{ required: true, message: '请输入物料名称', trigger: 'blur' }],
  materialType: [{ required: true, message: '请选择物料类型', trigger: 'change' }],
  unitId: [{ required: true, message: '请选择计量单位', trigger: 'change' }],
}

async function handleRowAction(key: string, row: Material) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row, spec: row.spec ?? '' })
    } else if (key === 'enable' || key === 'disable') {
      await updateMaterialStatus(row.id, key === 'enable' ? 1 : 0, row.version)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用')
      await refresh()
    } else if (key === 'delete') {
      await deleteMaterial(row.id, row.version)
      ElMessage.success('已删除')
      await refresh()
    }
  } catch {
    await refresh()
  }
}

/** 筛选下拉的 'true'/'false' 字符串转回 boolean 传给后端 */
function normalizeParams(params: Record<string, any>) {
  const next = { ...params }
  if (next.keyMaterial === 'true') next.keyMaterial = true
  else if (next.keyMaterial === 'false') next.keyMaterial = false
  return next
}
</script>

<template>
  <div class="page">
    <PageHeader title="物料主档" description="球头/羽毛/胶水等原辅料档案，关键物料参与齐套分析" />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="200"
      show-index
      @query="(params) => query(normalizeParams(params))"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="BASE_DATA_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增物料
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
      <el-form-item label="物料编码" prop="materialCode">
        <el-input
          v-model="dialog.model.value.materialCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 MAT-CORK-01"
        />
      </el-form-item>
      <el-form-item label="物料名称" prop="materialName">
        <el-input v-model="dialog.model.value.materialName" maxlength="128" />
      </el-form-item>
      <el-form-item label="规格型号" prop="spec">
        <el-input v-model="dialog.model.value.spec" maxlength="128" placeholder="选填" />
      </el-form-item>
      <el-form-item label="物料类型" prop="materialType">
        <el-select v-model="dialog.model.value.materialType">
          <el-option
            v-for="opt in MATERIAL_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
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
      <el-form-item label="关键物料" prop="keyMaterial">
        <el-switch v-model="dialog.model.value.keyMaterial" />
        <span class="form-tip">关键物料欠料时工单判定为欠料状态</span>
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

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
