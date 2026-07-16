<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  INSPECTION_CONCLUSION_MAP,
  INSPECTION_CONCLUSION_OPTIONS,
  INSPECTION_TYPE_OPTIONS,
  INSPECTION_TYPE_TEXT,
  ITEM_JUDGMENT_RESULT_MAP,
  ITEM_JUDGMENT_RESULT_OPTIONS,
  RECORD_STATUS,
  RECORD_STATUS_MAP,
  RECORD_STATUS_OPTIONS,
  RELEASE_STATUS_MAP,
  QUALITY_WRITE_ROLES,
} from '@/constants/quality'
import { loadEffectiveInspectionPlanOptions } from '@/api/quality/options'
import {
  createInspectionRecord,
  getInspectionRecord,
  getInspectionRecordPage,
  saveInspectionResults,
  submitInspectionRecord,
} from '@/api/quality/inspectionRecord'
import type {
  InspectionRecord,
  InspectionRecordPageParams,
  InspectionResult,
  InspectionResultSaveReq,
  InspectionRecordSubmitReq,
} from '@/api/quality/inspectionRecord'

defineOptions({ name: 'QualityInspectionRecordList' })

const planOptions = ref<OptionItem[]>([])
onMounted(async () => {
  planOptions.value = await loadEffectiveInspectionPlanOptions()
})

const filterFields: FilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '检验单号或批次' },
  { prop: 'inspectionType', label: '检验类型', type: 'select', options: INSPECTION_TYPE_OPTIONS },
  { prop: 'recordStatus', label: '单据状态', type: 'select', options: RECORD_STATUS_OPTIONS },
  { prop: 'conclusion', label: '检验结论', type: 'select', options: INSPECTION_CONCLUSION_OPTIONS },
]

const columns: ColumnDef<InspectionRecord>[] = [
  { prop: 'inspectionNo', label: '检验单号', width: 150 },
  {
    prop: 'inspectionType',
    label: '检验类型',
    width: 100,
    formatter: (row) => INSPECTION_TYPE_TEXT[row.inspectionType] ?? row.inspectionType,
  },
  { prop: 'planCode', label: '方案编码', width: 130 },
  { prop: 'batchNo', label: '批次号', width: 140 },
  { prop: 'sampleQuantity', label: '抽样数', width: 80, align: 'center' },
  { prop: 'recordStatus', label: '单据状态', width: 90, statusMap: RECORD_STATUS_MAP },
  { prop: 'conclusion', label: '结论', width: 90, statusMap: INSPECTION_CONCLUSION_MAP },
  { prop: 'releaseStatus', label: '放行', width: 90, statusMap: RELEASE_STATUS_MAP },
  { prop: 'inspectedAt', label: '检验时间', width: 170 },
]

