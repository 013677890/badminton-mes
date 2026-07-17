<script setup lang="ts">
// 故障原理主档为维修分析提供分类信息，页面的筛选和主档操作均通过统一 API 层完成。
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
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
  EQUIPMENT_FAULT_LEVEL_MAP,
  EQUIPMENT_FAULT_LEVEL_OPTIONS,
} from '@/constants/equipment'
import {
  createEquipmentFaultPrinciple,
  deleteEquipmentFaultPrinciple,
  getEquipmentFaultPrinciplePage,
  updateEquipmentFaultPrinciple,
} from '@/api/equipment/faultPrinciple'
import type {
  EquipmentFaultLevel,
  EquipmentFaultPrinciple,
  EquipmentFaultPrinciplePageParams,
  EquipmentFaultPrincipleSaveParams,
} from '@/api/equipment/faultPrinciple'
import { loadEquipmentCategoryOptions } from '@/api/equipment/options'

defineOptions({ name: 'EquipmentFaultPrincipleList' })

const FAULT_WRITE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

const categoryOptions = ref<OptionItem[]>([])
onMounted(async () => {
  categoryOptions.value = await loadEquipmentCategoryOptions()
})

const filterFields = computed<FilterField[]>(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '编码或名称' },
  { prop: 'categoryId', label: '设备类别', type: 'select', options: categoryOptions.value },
  { prop: 'faultLevel', label: '故障级别', type: 'select', options: EQUIPMENT_FAULT_LEVEL_OPTIONS },
  { prop: 'status', label: '状态', type: 'select', options: EQUIPMENT_ENABLE_STATUS_OPTIONS },
])

const columns: ColumnDef<EquipmentFaultPrinciple>[] = [
  { prop: 'faultCode', label: '故障编码', width: 140 },
  { prop: 'faultName', label: '故障名称', minWidth: 140 },
  { prop: 'categoryName', label: '设备类别', minWidth: 120 },
  { prop: 'faultLevel', label: '故障级别', width: 100, statusMap: EQUIPMENT_FAULT_LEVEL_MAP },
  { prop: 'suggestedSolution', label: '建议方案', minWidth: 180, showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 80, statusMap: EQUIPMENT_ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<EquipmentFaultPrinciple>[] = [
  { key: 'edit', label: '编辑', roles: FAULT_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: FAULT_WRITE_ROLES,
    confirm: '确认删除该故障原理？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  EquipmentFaultPrinciple,
  Omit<EquipmentFaultPrinciplePageParams, 'pageNo' | 'pageSize'>
>({ fetcher: getEquipmentFaultPrinciplePage })

interface EquipmentFaultPrincipleForm {
  id?: number
  faultCode: string
  faultName: string
  categoryId: number | null
  faultLevel: EquipmentFaultLevel | null
  faultDescription: string | null
  suggestedSolution: string | null
  sortOrder: number
  remark: string | null
  status: number
}

const dialog = useFormDialog<EquipmentFaultPrincipleForm>(
  () => ({
    faultCode: '',
    faultName: '',
    categoryId: null,
    faultLevel: 'LOW',
    faultDescription: null,
    suggestedSolution: null,
    sortOrder: 0,
    remark: null,
    status: 1,
  }),
  {
    titles: { create: '新增故障原理', edit: '编辑故障原理' },
    submit: async (model, mode) => {
      const payload: EquipmentFaultPrincipleSaveParams = {
        faultCode: model.faultCode,
        faultName: model.faultName,
        categoryId: model.categoryId,
        faultLevel: model.faultLevel,
        faultDescription: model.faultDescription,
        suggestedSolution: model.suggestedSolution,
        sortOrder: model.sortOrder,
        remark: model.remark,
        status: model.status,
      }
      if (mode === 'create') {
        await createEquipmentFaultPrinciple(payload)
        ElMessage.success('故障原理已创建')
      } else {
        await updateEquipmentFaultPrinciple(model.id!, payload)
        ElMessage.success('故障原理已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  faultCode: [{ required: true, message: '请输入故障编码', trigger: 'blur' }],
  faultName: [{ required: true, message: '请输入故障名称', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: EquipmentFaultPrinciple) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row })
    } else if (key === 'delete') {
      await deleteEquipmentFaultPrinciple(row.id)
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
    <PageHeader title="故障原理" description="设备故障知识库，记录故障分类与建议解决方案" />
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
        <PermissionButton :roles="FAULT_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增故障
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
      <el-form-item label="故障编码" prop="faultCode">
        <el-input
          v-model="dialog.model.value.faultCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 FP-001"
        />
      </el-form-item>
      <el-form-item label="故障名称" prop="faultName">
        <el-input v-model="dialog.model.value.faultName" maxlength="128" />
      </el-form-item>
      <el-form-item label="设备类别" prop="categoryId">
        <el-select
          v-model="dialog.model.value.categoryId"
          clearable
          filterable
          placeholder="选填"
        >
          <el-option
            v-for="opt in categoryOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="故障级别" prop="faultLevel">
        <el-select v-model="dialog.model.value.faultLevel">
          <el-option
            v-for="opt in EQUIPMENT_FAULT_LEVEL_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="故障描述" prop="faultDescription">
        <el-input
          v-model="dialog.model.value.faultDescription"
          type="textarea"
          :rows="3"
          maxlength="500"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="建议方案" prop="suggestedSolution">
        <el-input
          v-model="dialog.model.value.suggestedSolution"
          type="textarea"
          :rows="3"
          maxlength="500"
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
