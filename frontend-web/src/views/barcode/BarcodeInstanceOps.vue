<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { OptionItem } from '@/types/components'
import {
  BARCODE_MODE_TEXT,
  BARCODE_SOURCE_TEXT,
  BARCODE_STATUS_MAP,
} from '@/constants/barcode'
import { loadBarcodeApplicationRuleOptions } from '@/api/barcode/options'
import {
  batchGenerateBarcodes,
  generateBarcode,
  importBarcodes,
  parseBarcode,
} from '@/api/barcode/instance'
import type { BarcodeParseResp } from '@/api/barcode/instance'

defineOptions({ name: 'BarcodeInstanceOps' })

const emit = defineEmits<{ success: [] }>()

const applyRuleOptions = ref<OptionItem[]>([])
onMounted(async () => {
  applyRuleOptions.value = await loadBarcodeApplicationRuleOptions()
})

// ---------- 生成条码 ----------
const generateVisible = ref(false)
const generateSubmitting = ref(false)
const generateForm = reactive({
  applyRuleId: undefined as number | undefined,
  batchNo: '',
  workOrderId: undefined as number | undefined,
  taskId: undefined as number | undefined,
  lineCode: '',
})

function openGenerate() {
  Object.assign(generateForm, {
    applyRuleId: undefined,
    batchNo: '',
    workOrderId: undefined,
    taskId: undefined,
    lineCode: '',
  })
  generateVisible.value = true
}

async function submitGenerate() {
  if (!generateForm.applyRuleId) {
    ElMessage.warning('请选择应用规则')
    return
  }
  generateSubmitting.value = true
  try {
    const result = await generateBarcode({
      applyRuleId: generateForm.applyRuleId,
      batchNo: generateForm.batchNo || undefined,
      workOrderId: generateForm.workOrderId,
      taskId: generateForm.taskId,
      lineCode: generateForm.lineCode || undefined,
    })
    ElMessage.success(`条码已生成：${result.barcodeValue}`)
    generateVisible.value = false
    emit('success')
  } finally {
    generateSubmitting.value = false
  }
}

// ---------- 批量生成 ----------
const batchVisible = ref(false)
const batchSubmitting = ref(false)
const batchForm = reactive({
  applyRuleId: undefined as number | undefined,
  quantity: 10,
})

function openBatch() {
  batchForm.applyRuleId = undefined
  batchForm.quantity = 10
  batchVisible.value = true
}

async function submitBatch() {
  if (!batchForm.applyRuleId) {
    ElMessage.warning('请选择应用规则')
    return
  }
  batchSubmitting.value = true
  try {
    const result = await batchGenerateBarcodes({
      applyRuleId: batchForm.applyRuleId,
      quantity: batchForm.quantity,
    })
    ElMessage.success(`已生成 ${result.length} 条条码`)
    batchVisible.value = false
    emit('success')
  } finally {
    batchSubmitting.value = false
  }
}

// ---------- 导入条码 ----------
const importVisible = ref(false)
const importSubmitting = ref(false)
const importForm = reactive({ applyRuleId: undefined as number | undefined, barcodeText: '' })

function openImport() {
  importForm.applyRuleId = undefined
  importForm.barcodeText = ''
  importVisible.value = true
}