const rowActions: RowAction<InspectionRecord>[] = [
  {
    key: 'edit',
    label: '录入结果',
    roles: QUALITY_WRITE_ROLES,
    show: (row) => row.recordStatus === RECORD_STATUS.DRAFT,
  },
  {
    key: 'submit',
    label: '提交',
    type: 'success',
    roles: QUALITY_WRITE_ROLES,
    show: (row) => row.recordStatus === RECORD_STATUS.DRAFT,
  },
  { key: 'view', label: '查看详情' },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  InspectionRecord,
  InspectionRecordPageParams
>({ fetcher: getInspectionRecordPage })

// ---- 创建检验单弹窗 ----

interface RecordForm {
  inspectionType: string
  planId: number | undefined
  batchNo: string
  sampleQuantity: number
  workOrderId: number | undefined
}

const createDialog = useFormDialog<RecordForm>(
  () => ({
    inspectionType: 'FIRST_ARTICLE',
    planId: undefined,
    batchNo: '',
    sampleQuantity: 1,
    workOrderId: undefined,
  }),
  {
    titles: { create: '创建检验单' },
    submit: async (model) => {
      if (!model.planId) {
        ElMessage.warning('请选择检验方案')
        throw new Error('no plan')
      }
      await createInspectionRecord(model.inspectionType, {
        planId: model.planId,
        batchNo: model.batchNo,
        sampleQuantity: model.sampleQuantity,
        workOrderId: model.workOrderId || undefined,
      })
      ElMessage.success('检验单已创建')
    },
    onSuccess: refresh,
  },
)

const createRules = {
  planId: [{ required: true, message: '请选择检验方案', trigger: 'change' }],
  batchNo: [{ required: true, message: '请输入批次号', trigger: 'blur' }],
  sampleQuantity: [{ required: true, message: '请输入抽样数量', trigger: 'blur' }],
}

// ---- 结果录入抽屉 ----

const detailDrawer = ref(false)
const detailLoading = ref(false)
const detailRecord = ref<InspectionRecord | null>(null)
const editableResults = ref<InspectionResultSaveReq[]>([])

async function openDetail(row: InspectionRecord) {
  detailDrawer.value = true
  detailLoading.value = true
  try {
    const record = await getInspectionRecord(row.id)
    detailRecord.value = record
    editableResults.value = record.results.map((item) => ({
      resultId: item.id,
      measuredValue: item.measuredValue ?? undefined,
      judgmentResult: item.judgmentResult ?? undefined,
      defectDescription: item.defectDescription ?? undefined,
    }))
  } finally {
    detailLoading.value = false
  }
}

async function saveResults() {
  if (!detailRecord.value) return
  await saveInspectionResults(detailRecord.value.id, { results: editableResults.value })
  ElMessage.success('检验结果已保存')
  await openDetail(detailRecord.value)
}

// ---- 提交检验单 ----

const submitDialogVisible = ref(false)
const submitLoading = ref(false)
const submitForm = ref<InspectionRecordSubmitReq>({
  conclusion: 'PASS',
  nonconformanceDescription: '',
  disposition: '',
})

function openSubmit(row: InspectionRecord) {
  submitForm.value = { conclusion: 'PASS', nonconformanceDescription: '', disposition: '' }
  submitDialogVisible.value = true
  openDetail(row)
}

async function handleSubmit() {
  if (!detailRecord.value) return
  submitLoading.value = true
  try {
    await saveInspectionResults(detailRecord.value.id, { results: editableResults.value })
    await submitInspectionRecord(detailRecord.value.id, submitForm.value)
    ElMessage.success('检验单已提交')
    submitDialogVisible.value = false
    detailDrawer.value = false
    await refresh()
  } finally {
    submitLoading.value = false
  }
}

async function handleRowAction(key: string, row: InspectionRecord) {
  if (key === 'edit' || key === 'view') {
    await openDetail(row)
  } else if (key === 'submit') {
    openSubmit(row)
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="检验单" description="按检验方案执行质量检验，录入实测结果并提交结论" />
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
        <PermissionButton :roles="QUALITY_WRITE_ROLES" type="primary" @click="createDialog.open()">
          创建检验单
        </PermissionButton>
      </template>
    </FilterTable>

    <!-- 创建检验单 -->
    <FormDialog
      v-model:visible="createDialog.visible.value"
      :title="createDialog.title.value"
      :model="createDialog.model.value"
      :rules="createRules"
      :submit-loading="createDialog.submitLoading.value"
      @submit="createDialog.handleSubmit"
    >
      <el-form-item label="检验类型" prop="inspectionType">
        <el-select v-model="createDialog.model.value.inspectionType">
          <el-option
            v-for="opt in INSPECTION_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="检验方案" prop="planId">
        <el-select v-model="createDialog.model.value.planId" filterable>
          <el-option
            v-for="opt in planOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="批次号" prop="batchNo">
        <el-input v-model="createDialog.model.value.batchNo" maxlength="64" />
      </el-form-item>
      <el-form-item label="抽样数量" prop="sampleQuantity">
        <el-input-number v-model="createDialog.model.value.sampleQuantity" :min="1" />
      </el-form-item>
      <el-form-item label="工单 ID" prop="workOrderId">
        <el-input-number
          v-model="createDialog.model.value.workOrderId"
          :min="1"
          placeholder="选填"
        />
      </el-form-item>
    </FormDialog>

    <!-- 结果录入抽屉 -->
    <el-drawer
      v-model="detailDrawer"
      :title="`检验单 ${detailRecord?.inspectionNo ?? ''}`"
      size="70%"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailRecord" :column="3" border>
          <el-descriptions-item label="检验类型">
            {{ INSPECTION_TYPE_TEXT[detailRecord.inspectionType] }}
          </el-descriptions-item>
          <el-descriptions-item label="方案编码">{{ detailRecord.planCode }}</el-descriptions-item>
          <el-descriptions-item label="方案版本">V{{ detailRecord.planVersion }}</el-descriptions-item>
          <el-descriptions-item label="批次号">{{ detailRecord.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="抽样数量">
            {{ detailRecord.sampleQuantity }}
          </el-descriptions-item>
          <el-descriptions-item label="单据状态">
            {{ RECORD_STATUS_MAP[detailRecord.recordStatus]?.text }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">检验项目结果</el-divider>
        <el-table :data="detailRecord?.results" border size="small">
          <el-table-column label="项目编码" prop="itemCode" width="120" />
          <el-table-column label="项目名称" prop="itemName" min-width="120" />
          <el-table-column label="标准值" prop="standardValue" width="90" />
          <el-table-column label="下限" prop="lowerLimit" width="70" />
          <el-table-column label="上限" prop="upperLimit" width="70" />
          <el-table-column label="实测值" width="120">
            <template #default="{ row, $index }">
              <el-input
                v-if="detailRecord?.recordStatus === RECORD_STATUS.DRAFT"
                v-model="editableResults[$index].measuredValue"
                size="small"
              />
              <span v-else>{{ row.measuredValue }}</span>
            </template>
          </el-table-column>
          <el-table-column label="判定结果" width="120">
            <template #default="{ row, $index }">
              <el-select
                v-if="detailRecord?.recordStatus === RECORD_STATUS.DRAFT"
                v-model="editableResults[$index].judgmentResult"
                size="small"
              >
                <el-option
                  v-for="opt in ITEM_JUDGMENT_RESULT_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-tag
                v-else
                :type="ITEM_JUDGMENT_RESULT_MAP[row.judgmentResult]?.type"
                size="small"
              >
                {{ ITEM_JUDGMENT_RESULT_MAP[row.judgmentResult]?.text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="缺陷描述" min-width="140">
            <template #default="{ row, $index }">
              <el-input
                v-if="detailRecord?.recordStatus === RECORD_STATUS.DRAFT"
                v-model="editableResults[$index].defectDescription"
                size="small"
              />
              <span v-else>{{ row.defectDescription }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="detailRecord?.recordStatus === RECORD_STATUS.DRAFT" class="drawer-footer">
          <el-button type="primary" @click="saveResults">保存结果</el-button>
          <el-button type="success" @click="submitDialogVisible = true">提交检验单</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 提交检验单弹窗 -->
    <el-dialog v-model="submitDialogVisible" title="提交检验单" width="480px">
      <el-form label-width="100px">
        <el-form-item label="检验结论">
          <el-select v-model="submitForm.conclusion">
            <el-option
              v-for="opt in INSPECTION_CONCLUSION_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="不合格描述">
          <el-input v-model="submitForm.nonconformanceDescription" type="textarea" maxlength="500" />
        </el-form-item>
        <el-form-item label="处置方式">
          <el-input v-model="submitForm.disposition" type="textarea" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确认提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
.drawer-footer {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
