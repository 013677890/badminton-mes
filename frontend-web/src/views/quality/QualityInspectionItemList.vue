<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  INSPECTION_ENABLE_STATUS_MAP,
  INSPECTION_ENABLE_STATUS_OPTIONS,
  INSPECTION_VALUE_TYPE_OPTIONS,
  INSPECTION_VALUE_TYPE_TEXT,
  JUDGMENT_METHOD_OPTIONS,
  JUDGMENT_METHOD_TEXT,
  QUALITY_WRITE_ROLES,
} from '@/constants/quality'
import { loadInspectionCategoryOptions } from '@/api/quality/options'
import {
  createInspectionItem,
  deleteInspectionItem,
  getInspectionItemPage,
  updateInspectionItem,
} from '@/api/quality/inspectionItem'
import type {
  InspectionItem,
  InspectionItemPageParams,
  InspectionItemSaveReq,
} from '@/api/quality/inspectionItem'

defineOptions({ name: 'QualityInspectionItemList' })

const categoryOptions = ref<OptionItem[]>([])
onMounted(async () => {
  categoryOptions.value = await loadInspectionCategoryOptions()
})

const filterFields = computed<FilterField[]>(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '编码或名称' },
  {
    prop: 'categoryId',
    label: '检验分类',
    type: 'select',
    options: categoryOptions.value,
  },
  {
    prop: 'valueType',
    label: '值类型',
    type: 'select',
    options: INSPECTION_VALUE_TYPE_OPTIONS,
  },
  {
    prop: 'enabledStatus',
    label: '状态',
    type: 'select',
    options: INSPECTION_ENABLE_STATUS_OPTIONS,
  },
])

const columns: ColumnDef<InspectionItem>[] = [
  { prop: 'itemCode', label: '项目编码', width: 130 },
  { prop: 'itemName', label: '项目名称', minWidth: 140 },
  { prop: 'categoryName', label: '分类', width: 120 },
  {
    prop: 'valueType',
    label: '值类型',
    width: 80,
    formatter: (row) => INSPECTION_VALUE_TYPE_TEXT[row.valueType] ?? row.valueType,
  },
  {
    prop: 'judgmentMethod',
    label: '判定方式',
    width: 100,
    formatter: (row) => JUDGMENT_METHOD_TEXT[row.judgmentMethod] ?? row.judgmentMethod,
  },
  {
    prop: 'requiredFlag',
    label: '必检',
    width: 70,
    align: 'center',
    formatter: (row) => (row.requiredFlag ? '是' : '否'),
  },
  {
    prop: 'enabledStatus',
    label: '状态',
    width: 80,
    statusMap: INSPECTION_ENABLE_STATUS_MAP,
  },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<InspectionItem>[] = [
  { key: 'edit', label: '编辑', roles: QUALITY_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: QUALITY_WRITE_ROLES,
    confirm: '被检验方案引用的项目无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  InspectionItem,
  InspectionItemPageParams
>({ fetcher: getInspectionItemPage })

interface ItemForm {
  id?: number
  itemCode: string
  itemName: string
  categoryId: number | undefined
  valueType: string
  unit: string
  standardValue: string
  lowerLimit: number | undefined
  upperLimit: number | undefined
  judgmentMethod: string
  inspectionMethod: string
  requiredFlag: boolean
  enabledStatus: number
  remark: string
}

const dialog = useFormDialog<ItemForm>(
  () => ({
    itemCode: '',
    itemName: '',
    categoryId: undefined,
    valueType: 'NUMERIC',
    unit: '',
    standardValue: '',
    lowerLimit: undefined,
    upperLimit: undefined,
    judgmentMethod: 'RANGE',
    inspectionMethod: '',
    requiredFlag: true,
    enabledStatus: 1,
    remark: '',
  }),
  {
    titles: { create: '新增检验项目', edit: '编辑检验项目' },
    submit: async (model, mode) => {
      const payload: InspectionItemSaveReq = {
        itemCode: model.itemCode,
        itemName: model.itemName,
        categoryId: model.categoryId!,
        valueType: model.valueType,
        unit: model.unit || undefined,
        standardValue: model.standardValue || undefined,
        lowerLimit: model.lowerLimit ?? undefined,
        upperLimit: model.upperLimit ?? undefined,
        judgmentMethod: model.judgmentMethod,
        inspectionMethod: model.inspectionMethod || undefined,
        requiredFlag: model.requiredFlag,
        enabledStatus: model.enabledStatus,
        remark: model.remark || undefined,
      }
      if (mode === 'create') {
        await createInspectionItem(payload)
        ElMessage.success('检验项目已创建')
      } else {
        await updateInspectionItem(model.id!, payload)
        ElMessage.success('检验项目已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  itemCode: [{ required: true, message: '请输入项目编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择检验分类', trigger: 'change' }],
  valueType: [{ required: true, message: '请选择值类型', trigger: 'change' }],
  judgmentMethod: [{ required: true, message: '请选择判定方式', trigger: 'change' }],
}

async function handleRowAction(key: string, row: InspectionItem) {
  if (key === 'edit') {
    dialog.open('edit', {
      ...row,
      unit: row.unit ?? '',
      standardValue: row.standardValue ?? '',
      inspectionMethod: row.inspectionMethod ?? '',
      remark: row.remark ?? '',
    })
  } else if (key === 'delete') {
    await deleteInspectionItem(row.id)
    ElMessage.success('已删除')
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="检验项目" description="质量检验的标准项目库，关联检验分类和判定方式" />
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
          新增项目
        </PermissionButton>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      width="640px"
      @submit="dialog.handleSubmit"
    >
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="项目编码" prop="itemCode">
            <el-input
              v-model="dialog.model.value.itemCode"
              :disabled="dialog.mode.value === 'edit'"
              maxlength="32"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目名称" prop="itemName">
            <el-input v-model="dialog.model.value.itemName" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="检验分类" prop="categoryId">
            <el-select v-model="dialog.model.value.categoryId" filterable>
              <el-option
                v-for="opt in categoryOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="值类型" prop="valueType">
            <el-select v-model="dialog.model.value.valueType">
              <el-option
                v-for="opt in INSPECTION_VALUE_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="判定方式" prop="judgmentMethod">
            <el-select v-model="dialog.model.value.judgmentMethod">
              <el-option
                v-for="opt in JUDGMENT_METHOD_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位" prop="unit">
            <el-input v-model="dialog.model.value.unit" maxlength="32" placeholder="如 mm、g" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="标准值" prop="standardValue">
            <el-input v-model="dialog.model.value.standardValue" maxlength="64" placeholder="选填" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="下限" prop="lowerLimit">
            <el-input-number v-model="dialog.model.value.lowerLimit" :controls="false" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="上限" prop="upperLimit">
            <el-input-number v-model="dialog.model.value.upperLimit" :controls="false" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="检验方法" prop="inspectionMethod">
            <el-input v-model="dialog.model.value.inspectionMethod" maxlength="128" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="必检" prop="requiredFlag">
            <el-switch v-model="dialog.model.value.requiredFlag" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="enabledStatus">
            <el-radio-group v-model="dialog.model.value.enabledStatus">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="dialog.model.value.remark" type="textarea" maxlength="255" />
          </el-form-item>
        </el-col>
      </el-row>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
