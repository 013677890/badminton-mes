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
  APPLY_OBJECT_TYPE_OPTIONS,
  APPLY_OBJECT_TYPE_TEXT,
  BARCODE_CONFIG_ROLES,
  BARCODE_ENABLE_STATUS_MAP,
  BARCODE_ENABLE_STATUS_OPTIONS,
  BARCODE_MODE_OPTIONS,
  BARCODE_MODE_TEXT,
  BARCODE_SOURCE_OPTIONS,
  BARCODE_SOURCE_TEXT,
} from '@/constants/barcode'
import {
  loadBarcodeRuleOptions,
  loadBarcodeTemplateOptions,
  loadBarcodeTypeOptions,
} from '@/api/barcode/options'
import {
  createBarcodeApplicationRule,
  deleteBarcodeApplicationRule,
  disableBarcodeApplicationRule,
  enableBarcodeApplicationRule,
  getBarcodeApplicationRulePage,
  updateBarcodeApplicationRule,
} from '@/api/barcode/applicationRule'
import type {
  BarcodeApplicationRule,
  BarcodeApplicationRulePageParams,
  BarcodeApplicationRuleSaveReq,
} from '@/api/barcode/applicationRule'

defineOptions({ name: 'BarcodeApplicationRuleList' })

const typeOptions = ref<OptionItem[]>([])
const ruleOptions = ref<OptionItem[]>([])
const templateOptions = ref<OptionItem[]>([])
const typeNameMap = ref<Map<number, string>>(new Map())

onMounted(async () => {
  const [types, rules, templates] = await Promise.all([
    loadBarcodeTypeOptions(),
    loadBarcodeRuleOptions(),
    loadBarcodeTemplateOptions(),
  ])
  typeOptions.value = types
  ruleOptions.value = rules
  templateOptions.value = templates
  typeNameMap.value = new Map(types.map((opt) => [opt.value as number, opt.label]))
})

const filterFields: FilterField[] = [
  { prop: 'objectType', label: '对象类型', type: 'select', options: APPLY_OBJECT_TYPE_OPTIONS },
  { prop: 'productId', label: '产品ID', type: 'input' },
  { prop: 'materialId', label: '物料ID', type: 'input' },
  { prop: 'barcodeTypeId', label: '条码类型', type: 'select', options: typeOptions },
  { prop: 'sourceType', label: '来源', type: 'select', options: BARCODE_SOURCE_OPTIONS },
  { prop: 'status', label: '状态', type: 'select', options: BARCODE_ENABLE_STATUS_OPTIONS },
]

