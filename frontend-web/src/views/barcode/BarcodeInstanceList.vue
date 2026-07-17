<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import {
  BARCODE_GENERATE_ROLES,
  BARCODE_MODE_TEXT,
  BARCODE_PRINT_ROLES,
  BARCODE_SOURCE_OPTIONS,
  BARCODE_SOURCE_TEXT,
  BARCODE_STATUS_MAP,
  BARCODE_STATUS_OPTIONS,
  BARCODE_USE_TYPE_TEXT,
} from '@/constants/barcode'
import { loadBarcodeTypeOptions } from '@/api/barcode/options'
import {
  cancelBarcode,
  getBarcodeInstancePage,
  getBarcodeUseRecords,
  printBarcode,
} from '@/api/barcode/instance'
import type {
  BarcodeInstance,
  BarcodeInstancePageParams,
  BarcodeUseRecord,
} from '@/api/barcode/instance'
import BarcodeInstanceOps from './BarcodeInstanceOps.vue'

defineOptions({ name: 'BarcodeInstanceList' })

const typeOptions = ref<OptionItem[]>([])
const typeNameMap = ref<Map<number, string>>(new Map())

onMounted(async () => {
  typeOptions.value = await loadBarcodeTypeOptions()
  typeNameMap.value = new Map(typeOptions.value.map((opt) => [opt.value as number, opt.label]))
})

const opsRef = ref<InstanceType<typeof BarcodeInstanceOps>>()

const filterFields = computed<FilterField[]>(() => [
  { prop: 'barcodeValue', label: '条码值', type: 'input' },
  { prop: 'batchNo', label: '批次号', type: 'input' },
  { prop: 'barcodeTypeId', label: '条码类型', type: 'select', options: typeOptions.value },
  { prop: 'sourceType', label: '来源', type: 'select', options: BARCODE_SOURCE_OPTIONS },
  { prop: 'barcodeStatus', label: '状态', type: 'select', options: BARCODE_STATUS_OPTIONS },
])

const columns: ColumnDef<BarcodeInstance>[] = [
  { prop: 'barcodeValue', label: '条码值', minWidth: 180 },
  {
    prop: 'barcodeTypeId',
    label: '条码类型',
    width: 140,
    formatter: (row) => typeNameMap.value.get(row.barcodeTypeId) ?? String(row.barcodeTypeId),
  },
  {
    prop: 'barcodeMode',
    label: '模式',
    width: 80,
    formatter: (row) => BARCODE_MODE_TEXT[row.barcodeMode] ?? String(row.barcodeMode),
  },
  { prop: 'batchNo', label: '批次号', width: 130 },
  {
    prop: 'sourceType',
    label: '来源',
    width: 90,
    formatter: (row) => BARCODE_SOURCE_TEXT[row.sourceType] ?? String(row.sourceType),
  },
  { prop: 'barcodeStatus', label: '状态', width: 90, statusMap: BARCODE_STATUS_MAP },
  { prop: 'createTime', label: '生成时间', width: 170 },
]

const rowActions: RowAction<BarcodeInstance>[] = [
  { key: 'useRecord', label: '使用记录' },
  {
    key: 'print',
    label: '打印',
    type: 'primary',
    roles: BARCODE_PRINT_ROLES,
    show: (row) => row.barcodeStatus === 0,
  },
  {
    key: 'cancel',
    label: '作废',
    type: 'danger',
    roles: BARCODE_GENERATE_ROLES,
    show: (row) => row.barcodeStatus === 0,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  BarcodeInstance,
  BarcodeInstancePageParams
>({ fetcher: getBarcodeInstancePage })

const useRecordVisible = ref(false)
const useRecords = ref<BarcodeUseRecord[]>([])

async function showUseRecords(row: BarcodeInstance) {
  useRecords.value = await getBarcodeUseRecords(row.id)
  useRecordVisible.value = true
}

async function handlePrint(row: BarcodeInstance) {
  const { value } = await ElMessageBox.prompt('重复打印时须填写原因', '打印条码', {
    inputPlaceholder: '选填，首次打印可留空',
    confirmButtonText: '打印',
    inputType: 'text',
  })
  await printBarcode(row.id, { reason: value || undefined })
  ElMessage.success('打印记录已创建')
  await refresh()
}

async function handleCancel(row: BarcodeInstance) {
  const { value } = await ElMessageBox.prompt('请输入作废原因', '作废条码', {
    inputPlaceholder: '作废原因（选填）',
    confirmButtonText: '确认作废',
    inputType: 'text',
  })
  await cancelBarcode(row.id, { reason: value || undefined })
  ElMessage.success('条码已作废')
  await refresh()
}

async function handleRowAction(key: string, row: BarcodeInstance) {
  try {
    if (key === 'useRecord') await showUseRecords(row)
    else if (key === 'print') await handlePrint(row)
    else if (key === 'cancel') await handleCancel(row)
  } catch {
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="条码实例" description="条码生成、导入、解析、打印与作废" />
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
        <PermissionButton :roles="BARCODE_GENERATE_ROLES" type="primary" @click="opsRef?.openGenerate()">
          生成条码
        </PermissionButton>
        <PermissionButton :roles="BARCODE_GENERATE_ROLES" @click="opsRef?.openBatch()">
          批量生成
        </PermissionButton>
        <PermissionButton :roles="BARCODE_GENERATE_ROLES" @click="opsRef?.openImport()">
          导入条码
        </PermissionButton>
        <el-button @click="opsRef?.openParse()">解析条码</el-button>
      </template>
    </FilterTable>

    <!-- 使用记录 -->
    <el-dialog v-model="useRecordVisible" title="扫码使用记录" width="680px">
      <el-table :data="useRecords" border size="small" max-height="400">
        <el-table-column label="使用类型" width="110">
          <template #default="{ row }">
            {{ BARCODE_USE_TYPE_TEXT[row.useType] ?? row.useType }}
          </template>
        </el-table-column>
        <el-table-column prop="taskId" label="任务ID" width="100" />
        <el-table-column prop="processId" label="工序ID" width="100" />
        <el-table-column prop="userId" label="扫码人" width="100" />
        <el-table-column prop="businessTime" label="业务时间" width="170" />
        <el-table-column prop="createTime" label="记录时间" width="170" />
      </el-table>
      <el-empty v-if="useRecords.length === 0" description="暂无使用记录" />
    </el-dialog>

    <BarcodeInstanceOps ref="opsRef" @success="refresh" />
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