async function submitImport() {
  if (!importForm.applyRuleId) {
    ElMessage.warning('请选择应用规则')
    return
  }
  const values = importForm.barcodeText
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
  if (values.length === 0) {
    ElMessage.warning('请输入至少一条条码值')
    return
  }
  importSubmitting.value = true
  try {
    const result = await importBarcodes({
      applyRuleId: importForm.applyRuleId,
      items: values.map((barcodeValue) => ({ barcodeValue })),
    })
    ElMessage.success(`导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
    importVisible.value = false
    emit('success')
  } finally {
    importSubmitting.value = false
  }
}

// ---------- 解析条码 ----------
const parseVisible = ref(false)
const parseSubmitting = ref(false)
const parseValue = ref('')
const parseResult = ref<BarcodeParseResp | null>(null)

function openParse() {
  parseValue.value = ''
  parseResult.value = null
  parseVisible.value = true
}

async function submitParse() {
  if (!parseValue.value.trim()) {
    ElMessage.warning('请输入条码值')
    return
  }
  parseSubmitting.value = true
  try {
    parseResult.value = await parseBarcode({ barcodeValue: parseValue.value.trim() })
  } finally {
    parseSubmitting.value = false
  }
}

defineExpose({ openGenerate, openBatch, openImport, openParse })
</script>

<template>
  <!-- 生成条码 -->
  <el-dialog v-model="generateVisible" title="生成条码" width="520px">
    <el-form label-width="90px">
      <el-form-item label="应用规则" required>
        <el-select v-model="generateForm.applyRuleId" filterable placeholder="选择应用规则">
          <el-option
            v-for="opt in applyRuleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="批次号">
        <el-input v-model="generateForm.batchNo" maxlength="64" placeholder="选填" />
      </el-form-item>
      <el-form-item label="工单ID">
        <el-input-number v-model="generateForm.workOrderId" :min="1" :controls="false" />
      </el-form-item>
      <el-form-item label="任务ID">
        <el-input-number v-model="generateForm.taskId" :min="1" :controls="false" />
      </el-form-item>
      <el-form-item label="产线编码">
        <el-input v-model="generateForm.lineCode" maxlength="64" placeholder="规则含产线变量时必填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="generateVisible = false">取消</el-button>
      <el-button type="primary" :loading="generateSubmitting" @click="submitGenerate">
        生成
      </el-button>
    </template>
  </el-dialog>

  <!-- 批量生成 -->
  <el-dialog v-model="batchVisible" title="批量生成" width="440px">
    <el-form label-width="90px">
      <el-form-item label="应用规则" required>
        <el-select v-model="batchForm.applyRuleId" filterable placeholder="选择应用规则">
          <el-option
            v-for="opt in applyRuleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="生成数量" required>
        <el-input-number v-model="batchForm.quantity" :min="1" :max="500" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchVisible = false">取消</el-button>
      <el-button type="primary" :loading="batchSubmitting" @click="submitBatch">
        批量生成
      </el-button>
    </template>
  </el-dialog>

  <!-- 导入条码 -->
  <el-dialog v-model="importVisible" title="导入条码" width="520px">
    <el-form label-width="90px">
      <el-form-item label="应用规则" required>
        <el-select v-model="importForm.applyRuleId" filterable placeholder="选择应用规则">
          <el-option
            v-for="opt in applyRuleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="条码值列表" required>
        <el-input
          v-model="importForm.barcodeText"
          type="textarea"
          :rows="8"
          placeholder="每行一个条码值，最多 500 条"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="importVisible = false">取消</el-button>
      <el-button type="primary" :loading="importSubmitting" @click="submitImport">
        导入
      </el-button>
    </template>
  </el-dialog>

  <!-- 解析条码 -->
  <el-dialog v-model="parseVisible" title="解析条码" width="560px">
    <el-form label-width="80px">
      <el-form-item label="条码值" required>
        <el-input v-model="parseValue" maxlength="64" placeholder="输入待解析的条码值" />
      </el-form-item>
    </el-form>
    <el-button type="primary" :loading="parseSubmitting" @click="submitParse">解析</el-button>
    <el-descriptions v-if="parseResult" :column="2" border class="parse-result" size="small">
      <el-descriptions-item label="条码值">{{ parseResult.barcodeValue }}</el-descriptions-item>
      <el-descriptions-item label="条码类型">
        {{ parseResult.barcodeTypeName ?? parseResult.barcodeTypeId }}
      </el-descriptions-item>
      <el-descriptions-item label="模式">
        {{ BARCODE_MODE_TEXT[parseResult.barcodeMode] }}
      </el-descriptions-item>
      <el-descriptions-item label="批次号">{{ parseResult.batchNo ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="产品">
        {{ parseResult.productCode ?? '-' }} {{ parseResult.productName ?? '' }}
      </el-descriptions-item>
      <el-descriptions-item label="物料">
        {{ parseResult.materialCode ?? '-' }} {{ parseResult.materialName ?? '' }}
      </el-descriptions-item>
      <el-descriptions-item label="工单ID">{{ parseResult.workOrderId ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="任务ID">{{ parseResult.taskId ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="来源">
        {{ BARCODE_SOURCE_TEXT[parseResult.sourceType] }}
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        {{ BARCODE_STATUS_MAP[parseResult.barcodeStatus]?.text ?? '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<style scoped>
.parse-result {
  margin-top: 16px;
}
</style>
