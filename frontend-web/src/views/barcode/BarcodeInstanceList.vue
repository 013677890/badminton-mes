<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import type { OptionItem } from '@/types/components'
import {
  BARCODE_MANAGE_ROLES,
  BARCODE_MODE_TEXT,
  BARCODE_PRINT_ROLES,
  BARCODE_SOURCE_OPTIONS,
  BARCODE_SOURCE_TEXT,
  BARCODE_STATUS_MAP,
  BARCODE_STATUS_OPTIONS,
} from '@/constants/barcode'
import {
  batchGenerateBarcodes,
  cancelBarcode,
  generateBarcode,
  getBarcodeApplicationRuleOptions,
  getBarcodeInstancePage,
  getBarcodeTypeOptions,
  getBarcodeUseRecords,
  importBarcodes,
  parseBarcode,
  printBarcode,
} from '@/api/barcode'
import type { BarcodeInstance, BarcodeParseResult, BarcodeUseRecord } from '@/api/barcode'

defineOptions({ name: 'BarcodeInstanceList' })

const filters = reactive({ barcodeValue: '', batchNo: '', barcodeTypeId: undefined as number | undefined, sourceType: undefined as number | undefined, barcodeStatus: undefined as number | undefined })
const table = useTable({ fetcher: getBarcodeInstancePage })
const typeOptions = ref<OptionItem[]>([])
const applicationOptions = ref<OptionItem[]>([])
const createVisible = ref(false)
const createMode = ref<'single' | 'batch' | 'import'>('single')
const createForm = reactive({ applyRuleId: undefined as number | undefined, batchNo: '', workOrderId: undefined as number | undefined, taskId: undefined as number | undefined, lineCode: '', inputBarcodeValue: '', quantity: 1, importText: '' })
const resultVisible = ref(false)
const resultTitle = ref('执行结果')
const resultData = ref<unknown>()
const parseVisible = ref(false)
const parseValue = ref('')
const parseResult = ref<BarcodeParseResult>()
const recordsVisible = ref(false)
const records = ref<BarcodeUseRecord[]>([])

onMounted(async () => {
  const [types, rules] = await Promise.all([getBarcodeTypeOptions(), getBarcodeApplicationRuleOptions({ status: 1 })])
  typeOptions.value = types.map((item) => ({ label: `${item.typeCode} ${item.typeName}`, value: item.id }))
  applicationOptions.value = rules.map((item) => ({ label: `#${item.id} ${item.objectType === 1 ? '产品' : '物料'} / 模板#${item.templateId}`, value: item.id }))
})

function resetFilters() {
  Object.assign(filters, { barcodeValue: '', batchNo: '', barcodeTypeId: undefined, sourceType: undefined, barcodeStatus: undefined })
  void table.reset()
}

function openCreate(mode: 'single' | 'batch' | 'import') {
  createMode.value = mode
  Object.assign(createForm, { applyRuleId: undefined, batchNo: '', workOrderId: undefined, taskId: undefined, lineCode: '', inputBarcodeValue: '', quantity: 1, importText: '' })
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.applyRuleId) { ElMessage.warning('请选择应用规则'); return }
  if (createMode.value === 'import') {
    const items = createForm.importText.split(/\r?\n/).map((line) => line.trim()).filter(Boolean).map((line) => {
      const [barcodeValue, batchNo] = line.split(',').map((value) => value.trim())
      return { barcodeValue, batchNo: batchNo || undefined, workOrderId: createForm.workOrderId, taskId: createForm.taskId }
    })
    if (!items.length) { ElMessage.warning('请至少输入一条待导入条码'); return }
    resultData.value = await importBarcodes({ applyRuleId: createForm.applyRuleId, items })
    resultTitle.value = '导入结果'
  } else {
    const request = { applyRuleId: createForm.applyRuleId, batchNo: createForm.batchNo || undefined, workOrderId: createForm.workOrderId, taskId: createForm.taskId, lineCode: createForm.lineCode || undefined, inputBarcodeValue: createForm.inputBarcodeValue || undefined }
    if (createMode.value === 'batch') {
      resultData.value = await batchGenerateBarcodes({ ...request, quantity: createForm.quantity })
      resultTitle.value = '批量生成结果'
    } else {
      resultData.value = await generateBarcode(request)
      resultTitle.value = '生成结果'
    }
  }
  createVisible.value = false
  resultVisible.value = true
  ElMessage.success('操作成功')
  await table.refresh()
}

