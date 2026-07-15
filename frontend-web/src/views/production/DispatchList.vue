<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  ColumnDef,
  FilterField,
  GanttRow,
  GanttTask,
  OptionItem,
  RowAction,
} from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import GanttSchedule from '@/components/business/GanttSchedule.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import ProTable from '@/components/base/ProTable.vue'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import {
  DISPATCH_AUDIT_ROLES,
  DISPATCH_EDIT_ROLES,
  DISPATCH_STATUS,
  DISPATCH_STATUS_MAP,
  DISPATCH_ADJUST_TYPE_TEXT,
  KIT_STATUS_MAP,
  SHIFT_OPTIONS,
  SHIFT_SEEDS,
  WO_STATUS,
  remainingDispatchQuantity,
  statusMapToOptions,
} from '@/constants/production'
import { loadLineOptions } from '@/api/production/options'
import { getWorkOrder, getWorkOrderPage } from '@/api/production/workOrder'
import type { WorkOrder } from '@/api/production/workOrder'
import {
  auditDispatch,
  cancelDispatch,
  createDispatch,
  getDispatchAdjustLogs,
  getDispatchPage,
  getLineSchedule,
  issueDispatch,
  suggestDispatch,
  updateDispatch,
} from '@/api/production/dispatch'
import type {
  DispatchAdjustLog,
  DispatchOrder,
  DispatchPageParams,
  DispatchSuggest,
} from '@/api/production/dispatch'

defineOptions({ name: 'DispatchList' })

const activeTab = ref('list')

// ---------- 下拉选项 ----------

const lineOptions = ref<OptionItem[]>([])
/** 可派工单：已下达/生产中 */
const workOrderOptions = ref<OptionItem[]>([])

const filterFields = ref<FilterField[]>([
  { prop: 'lineId', label: '产线', type: 'select', options: [] },
  { prop: 'shiftId', label: '班次', type: 'select', options: SHIFT_OPTIONS },
  {
    prop: 'dispatchStatus',
    label: '派工状态',
    type: 'select',
    options: statusMapToOptions(DISPATCH_STATUS_MAP),
  },
  { prop: 'planDateRange', label: '排产日期', type: 'dateRange' },
])

async function loadDispatchableOrders() {
  const [released, producing] = await Promise.all([
    getWorkOrderPage({ pageNo: 1, pageSize: 100, orderStatus: WO_STATUS.RELEASED }),
    getWorkOrderPage({ pageNo: 1, pageSize: 100, orderStatus: WO_STATUS.IN_PRODUCTION }),
  ])
  workOrderOptions.value = [...released.list, ...producing.list].map((order) => ({
    label: `${order.workOrderNo} ${order.productName}`,
    value: order.id,
  }))
}