const columns: ColumnDef<BarcodeApplicationRule>[] = [
  {
    prop: 'objectType',
    label: '对象类型',
    width: 90,
    formatter: (row) => APPLY_OBJECT_TYPE_TEXT[row.objectType] ?? String(row.objectType),
  },
  {
    prop: 'productId',
    label: '产品/物料ID',
    width: 120,
    formatter: (row) => String(row.productId ?? row.materialId ?? '-'),
  },
  {
    prop: 'barcodeTypeId',
    label: '条码类型',
    width: 140,
    formatter: (row) => typeNameMap.value.get(row.barcodeTypeId) ?? String(row.barcodeTypeId),
  },
  {
    prop: 'barcodeMode',
    label: '条码模式',
    width: 90,
    formatter: (row) => BARCODE_MODE_TEXT[row.barcodeMode] ?? String(row.barcodeMode),
  },
  {
    prop: 'sourceType',
    label: '来源',
    width: 90,
    formatter: (row) => BARCODE_SOURCE_TEXT[row.sourceType] ?? String(row.sourceType),
  },
  {
    prop: 'defaultFlag',
    label: '默认',
    width: 70,
    align: 'center',
    formatter: (row) => (row.defaultFlag ? '是' : '否'),
  },
  { prop: 'status', label: '状态', width: 80, statusMap: BARCODE_ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<BarcodeApplicationRule>[] = [
  { key: 'edit', label: '编辑', roles: BARCODE_CONFIG_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '启用前校验类型/规则/模板均启用，确认？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '停用后不可用于生成条码，确认？',
    show: (row) => row.status === 1,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '已被使用的应用规则无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  BarcodeApplicationRule,
  BarcodeApplicationRulePageParams
>({ fetcher: getBarcodeApplicationRulePage })

interface ApplicationRuleForm {
  id?: number
  objectType: number
  productId: number | undefined
  materialId: number | undefined
  barcodeTypeId: number | undefined
  barcodeMode: number
  ruleId: number | undefined
  templateId: number | undefined
  sourceType: number
  defaultFlag: boolean
}

const dialog = useFormDialog<ApplicationRuleForm>(
  () => ({
    objectType: 1,
    productId: undefined,
    materialId: undefined,
    barcodeTypeId: undefined,
    barcodeMode: 2,
    ruleId: undefined,
    templateId: undefined,
    sourceType: 1,
    defaultFlag: true,
  }),
  {
    titles: { create: '新增应用规则', edit: '编辑应用规则' },
    submit: async (model, mode) => {
      if (!model.barcodeTypeId) {
        ElMessage.warning('请选择条码类型')
        throw new Error('missing type')
      }
      if (!model.templateId) {
        ElMessage.warning('请选择标签模板')
        throw new Error('missing template')
      }
      if (model.objectType === 1 && !model.productId) {
        ElMessage.warning('对象类型为产品时须填写产品ID')
        throw new Error('missing product')
      }
      if (model.objectType === 2 && !model.materialId) {
        ElMessage.warning('对象类型为物料时须填写物料ID')
        throw new Error('missing material')
      }
      const payload: BarcodeApplicationRuleSaveReq = {
        objectType: model.objectType,
        productId: model.productId || undefined,
        materialId: model.materialId || undefined,
        barcodeTypeId: model.barcodeTypeId,
        barcodeMode: model.barcodeMode,
        ruleId: model.ruleId || undefined,
        templateId: model.templateId,
        sourceType: model.sourceType,
        defaultFlag: model.defaultFlag,
      }
      if (mode === 'create') {
        await createBarcodeApplicationRule(payload)
        ElMessage.success('应用规则已创建')
      } else {
        await updateBarcodeApplicationRule(model.id!, payload)
        ElMessage.success('应用规则已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  objectType: [{ required: true, message: '请选择对象类型', trigger: 'change' }],
  barcodeTypeId: [{ required: true, message: '请选择条码类型', trigger: 'change' }],
  barcodeMode: [{ required: true, message: '请选择条码模式', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择标签模板', trigger: 'change' }],
  sourceType: [{ required: true, message: '请选择条码来源', trigger: 'change' }],
}

async function handleRowAction(key: string, row: BarcodeApplicationRule) {
  try {
    if (key === 'edit') {
      dialog.open('edit', {
        id: row.id,
        objectType: row.objectType,
        productId: row.productId ?? undefined,
        materialId: row.materialId ?? undefined,
        barcodeTypeId: row.barcodeTypeId,
        barcodeMode: row.barcodeMode,
        ruleId: row.ruleId ?? undefined,
        templateId: row.templateId,
        sourceType: row.sourceType,
        defaultFlag: row.defaultFlag,
      })
    } else if (key === 'enable' || key === 'disable') {
      if (key === 'enable') await enableBarcodeApplicationRule(row.id)
      else await disableBarcodeApplicationRule(row.id)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用')
      await refresh()
    } else if (key === 'delete') {
      await deleteBarcodeApplicationRule(row.id)
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
    <PageHeader title="条码应用规则" description="组合对象/类型/规则/模板为条码生成入口" />
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
          新增应用规则
        </PermissionButton>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      width="680px"
      @submit="dialog.handleSubmit"
    >
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="对象类型" prop="objectType">
            <el-select v-model="dialog.model.value.objectType">
              <el-option
                v-for="opt in APPLY_OBJECT_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col v-if="dialog.model.value.objectType === 1" :span="12">
          <el-form-item label="产品ID" prop="productId">
            <el-input-number
              v-model="dialog.model.value.productId"
              :min="1"
              :controls="false"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col v-if="dialog.model.value.objectType === 2" :span="12">
          <el-form-item label="物料ID" prop="materialId">
            <el-input-number
              v-model="dialog.model.value.materialId"
              :min="1"
              :controls="false"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="条码类型" prop="barcodeTypeId">
            <el-select v-model="dialog.model.value.barcodeTypeId" filterable placeholder="选择类型">
              <el-option
                v-for="opt in typeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="条码模式" prop="barcodeMode">
            <el-select v-model="dialog.model.value.barcodeMode">
              <el-option
                v-for="opt in BARCODE_MODE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="条码来源" prop="sourceType">
            <el-select v-model="dialog.model.value.sourceType">
              <el-option
                v-for="opt in BARCODE_SOURCE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col v-if="dialog.model.value.sourceType === 1" :span="12">
          <el-form-item label="条码规则" prop="ruleId">
            <el-select v-model="dialog.model.value.ruleId" filterable placeholder="规则生成时必选">
              <el-option
                v-for="opt in ruleOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="标签模板" prop="templateId">
            <el-select v-model="dialog.model.value.templateId" filterable placeholder="选择模板">
              <el-option
                v-for="opt in templateOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="默认规则" prop="defaultFlag">
            <el-switch v-model="dialog.model.value.defaultFlag" />
            <span class="form-tip">同对象同类型仅一条默认规则</span>
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
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