async function submitParse() {
  if (!parseValue.value.trim()) { ElMessage.warning('请输入条码值'); return }
  parseResult.value = await parseBarcode(parseValue.value.trim())
}

async function handlePrint(row: BarcodeInstance | Record<string, any>) {
  const { value } = await ElMessageBox.prompt('请输入打印原因', '打印标签', { inputValue: '现场补打', inputPattern: /\S+/, inputErrorMessage: '打印原因不能为空' })
  resultData.value = await printBarcode(row.id, value)
  resultTitle.value = '打印结果'
  resultVisible.value = true
}

async function handleCancel(row: BarcodeInstance | Record<string, any>) {
  const { value } = await ElMessageBox.prompt('请输入作废原因', '作废条码', { type: 'warning', inputPattern: /\S+/, inputErrorMessage: '作废原因不能为空' })
  await cancelBarcode(row.id, value)
  ElMessage.success('条码已作废')
  await table.refresh()
}

async function showRecords(row: BarcodeInstance | Record<string, any>) {
  records.value = await getBarcodeUseRecords(row.id)
  recordsVisible.value = true
}
</script>

<template>
  <div class="page-container">
    <PageHeader title="条码实例" description="条码生成、外部导入、解析、打印、作废与使用履历">
      <template #extra>
        <PermissionButton :roles="BARCODE_MANAGE_ROLES" type="primary" @click="openCreate('single')">生成条码</PermissionButton>
        <PermissionButton :roles="BARCODE_MANAGE_ROLES" @click="openCreate('batch')">批量生成</PermissionButton>
        <PermissionButton :roles="BARCODE_MANAGE_ROLES" @click="openCreate('import')">导入条码</PermissionButton>
        <el-button @click="parseVisible = true">解析条码</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-form :inline="true" :model="filters">
        <el-form-item label="条码"><el-input v-model="filters.barcodeValue" clearable /></el-form-item>
        <el-form-item label="批次"><el-input v-model="filters.batchNo" clearable /></el-form-item>
        <el-form-item label="类型"><el-select v-model="filters.barcodeTypeId" clearable style="width: 180px"><el-option v-for="item in typeOptions" :key="item.value" v-bind="item" /></el-select></el-form-item>
        <el-form-item label="来源"><el-select v-model="filters.sourceType" clearable style="width: 130px"><el-option v-for="item in BARCODE_SOURCE_OPTIONS" :key="item.value" v-bind="item" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="filters.barcodeStatus" clearable style="width: 120px"><el-option v-for="item in BARCODE_STATUS_OPTIONS" :key="item.value" v-bind="item" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="table.query(filters)">查询</el-button><el-button @click="resetFilters">重置</el-button></el-form-item>
      </el-form>
      <el-table v-loading="table.loading.value" :data="table.data.value" border>
        <el-table-column prop="barcodeValue" label="条码值" min-width="210" show-overflow-tooltip />
        <el-table-column label="类型" width="150"><template #default="{ row }">{{ typeOptions.find((item) => item.value === row.barcodeTypeId)?.label || `#${row.barcodeTypeId}` }}</template></el-table-column>
        <el-table-column label="模式" width="90"><template #default="{ row }">{{ BARCODE_MODE_TEXT[row.barcodeMode] }}</template></el-table-column>
        <el-table-column prop="batchNo" label="批次" width="140" />
        <el-table-column label="来源" width="100"><template #default="{ row }">{{ BARCODE_SOURCE_TEXT[row.sourceType] }}</template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.barcodeStatus" :map="BARCODE_STATUS_MAP" /></template></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showRecords(row)">使用记录</el-button>
            <PermissionButton :roles="BARCODE_PRINT_ROLES" link type="primary" @click="handlePrint(row)">打印</PermissionButton>
            <PermissionButton v-if="row.barcodeStatus === 0" :roles="BARCODE_MANAGE_ROLES" link type="danger" @click="handleCancel(row)">作废</PermissionButton>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total, sizes, prev, pager, next" :total="table.pagination.value.total" :current-page="table.pagination.value.pageNo" :page-size="table.pagination.value.pageSize" @current-change="(pageNo: number) => table.onPageChange({ pageNo, pageSize: table.pagination.value.pageSize })" @size-change="(pageSize: number) => table.onPageChange({ pageNo: 1, pageSize })" />
    </el-card>

    <el-dialog v-model="createVisible" :title="createMode === 'single' ? '生成条码' : createMode === 'batch' ? '批量生成条码' : '导入条码'" width="620px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="应用规则" required><el-select v-model="createForm.applyRuleId" filterable style="width: 100%"><el-option v-for="item in applicationOptions" :key="item.value" v-bind="item" /></el-select></el-form-item>
        <template v-if="createMode !== 'import'">
          <el-form-item label="产品批次"><el-input v-model="createForm.batchNo" /></el-form-item>
          <el-form-item label="产线编码"><el-input v-model="createForm.lineCode" /></el-form-item>
          <el-form-item label="传入条码值"><el-input v-model="createForm.inputBarcodeValue" placeholder="仅传入值生成规则需要" /></el-form-item>
          <el-form-item v-if="createMode === 'batch'" label="生成数量"><el-input-number v-model="createForm.quantity" :min="1" :max="1000" /></el-form-item>
        </template>
        <el-form-item v-else label="条码清单" required><el-input v-model="createForm.importText" type="textarea" :rows="8" placeholder="每行：条码值,批次号（批次可省略）" /></el-form-item>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="工单 ID"><el-input-number v-model="createForm.workOrderId" :min="1" /></el-form-item></el-col><el-col :span="12"><el-form-item label="任务 ID"><el-input-number v-model="createForm.taskId" :min="1" /></el-form-item></el-col></el-row>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" @click="submitCreate">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="parseVisible" title="解析条码" width="620px">
      <el-input v-model="parseValue" placeholder="扫描或输入条码值" @keyup.enter="submitParse"><template #append><el-button @click="submitParse">解析</el-button></template></el-input>
      <el-descriptions v-if="parseResult" class="result" :column="2" border><el-descriptions-item label="条码">{{ parseResult.barcodeValue }}</el-descriptions-item><el-descriptions-item label="类型">{{ parseResult.barcodeTypeName }}</el-descriptions-item><el-descriptions-item label="产品">{{ parseResult.productName || '-' }}</el-descriptions-item><el-descriptions-item label="物料">{{ parseResult.materialName || '-' }}</el-descriptions-item><el-descriptions-item label="批次">{{ parseResult.batchNo || '-' }}</el-descriptions-item><el-descriptions-item label="状态"><StatusTag :status="parseResult.barcodeStatus" :map="BARCODE_STATUS_MAP" /></el-descriptions-item></el-descriptions>
    </el-dialog>
    <el-dialog v-model="resultVisible" :title="resultTitle" width="720px"><pre class="json-result">{{ JSON.stringify(resultData, null, 2) }}</pre></el-dialog>
    <el-drawer v-model="recordsVisible" title="条码使用记录" size="760px"><el-table :data="records" border><el-table-column prop="businessTime" label="业务时间" width="170" /><el-table-column prop="useType" label="使用类型" width="100" /><el-table-column prop="taskId" label="任务 ID" /><el-table-column prop="processId" label="工序 ID" /><el-table-column prop="userId" label="人员 ID" /><el-table-column prop="equipmentId" label="设备 ID" /></el-table></el-drawer>
  </div>
</template>

<style scoped>
.pagination { margin-top: 16px; justify-content: flex-end; }
.result { margin-top: 16px; }
.json-result { max-height: 520px; margin: 0; overflow: auto; padding: 12px; background: var(--el-fill-color-light); white-space: pre-wrap; }
</style>