onMounted(async () => {
  try {
    lineOptions.value = await loadLineOptions()
    const field = filterFields.value.find((item) => item.prop === 'lineId')
    if (field) field.options = lineOptions.value
    await loadDispatchableOrders()
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

// ---------- 派工列表 ----------

const columns: ColumnDef<DispatchOrder>[] = [
  { prop: 'dispatchNo', label: '派工单号', width: 170, fixed: 'left' },
  { prop: 'workOrderNo', label: '工单号', width: 160 },
  { prop: 'productName', label: '产品', minWidth: 120 },
  { prop: 'kitStatus', label: '齐套', width: 92, statusMap: KIT_STATUS_MAP },
  { prop: 'lineName', label: '产线', width: 110 },
  { prop: 'shiftName', label: '班次', width: 76 },
  { prop: 'planDate', label: '排产日期', width: 108 },
  { prop: 'planQuantity', label: '计划数', width: 88, align: 'right' },
  { prop: 'planStartTime', label: '计划开始', width: 160 },
  { prop: 'planEndTime', label: '计划结束', width: 160 },
  { prop: 'dispatchStatus', label: '状态', width: 90, statusMap: DISPATCH_STATUS_MAP },
]

const rowActions: RowAction<DispatchOrder>[] = [
  {
    key: 'audit',
    label: '审核',
    type: 'success',
    roles: DISPATCH_AUDIT_ROLES,
    confirm: '确认审核通过该派工单？',
    show: (row) => row.dispatchStatus === DISPATCH_STATUS.PENDING_AUDIT,
  },
  {
    key: 'issue',
    label: '下发',
    type: 'success',
    roles: DISPATCH_AUDIT_ROLES,
    confirm: '下发后产线即可执行，确认下发？',
    show: (row) => row.dispatchStatus === DISPATCH_STATUS.AUDITED,
  },
  {
    key: 'edit',
    label: '调整',
    roles: DISPATCH_EDIT_ROLES,
    show: (row) =>
      row.dispatchStatus === DISPATCH_STATUS.PENDING_AUDIT ||
      row.dispatchStatus === DISPATCH_STATUS.AUDITED ||
      row.dispatchStatus === DISPATCH_STATUS.ISSUED,
  },
  {
    key: 'cancel',
    label: '取消',
    type: 'danger',
    roles: DISPATCH_EDIT_ROLES,
    show: (row) =>
      row.dispatchStatus === DISPATCH_STATUS.PENDING_AUDIT ||
      row.dispatchStatus === DISPATCH_STATUS.AUDITED ||
      row.dispatchStatus === DISPATCH_STATUS.ISSUED,
  },
  { key: 'logs', label: '日志' },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  DispatchOrder,
  DispatchPageParams
>({ fetcher: getDispatchPage })

function handleQuery(params: Record<string, any>) {
  const { planDateRange, ...rest } = params
  if (Array.isArray(planDateRange) && planDateRange.length === 2) {
    rest.planDateBegin = planDateRange[0]
    rest.planDateEnd = planDateRange[1]
  }
  query(rest as DispatchPageParams)
}

// ---------- 创建 / 调整弹窗 ----------

interface DispatchForm {
  id?: number
  workOrderId: number | null
  lineId: number | null
  shiftId: number | null
  planDate: string
  planQuantity: number
  planStartTime: string
  planEndTime: string
  suggest: boolean
  adjustReason: string
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const editingStatus = ref<number>()
const form = ref<DispatchForm>(emptyForm())
const formRef = ref()

/** 选中工单的完整信息（剩余可派提示用） */
const selectedOrder = ref<WorkOrder>()
const suggests = ref<DispatchSuggest[]>([])
const suggestLoading = ref(false)

function emptyForm(): DispatchForm {
  return {
    workOrderId: null,
    lineId: null,
    shiftId: null,
    planDate: '',
    planQuantity: 1,
    planStartTime: '',
    planEndTime: '',
    suggest: false,
    adjustReason: '',
  }
}

const adjustReasonRequired = computed(
  () => dialogMode.value === 'edit' && editingStatus.value === DISPATCH_STATUS.ISSUED,
)

const formRules = computed(() => ({
  workOrderId: [{ required: true, message: '请选择生产工单', trigger: 'change' }],
  lineId: [{ required: true, message: '请选择产线', trigger: 'change' }],
  shiftId: [{ required: true, message: '请选择班次', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择排产日期', trigger: 'change' }],
  planQuantity: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
  planStartTime: [{ required: true, message: '请选择计划开始时间', trigger: 'change' }],
  planEndTime: [{ required: true, message: '请选择计划结束时间', trigger: 'change' }],
  adjustReason: adjustReasonRequired.value
    ? [{ required: true, message: '已下发派工单调整必须填写原因', trigger: 'blur' }]
    : [],
}))

const remaining = computed(() => {
  if (!selectedOrder.value) return null
  return remainingDispatchQuantity(selectedOrder.value)
})

const overDispatch = computed(
  () => remaining.value !== null && form.value.planQuantity > remaining.value,
)

async function handleWorkOrderChange(workOrderId: number | null) {
  selectedOrder.value = undefined
  suggests.value = []
  if (!workOrderId) return
  try {
    selectedOrder.value = await getWorkOrder(workOrderId)
  } catch {
    // 工单详情加载失败仅缺剩余可派提示
  }
}

/** 班次 + 日期变化 → 预填计划起止（夜班跨天） */
function prefillShiftTime() {
  const { shiftId, planDate } = form.value
  const shift = SHIFT_SEEDS.find((item) => item.id === shiftId)
  if (!shift || !planDate) return
  form.value.planStartTime = `${planDate} ${shift.startTime}`
  const endDate = shift.crossDay ? addDays(planDate, 1) : planDate
  form.value.planEndTime = `${endDate} ${shift.endTime}`
}

function addDays(dateStr: string, days: number): string {
  const date = new Date(`${dateStr}T00:00:00`)
  date.setDate(date.getDate() + days)
  return formatDateTime(date, false)
}

watch(() => [form.value.shiftId, form.value.planDate], prefillShiftTime)

function openCreate() {
  dialogMode.value = 'create'
  form.value = emptyForm()
  selectedOrder.value = undefined
  suggests.value = []
  dialogVisible.value = true
}

function openEdit(row: DispatchOrder) {
  dialogMode.value = 'edit'
  editingStatus.value = row.dispatchStatus
  form.value = {
    id: row.id,
    workOrderId: row.workOrderId,
    lineId: row.lineId,
    shiftId: row.shiftId,
    planDate: row.planDate,
    planQuantity: row.planQuantity,
    planStartTime: row.planStartTime,
    planEndTime: row.planEndTime,
    suggest: false,
    adjustReason: '',
  }
  suggests.value = []
  dialogVisible.value = true
  void handleWorkOrderChange(row.workOrderId)
}

async function loadSuggests() {
  if (!form.value.workOrderId) {
    ElMessage.info('请先选择生产工单')
    return
  }
  suggestLoading.value = true
  try {
    suggests.value = await suggestDispatch(form.value.workOrderId)
    if (suggests.value.length === 0) {
      ElMessage.info('交期内无可行排产建议（检查产线产能与工厂日历）')
    }
  } catch {
    // 建议失败不影响手工填写
  } finally {
    suggestLoading.value = false
  }
}

/** 采纳建议行：整行回填 + 标记 suggest */
function adoptSuggest(row: DispatchSuggest) {
  form.value.lineId = row.lineId
  form.value.shiftId = row.shiftId
  form.value.planDate = row.planDate
  form.value.planQuantity = row.planQuantity
  form.value.planStartTime = row.planStartTime
  form.value.planEndTime = row.planEndTime
  form.value.suggest = true
  ElMessage.success('已采纳该建议，可微调后提交')
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (form.value.planEndTime <= form.value.planStartTime) {
    ElMessage.warning('计划结束时间必须晚于开始时间')
    return
  }
  if (overDispatch.value) {
    try {
      await ElMessageBox.confirm(
        `计划数量超出剩余可派 ${remaining.value} 个，后端将拒绝该请求，仍要提交？`,
        '超派提醒',
        { type: 'warning' },
      )
    } catch {
      return
    }
  }
  const payload = {
    workOrderId: form.value.workOrderId!,
    lineId: form.value.lineId!,
    shiftId: form.value.shiftId!,
    planDate: form.value.planDate,
    planQuantity: form.value.planQuantity,
    planStartTime: form.value.planStartTime,
    planEndTime: form.value.planEndTime,
    suggest: form.value.suggest || undefined,
    adjustReason: form.value.adjustReason || undefined,
  }
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createDispatch(payload)
      ElMessage.success('派工单已创建（待审核）')
    } else {
      await updateDispatch(form.value.id!, payload)
      ElMessage.success('派工单已调整')
    }
    dialogVisible.value = false
    await refresh()
  } catch {
    // 提示由拦截器弹出
  } finally {
    submitLoading.value = false
  }
}

// ---------- 调整日志 ----------

const logsVisible = ref(false)
const logsLoading = ref(false)
const logs = ref<DispatchAdjustLog[]>([])
const logsDispatchNo = ref('')

const logColumns: ColumnDef<DispatchAdjustLog>[] = [
  { prop: 'adjustType', label: '类型', width: 100, formatter: (row) => DISPATCH_ADJUST_TYPE_TEXT[row.adjustType] ?? String(row.adjustType) },
  { prop: 'adjustReason', label: '原因', minWidth: 160 },
  { prop: 'beforeSnapshot', label: '调整前', minWidth: 200 },
  { prop: 'afterSnapshot', label: '调整后', minWidth: 200 },
  { prop: 'operatorId', label: '操作人', width: 90, formatter: (row) => `#${row.operatorId}` },
  { prop: 'createTime', label: '时间', width: 170 },
]

async function openLogs(row: DispatchOrder) {
  logsDispatchNo.value = row.dispatchNo
  logsVisible.value = true
  logsLoading.value = true
  try {
    logs.value = await getDispatchAdjustLogs(row.id)
  } finally {
    logsLoading.value = false
  }
}

// ---------- 行操作 ----------

async function handleRowAction(key: string, row: DispatchOrder) {
  if (key === 'edit') {
    openEdit(row)
    return
  }
  if (key === 'logs') {
    await openLogs(row)
    return
  }
  try {
    if (key === 'audit') {
      await auditDispatch(row.id)
      ElMessage.success('已审核')
    } else if (key === 'issue') {
      await issueDispatch(row.id)
      ElMessage.success('已下发')
    } else if (key === 'cancel') {
      const { value } = await ElMessageBox.prompt('取消将回退工单已派数量，请填写原因', '取消派工单', {
        inputPattern: /\S+/,
        inputErrorMessage: '原因不能为空',
        type: 'warning',
      })
      await cancelDispatch(row.id, value.trim())
      ElMessage.success('已取消')
    }
  } catch {
    // 用户取消输入或请求失败
  } finally {
    if (key !== 'logs') await refresh()
  }
}

// ---------- 产线排程甘特 ----------

const scheduleRange = ref<[string, string]>([today(), addDays(today(), 6)])
const scheduleLoading = ref(false)
const scheduleTasks = ref<GanttTask[]>([])

function today(): string {
  return formatDateTime(new Date(), false)
}

const ganttRows = computed<GanttRow[]>(() =>
  lineOptions.value.map((opt) => ({ key: String(opt.value), label: opt.label })),
)

async function loadSchedule() {
  if (lineOptions.value.length === 0) return
  const [start, end] = scheduleRange.value
  scheduleLoading.value = true
  try {
    const results = await Promise.all(
      lineOptions.value.map((opt) =>
        getLineSchedule(opt.value as number, start, end).catch(() => [] as DispatchOrder[]),
      ),
    )
    scheduleTasks.value = results.flat().map((item) => ({
      id: item.id,
      rowKey: String(item.lineId),
      name: `${item.workOrderNo}×${item.planQuantity}`,
      start: item.planStartTime,
      end: item.planEndTime,
      status: String(item.dispatchStatus),
    }))
  } finally {
    scheduleLoading.value = false
  }
}

function handleTabChange(name: string | number) {
  if (name === 'schedule' && scheduleTasks.value.length === 0) {
    void loadSchedule()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="派工管理"
      description="工单拆分到产线×班次×日期执行：待审核 → 已审核 → 已下发；取消回退工单已派数量"
    />

    <el-tabs v-model="activeTab" class="dispatch-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="派工列表" name="list">
        <FilterTable
          :filter-fields="filterFields"
          :columns="columns"
          :data="data"
          :loading="loading"
          :pagination="pagination"
          :row-actions="rowActions"
          :action-width="240"
          @query="handleQuery"
          @reset="reset"
          @page-change="onPageChange"
          @row-action="handleRowAction"
        >
          <template #toolbar>
            <PermissionButton :roles="DISPATCH_EDIT_ROLES" type="primary" @click="openCreate">
              创建派工单
            </PermissionButton>
          </template>
        </FilterTable>
      </el-tab-pane>

      <el-tab-pane label="产线排程" name="schedule">
        <el-card shadow="never">
          <div class="schedule-toolbar">
            <el-date-picker
              v-model="scheduleRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :clearable="false"
            />
            <el-button type="primary" :loading="scheduleLoading" @click="loadSchedule">
              刷新排程
            </el-button>
          </div>
          <GanttSchedule
            :rows="ganttRows"
            :tasks="scheduleTasks"
            :status-map="DISPATCH_STATUS_MAP"
            :loading="scheduleLoading"
            :time-range="[`${scheduleRange[0]} 00:00:00`, `${scheduleRange[1]} 23:59:59`]"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建 / 调整弹窗：含排产建议区，宽版布局 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '创建派工单' : '调整派工单'"
      width="880px"
      destroy-on-close
      :close-on-click-modal="false"
      append-to-body
    >
      <el-scrollbar max-height="64vh">
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="生产工单" prop="workOrderId">
                <el-select
                  v-model="form.workOrderId"
                  filterable
                  placeholder="已下达/生产中的工单"
                  :disabled="dialogMode === 'edit'"
                  @change="handleWorkOrderChange"
                >
                  <el-option
                    v-for="opt in workOrderOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label-width="0">
                <el-alert
                  v-if="selectedOrder"
                  :type="overDispatch ? 'warning' : 'info'"
                  :closable="false"
                  show-icon
                  class="remaining-alert"
                >
                  <template #title>
                    计划 {{ selectedOrder.planQuantity }} · 已派
                    {{ selectedOrder.dispatchedQuantity }} · 剩余可派
                    <b>{{ remaining }}</b>
                    <template v-if="overDispatch">，当前填写已超派</template>
                  </template>
                </el-alert>
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">
            排产建议
            <el-button
              link
              type="primary"
              :loading="suggestLoading"
              class="suggest-btn"
              @click="loadSuggests"
            >
              获取建议
            </el-button>
          </el-divider>
          <el-table v-if="suggests.length" :data="suggests" border size="small" class="suggest-table">
            <el-table-column prop="lineName" label="产线" min-width="110" />
            <el-table-column prop="shiftName" label="班次" width="70" />
            <el-table-column prop="planDate" label="日期" width="100" />
            <el-table-column prop="planQuantity" label="建议数量" width="90" align="right" />
            <el-table-column prop="planStartTime" label="开始" width="150" />
            <el-table-column prop="planEndTime" label="结束" width="150" />
            <el-table-column label="按期" width="70" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.canFinishOnTime ? 'success' : 'danger'" size="small" disable-transitions>
                  {{ scope.row.canFinishOnTime ? '能' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="scope">
                <el-button link type="primary" size="small" @click="adoptSuggest(scope.row as DispatchSuggest)">
                  采纳
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="suggest-empty">
            选择工单后点击「获取建议」，按交期内工作日与产线剩余产能自动填充
          </div>

          <el-divider content-position="left">派工明细</el-divider>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="产线" prop="lineId">
                <el-select v-model="form.lineId" filterable placeholder="请选择">
                  <el-option
                    v-for="opt in lineOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="班次" prop="shiftId">
                <el-select v-model="form.shiftId" placeholder="选择后预填时间">
                  <el-option
                    v-for="opt in SHIFT_OPTIONS"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="排产日期" prop="planDate">
                <el-date-picker
                  v-model="form.planDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                  class="full-width"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="计划数量" prop="planQuantity">
                <el-input-number
                  v-model="form.planQuantity"
                  :min="1"
                  :step="100"
                  controls-position="right"
                  class="full-width"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="计划开始" prop="planStartTime">
                <el-date-picker
                  v-model="form.planStartTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="班次预填可改"
                  class="full-width"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="计划结束" prop="planEndTime">
                <el-date-picker
                  v-model="form.planEndTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="夜班自动跨天"
                  class="full-width"
                />
              </el-form-item>
            </el-col>
            <el-col v-if="dialogMode === 'edit'" :span="24">
              <el-form-item label="调整原因" prop="adjustReason">
                <el-input
                  v-model="form.adjustReason"
                  type="textarea"
                  :rows="2"
                  maxlength="255"
                  show-word-limit
                  :placeholder="adjustReasonRequired ? '已下发派工单调整必填' : '选填'"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-scrollbar>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="logsVisible"
      :title="`调整日志 - ${logsDispatchNo}`"
      width="960px"
      append-to-body
    >
      <ProTable :columns="logColumns" :data="logs" :loading="logsLoading" />
    </el-dialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.dispatch-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.full-width {
  width: 100%;
}

.remaining-alert {
  width: 100%;
}

.suggest-btn {
  margin-left: 8px;
}

.suggest-table {
  margin-bottom: 8px;
}

.suggest-empty {
  padding: 8px 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.schedule-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}
</style>
