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
  BARCODE_CONFIG_ROLES,
  BARCODE_ENABLE_STATUS_MAP,
  BARCODE_ENABLE_STATUS_OPTIONS,
  BARCODE_ITEM_TYPE_OPTIONS,
  BARCODE_ITEM_TYPE_TEXT,
  SERIAL_RESET_CYCLE_OPTIONS,
  SERIAL_RESET_CYCLE_TEXT,
} from '@/constants/barcode'
import { loadBarcodeTypeOptions } from '@/api/barcode/options'
import {
  createBarcodeRule,
  deleteBarcodeRule,
  disableBarcodeRule,
  enableBarcodeRule,
  getBarcodeRule,
  getBarcodeRulePage,
  updateBarcodeRule,
} from '@/api/barcode/rule'
import type {
  BarcodeRule,
  BarcodeRuleItemSaveReq,
  BarcodeRulePageParams,
  BarcodeRuleSaveReq,
} from '@/api/barcode/rule'

defineOptions({ name: 'BarcodeRuleList' })

// 条码规则依赖条码类型选项；页面只展示和提交规则，启停及已使用引用限制由后端执行。
const typeOptions = ref<OptionItem[]>([])
const typeNameMap = ref<Map<number, string>>(new Map())

onMounted(async () => {
  typeOptions.value = await loadBarcodeTypeOptions()
  typeNameMap.value = new Map(typeOptions.value.map((opt) => [opt.value as number, opt.label]))
})

const filterFields = computed<FilterField[]>(() => [
  { prop: 'ruleCode', label: '规则编码', type: 'input' },
  { prop: 'ruleName', label: '规则名称', type: 'input' },
  { prop: 'barcodeTypeId', label: '条码类型', type: 'select', options: typeOptions.value },
  { prop: 'status', label: '状态', type: 'select', options: BARCODE_ENABLE_STATUS_OPTIONS },
])

const columns: ColumnDef<BarcodeRule>[] = [
  { prop: 'ruleCode', label: '规则编码', width: 140 },
  { prop: 'ruleName', label: '规则名称', minWidth: 140 },
  {
    prop: 'barcodeTypeId',
    label: '条码类型',
    width: 140,
    formatter: (row) => typeNameMap.value.get(row.barcodeTypeId) ?? String(row.barcodeTypeId),
  },
  { prop: 'serialLength', label: '流水位数', width: 90, align: 'center' },
  {
    prop: 'serialResetCycle',
    label: '重置周期',
    width: 100,
    formatter: (row) => SERIAL_RESET_CYCLE_TEXT[row.serialResetCycle] ?? String(row.serialResetCycle),
  },
  { prop: 'status', label: '状态', width: 80, statusMap: BARCODE_ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<BarcodeRule>[] = [
  { key: 'edit', label: '编辑', roles: BARCODE_CONFIG_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '确认启用该条码规则？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '停用后不可用于生成新条码，确认？',
    show: (row) => row.status === 1,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '已被使用的规则无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  BarcodeRule,
  BarcodeRulePageParams
>({ fetcher: getBarcodeRulePage })

interface RuleItemForm extends BarcodeRuleItemSaveReq {
  _uid: number
}

let itemUidSeed = 0

interface RuleForm {
  id?: number
  ruleCode: string
  ruleName: string
  barcodeTypeId: number | undefined
  serialLength: number
  serialResetCycle: number
  items: RuleItemForm[]
}

const dialog = useFormDialog<RuleForm>(
  () => ({
    ruleCode: '',
    ruleName: '',
    barcodeTypeId: undefined,
    serialLength: 4,
    serialResetCycle: 3,
    items: [],
  }),
  {
    titles: { create: '新增条码规则', edit: '编辑条码规则' },
    submit: async (model, mode) => {
      if (!model.barcodeTypeId) {
        ElMessage.warning('请选择条码类型')
        throw new Error('missing type')
      }
      if (model.items.length === 0) {
        ElMessage.warning('请至少添加一个组成明细')
        throw new Error('empty items')
      }
      const payload: BarcodeRuleSaveReq = {
        ruleCode: model.ruleCode,
        ruleName: model.ruleName,
        barcodeTypeId: model.barcodeTypeId,
        serialLength: model.serialLength,
        serialResetCycle: model.serialResetCycle,
        items: model.items.map(({ _uid: _, ...rest }) => rest),
      }
      if (mode === 'create') {
        await createBarcodeRule(payload)
        ElMessage.success('条码规则已创建')
      } else {
        await updateBarcodeRule(model.id!, payload)
        ElMessage.success('条码规则已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  ruleCode: [
    { required: true, message: '请输入规则编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  barcodeTypeId: [{ required: true, message: '请选择条码类型', trigger: 'change' }],
  serialLength: [{ required: true, message: '请输入流水位数', trigger: 'blur' }],
  serialResetCycle: [{ required: true, message: '请选择重置周期', trigger: 'change' }],
}

function addRuleItem() {
  dialog.model.value.items.push({
    _uid: ++itemUidSeed,
    seq: dialog.model.value.items.length + 1,
    itemType: 1,
    itemValue: '',
    dateFormat: '',
    itemLength: undefined,
  })
}

function removeRuleItem(index: number) {
  dialog.model.value.items.splice(index, 1)
}

async function handleRowAction(key: string, row: BarcodeRule) {
  try {
    if (key === 'edit') {
      const detail = await getBarcodeRule(row.id)
      dialog.open('edit', {
        id: detail.id,
        ruleCode: detail.ruleCode,
        ruleName: detail.ruleName,
        barcodeTypeId: detail.barcodeTypeId,
        serialLength: detail.serialLength,
        serialResetCycle: detail.serialResetCycle,
        items: detail.items.map((item) => ({
          _uid: ++itemUidSeed,
          seq: item.seq,
          itemType: item.itemType,
          itemValue: item.itemValue ?? '',
          dateFormat: item.dateFormat ?? '',
          itemLength: item.itemLength ?? undefined,
        })),
      })
    } else if (key === 'enable' || key === 'disable') {
      if (key === 'enable') await enableBarcodeRule(row.id)
      else await disableBarcodeRule(row.id)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用')
      await refresh()
    } else if (key === 'delete') {
      await deleteBarcodeRule(row.id)
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
    <PageHeader title="条码规则" description="配置条码组成段与流水号规则，支持预览与校验" />
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
          新增规则
        </PermissionButton>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      width="900px"
      @submit="dialog.handleSubmit"
    >
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="规则编码" prop="ruleCode">
            <el-input
              v-model="dialog.model.value.ruleCode"
              :disabled="dialog.mode.value === 'edit'"
              maxlength="32"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="规则名称" prop="ruleName">
            <el-input v-model="dialog.model.value.ruleName" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
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
        <el-col :span="8">
          <el-form-item label="流水位数" prop="serialLength">
            <el-input-number v-model="dialog.model.value.serialLength" :min="1" :max="9" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="重置周期" prop="serialResetCycle">
            <el-select v-model="dialog.model.value.serialResetCycle">
              <el-option
                v-for="opt in SERIAL_RESET_CYCLE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">组成明细</el-divider>
      <div class="detail-toolbar">
        <el-button type="primary" size="small" @click="addRuleItem">添加明细</el-button>
        <span class="form-tip">规则须且仅须包含一个流水号段</span>
      </div>
      <el-table :data="dialog.model.value.items" border size="small" max-height="320">
        <el-table-column label="序号" type="index" width="55" align="center" />
        <el-table-column label="顺序" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.seq" :min="1" :max="255" :controls="false" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="段类型" width="120">
          <template #default="{ row }">
            <el-select v-model="row.itemType" size="small">
              <el-option
                v-for="opt in BARCODE_ITEM_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="常量值/变量名" min-width="140">
          <template #default="{ row }">
            <el-input
              v-model="row.itemValue"
              size="small"
              :placeholder="row.itemType === 3 ? 'productCode/lineCode' : '常量值'"
            />
          </template>
        </el-table-column>
        <el-table-column label="日期格式" width="120">
          <template #default="{ row }">
            <el-input
              v-model="row.dateFormat"
              size="small"
              :disabled="row.itemType !== 2"
              placeholder="yyyyMMdd"
            />
          </template>
        </el-table-column>
        <el-table-column label="段长度" width="90">
          <template #default="{ row }">
            <el-input-number
              v-model="row.itemLength"
              :min="1"
              :max="64"
              :controls="false"
              size="small"
              :disabled="row.itemType === 4"
            />
          </template>
        </el-table-column>
        <el-table-column label="说明" width="100">
          <template #default="{ row }">
            {{ BARCODE_ITEM_TYPE_TEXT[row.itemType] }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" size="small" link @click="removeRuleItem($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
.detail-toolbar {
  margin-bottom: 8px;
}
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
