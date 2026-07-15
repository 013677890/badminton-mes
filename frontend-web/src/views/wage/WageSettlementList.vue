<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import ApprovalActionBar from '@/components/business/ApprovalActionBar.vue'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { usePermission } from '@/composables/usePermission'
import { ROLE_SEED_IDS, statusMapToOptions } from '@/constants/production'
import {
  SETTLEMENT_ACTION_TEXT,
  SETTLEMENT_STATUS,
  SETTLEMENT_STATUS_MAP,
  WAGE_MANAGE_ROLES,
} from '@/constants/wage'
import { formatNumber } from '@/utils/format'
import {
  loadProcessOptions,
  loadProductOptions,
  loadRoleUserOptions,
} from '@/api/production/options'
import {
  adjustSettlementDetail,
  approveSettlement,
  calculateSettlement,
  getSettlement,
  getSettlementAuditLogPage,
  getSettlementDetailPage,
  getSettlementPage,
  recalculateSettlement,
  rejectSettlement,
  submitSettlement,
  summarizeEmployees,
  summarizeProcesses,
} from '@/api/wage/settlement'
import type {
  EmployeeWageSummary,
  ProcessWageSummary,
  WageSettlement,
  WageSettlementAuditLog,
  WageSettlementDetail,
  WageSettlementPageParams,
} from '@/api/wage/settlement'

defineOptions({ name: 'WageSettlementList' })

const { hasRole } = usePermission()
const canManage = computed(() => hasRole(WAGE_MANAGE_ROLES))

// ---------- 下拉选项 ----------

const employeeOptions = ref<OptionItem[]>([])
const processOptions = ref<OptionItem[]>([])
const productOptions = ref<OptionItem[]>([])

onMounted(async () => {
  try {
    const [operators, leaders, processes, products] = await Promise.all([
      loadRoleUserOptions(ROLE_SEED_IDS.OPERATOR),
      loadRoleUserOptions(ROLE_SEED_IDS.TEAM_LEADER),
      loadProcessOptions(),
      loadProductOptions(),
    ])
    const merged = new Map<string | number, OptionItem>()
    for (const opt of [...operators, ...leaders]) merged.set(opt.value, opt)
    employeeOptions.value = [...merged.values()]
    processOptions.value = processes
    productOptions.value = products
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

function optionLabel(options: OptionItem[], value: number): string {
  return String(options.find((opt) => opt.value === value)?.label ?? value)
}

// ---------- 列表 ----------

const filterFields: FilterField[] = [
  {
    prop: 'settlementStatus',
    label: '状态',
    type: 'select',
    options: statusMapToOptions(SETTLEMENT_STATUS_MAP),
  },
  { prop: 'periodRange', label: '结算期间', type: 'dateRange', span: 8 },
]

const columns: ColumnDef<WageSettlement>[] = [
  { prop: 'settlementNo', label: '结算单号', width: 170 },
  {
    prop: 'periodStart',
    label: '结算期间',
    width: 200,
    formatter: (row) => `${row.periodStart} ~ ${row.periodEnd}`,
  },
  {
    prop: 'totalQualifiedQuantity',
    label: '合格数',
    width: 90,
    align: 'right',
    formatter: (row) => formatNumber(row.totalQualifiedQuantity),
  },
  {
    prop: 'totalDefectQuantity',
    label: '不良数',
    width: 90,
    align: 'right',
    formatter: (row) => formatNumber(row.totalDefectQuantity),
  },
  {
    prop: 'totalAmount',
    label: '总金额（元）',
    width: 120,
    align: 'right',
    formatter: (row) => row.totalAmount.toFixed(2),
  },
  { prop: 'settlementStatus', label: '状态', width: 90, statusMap: SETTLEMENT_STATUS_MAP },
  { prop: 'submitTime', label: '提交时间', width: 170, formatter: (row) => row.submitTime ?? '-' },
  { prop: 'auditTime', label: '审核时间', width: 170, formatter: (row) => row.auditTime ?? '-' },
]

const rowActions: RowAction<WageSettlement>[] = [
  { key: 'detail', label: '明细' },
  {
    key: 'recalculate',
    label: '重算',
    roles: WAGE_MANAGE_ROLES,
    confirm: '按原期间范围重新取数计算，当前明细（含人工调整）将被替换，确认？',
    show: (row) =>
      row.settlementStatus === SETTLEMENT_STATUS.DRAFT ||
      row.settlementStatus === SETTLEMENT_STATUS.REJECTED,
  },
  {
    key: 'submit',
    label: '提交审核',
    type: 'primary',
    roles: WAGE_MANAGE_ROLES,
    confirm: '提交后进入待审核，不可再调整明细，确认？',
    show: (row) => row.settlementStatus === SETTLEMENT_STATUS.DRAFT,
  },
  {
    key: 'audit',
    label: '审核',
    type: 'warning',
    roles: WAGE_MANAGE_ROLES,
    show: (row) => row.settlementStatus === SETTLEMENT_STATUS.PENDING,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  WageSettlement,
  WageSettlementPageParams
>({ fetcher: getSettlementPage })

/** dateRange 拆为后端的期间过滤字段 */
function handleQuery(params: Record<string, any>) {
  const { periodRange, ...rest } = params
  if (Array.isArray(periodRange) && periodRange.length === 2) {
    rest.periodStartBegin = periodRange[0]
    rest.periodEndEnd = periodRange[1]
  }
  query(rest as WageSettlementPageParams)
}

// ---------- 计算结算 ----------

interface CalculateForm {
  periodRange: [string, string] | null
  employeeIds: number[]
  reason: string
}

const calculateDialog = useFormDialog<CalculateForm>(
  () => ({ periodRange: null, employeeIds: [], reason: '' }),
  {
    titles: { create: '计算结算批次' },
    submit: async (model) => {
      const [periodStart, periodEnd] = model.periodRange!
      await calculateSettlement({
        periodStart,
        periodEnd,
        employeeIds: model.employeeIds.length ? model.employeeIds : undefined,
        reason: model.reason || undefined,
      })
      ElMessage.success('结算批次已生成（草稿），请核对明细后提交审核')
    },
    onSuccess: refresh,
  },
)

const calculateRules = {
  periodRange: [{ required: true, message: '请选择结算期间', trigger: 'change' }],
}

// ---------- 明细抽屉 ----------

const drawerVisible = ref(false)
const drawerTab = ref('details')
const current = ref<WageSettlement>()
const currentLoading = ref(false)
const actionLoading = ref(false)

const details = ref<WageSettlementDetail[]>([])
const detailLoading = ref(false)
const detailPagination = ref({ pageNo: 1, pageSize: 10, total: 0 })

const auditLogs = ref<WageSettlementAuditLog[]>([])
const auditLogLoading = ref(false)
const auditLogPagination = ref({ pageNo: 1, pageSize: 10, total: 0 })

const canAdjust = computed(
  () => canManage.value && current.value?.settlementStatus === SETTLEMENT_STATUS.DRAFT,
)

function openDrawer(row: WageSettlement) {
  current.value = row
  drawerTab.value = 'details'
  detailPagination.value = { pageNo: 1, pageSize: 10, total: 0 }
  auditLogPagination.value = { pageNo: 1, pageSize: 10, total: 0 }
  details.value = []
  auditLogs.value = []
  drawerVisible.value = true
  void reloadCurrent()
  void loadDetails()
  void loadAuditLogs()
}

/** 流转/调整后重取最新状态与乐观锁版本 */
async function reloadCurrent() {
  if (!current.value) return
  currentLoading.value = true
  try {
    current.value = await getSettlement(current.value.id)
  } finally {
    currentLoading.value = false
  }
}

async function loadDetails() {
  if (!current.value) return
  detailLoading.value = true
  try {
    const page = await getSettlementDetailPage(current.value.id, {
      pageNo: detailPagination.value.pageNo,
      pageSize: detailPagination.value.pageSize,
    })
    details.value = page.list
    detailPagination.value.total = page.total
  } finally {
    detailLoading.value = false
  }
}

async function loadAuditLogs() {
  if (!current.value) return
  auditLogLoading.value = true
  try {
    const page = await getSettlementAuditLogPage(current.value.id, {
      pageNo: auditLogPagination.value.pageNo,
      pageSize: auditLogPagination.value.pageSize,
    })
    auditLogs.value = page.list
    auditLogPagination.value.total = page.total
  } finally {
    auditLogLoading.value = false
  }
}

function onDetailPageChange(pageNo: number) {
  detailPagination.value.pageNo = pageNo
  void loadDetails()
}

function onAuditLogPageChange(pageNo: number) {
  auditLogPagination.value.pageNo = pageNo
  void loadAuditLogs()
}

// ----- 审核（抽屉底部 ApprovalActionBar） -----

async function handleApprove(comment?: string) {
  if (!current.value) return
  actionLoading.value = true
  try {
    await approveSettlement(current.value.id, {
      version: current.value.version,
      reason: comment,
    })
    ElMessage.success('结算已审核通过')
    await reloadCurrent()
    await loadAuditLogs()
    await refresh()
  } catch {
    await reloadCurrent()
  } finally {
    actionLoading.value = false
  }
}

async function handleReject(reason: string) {
  if (!current.value) return
  actionLoading.value = true
  try {
    await rejectSettlement(current.value.id, {
      version: current.value.version,
      reason,
    })
    ElMessage.success('结算已驳回，可重算后再次提交')
    await reloadCurrent()
    await loadAuditLogs()
    await refresh()
  } catch {
    await reloadCurrent()
  } finally {
    actionLoading.value = false
  }
}

// ----- 明细金额调整 -----

interface AdjustForm {
  detailId?: number
  employeeText: string
  calculatedAmount: number
  adjustedAmount: number
  reason: string
}

const adjustDialog = useFormDialog<AdjustForm>(
  () => ({ employeeText: '', calculatedAmount: 0, adjustedAmount: 0, reason: '' }),
  {
    titles: { edit: '调整明细金额' },
    submit: async (model) => {
      await adjustSettlementDetail(current.value!.id, model.detailId!, {
        settlementVersion: current.value!.version,
        adjustedAmount: model.adjustedAmount,
        reason: model.reason,
      })
      ElMessage.success('明细金额已调整')
    },
    onSuccess: () => {
      void reloadCurrent()
      void loadDetails()
      void loadAuditLogs()
      void refresh()
    },
  },
)

const adjustRules = {
  reason: [{ required: true, message: '请填写调整原因', trigger: 'blur' }],
}

function openAdjust(row: WageSettlementDetail) {
  adjustDialog.open('edit', {
    detailId: row.id,
    employeeText: `${optionLabel(employeeOptions.value, row.employeeId)} · ${row.workDate}`,
    calculatedAmount: row.calculatedAmount,
    adjustedAmount: row.finalAmount,
    reason: '',
  })
}

// ---------- 工资汇总 ----------

const summaryVisible = ref(false)
const summaryTab = ref('employee')
const summaryRange = ref<[string, string] | null>(null)
const summaryLoading = ref(false)
const employeeSummaries = ref<EmployeeWageSummary[]>([])
const processSummaries = ref<ProcessWageSummary[]>([])

function openSummary() {
  summaryVisible.value = true
}

async function loadSummaries() {
  if (!summaryRange.value) {
    ElMessage.warning('请先选择汇总期间')
    return
  }
  const [periodStart, periodEnd] = summaryRange.value
  summaryLoading.value = true
  try {
    const [byEmployee, byProcess] = await Promise.all([
      summarizeEmployees({ periodStart, periodEnd }),
      summarizeProcesses({ periodStart, periodEnd }),
    ])
    employeeSummaries.value = byEmployee
    processSummaries.value = byProcess
  } finally {
    summaryLoading.value = false
  }
}

// ---------- 行操作 ----------

async function handleRowAction(key: string, row: WageSettlement) {
  try {
    if (key === 'detail' || key === 'audit') {
      openDrawer(row)
    } else if (key === 'recalculate') {
      await recalculateSettlement(row.id, row.version)
      ElMessage.success('已按原范围重算')
      await refresh()
    } else if (key === 'submit') {
      await submitSettlement(row.id, { version: row.version })
      ElMessage.success('已提交审核')
      await refresh()
    }
  } catch {
    // 失败提示由拦截器统一弹出；乐观锁冲突时刷新最新数据
    await refresh()
  }
}

async function confirmDrawerRecalculate() {
  if (!current.value) return
  try {
    await ElMessageBox.confirm(
      '按原期间范围重新取数计算，当前明细（含人工调整）将被替换，确认？',
      '重新计算',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await recalculateSettlement(current.value.id, current.value.version)
    ElMessage.success('已按原范围重算')
  } catch {
    // 提示由拦截器弹出
  }
  await reloadCurrent()
  await loadDetails()
  await loadAuditLogs()
  await refresh()
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="工资结算"
      description="按期间取已审核报工计算计件工资：草稿 → 待审核 → 已审核/已驳回；草稿可调整明细金额，驳回可重算再提交"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="240"
      show-index
      @query="handleQuery"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton
          :roles="WAGE_MANAGE_ROLES"
          type="primary"
          @click="calculateDialog.open()"
        >
          计算结算
        </PermissionButton>
        <el-button @click="openSummary">工资汇总</el-button>
      </template>
    </FilterTable>

    <!-- 计算结算 -->
    <FormDialog
      v-model:visible="calculateDialog.visible.value"
      :title="calculateDialog.title.value"
      :model="calculateDialog.model.value"
      :rules="calculateRules"
      :submit-loading="calculateDialog.submitLoading.value"
      width="560px"
      @submit="calculateDialog.handleSubmit"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="取期间内已审核报工快照，按作业日期匹配单价规则逐条计薪；无匹配规则的记录会计算失败"
        class="dialog-tip"
      />
      <el-form-item label="结算期间" prop="periodRange">
        <el-date-picker
          v-model="calculateDialog.model.value.periodRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="员工范围" prop="employeeIds">
        <el-select
          v-model="calculateDialog.model.value.employeeIds"
          multiple
          filterable
          clearable
          placeholder="不选则包含期间内全部有记录的员工"
        >
          <el-option
            v-for="opt in employeeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="reason">
        <el-input
          v-model="calculateDialog.model.value.reason"
          maxlength="255"
          placeholder="选填，记入审计日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 结算明细抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="`结算单：${current?.settlementNo ?? ''}`"
      size="820px"
      destroy-on-close
    >
      <div v-loading="currentLoading">
        <el-descriptions :column="3" border size="small" class="drawer-desc">
          <el-descriptions-item label="结算期间" :span="2">
            {{ current?.periodStart }} ~ {{ current?.periodEnd }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="current?.settlementStatus ?? 0"
              :status-map="SETTLEMENT_STATUS_MAP"
            />
          </el-descriptions-item>
          <el-descriptions-item label="合格数">
            {{ formatNumber(current?.totalQualifiedQuantity) }}
          </el-descriptions-item>
          <el-descriptions-item label="不良数">
            {{ formatNumber(current?.totalDefectQuantity) }}
          </el-descriptions-item>
          <el-descriptions-item label="总金额（元）">
            {{ current?.totalAmount?.toFixed(2) ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ current?.submitTime ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核时间">
            {{ current?.auditTime ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见">
            {{ current?.auditReason ?? '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="drawerTab">
          <el-tab-pane label="结算明细" name="details">
            <el-alert
              v-if="canAdjust"
              type="info"
              :closable="false"
              show-icon
              title="草稿态可对单条明细做人工调整，调整额与原因记入审计日志"
              class="drawer-tip"
            />
            <el-table v-loading="detailLoading" :data="details" border size="small">
              <el-table-column label="员工" min-width="120">
                <template #default="{ row }">
                  {{ optionLabel(employeeOptions, row.employeeId) }}
                </template>
              </el-table-column>
              <el-table-column prop="workDate" label="作业日期" width="100" />
              <el-table-column label="工序" min-width="120">
                <template #default="{ row }">
                  {{ optionLabel(processOptions, row.processId) }}
                </template>
              </el-table-column>
              <el-table-column prop="qualifiedQuantity" label="合格" width="70" align="right" />
              <el-table-column prop="defectQuantity" label="不良" width="70" align="right" />
              <el-table-column label="单价" width="90" align="right">
                <template #default="{ row }">{{ row.unitPrice.toFixed(4) }}</template>
              </el-table-column>
              <el-table-column label="计算额" width="90" align="right">
                <template #default="{ row }">{{ row.calculatedAmount.toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="最终额" width="90" align="right">
                <template #default="{ row }">
                  <span :class="{ 'amount-adjusted': row.adjustedAmount !== null }">
                    {{ row.finalAmount.toFixed(2) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column v-if="canAdjust" label="操作" width="70" fixed="right">
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click="openAdjust(row as WageSettlementDetail)"
                  >
                    调整
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-if="detailPagination.total > detailPagination.pageSize"
              :current-page="detailPagination.pageNo"
              :page-size="detailPagination.pageSize"
              :total="detailPagination.total"
              layout="prev, pager, next"
              class="drawer-pagination"
              @current-change="onDetailPageChange"
            />
          </el-tab-pane>

          <el-tab-pane label="审计日志" name="logs">
            <el-table v-loading="auditLogLoading" :data="auditLogs" border size="small">
              <el-table-column prop="operateTime" label="时间" width="170" />
              <el-table-column label="动作" width="90">
                <template #default="{ row }">
                  {{ SETTLEMENT_ACTION_TEXT[row.actionType] ?? row.actionType }}
                </template>
              </el-table-column>
              <el-table-column label="状态流转" width="140">
                <template #default="{ row }">
                  <template v-if="row.fromStatus !== null && row.toStatus !== null">
                    {{ SETTLEMENT_STATUS_MAP[row.fromStatus]?.text ?? row.fromStatus }}
                    →
                    {{ SETTLEMENT_STATUS_MAP[row.toStatus]?.text ?? row.toStatus }}
                  </template>
                  <template v-else>-</template>
                </template>
              </el-table-column>
              <el-table-column label="金额变化" width="140" align="right">
                <template #default="{ row }">
                  <template v-if="row.beforeAmount !== null && row.afterAmount !== null">
                    {{ row.beforeAmount.toFixed(2) }} → {{ row.afterAmount.toFixed(2) }}
                  </template>
                  <template v-else>-</template>
                </template>
              </el-table-column>
              <el-table-column prop="actionReason" label="原因" min-width="130">
                <template #default="{ row }">{{ row.actionReason ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="operateBy" label="操作人" width="80" />
            </el-table>
            <el-pagination
              v-if="auditLogPagination.total > auditLogPagination.pageSize"
              :current-page="auditLogPagination.pageNo"
              :page-size="auditLogPagination.pageSize"
              :total="auditLogPagination.total"
              layout="prev, pager, next"
              class="drawer-pagination"
              @current-change="onAuditLogPageChange"
            />
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <!-- 待审核：驳回/通过；草稿或已驳回：重算入口 -->
        <ApprovalActionBar
          v-if="current?.settlementStatus === SETTLEMENT_STATUS.PENDING"
          :roles="WAGE_MANAGE_ROLES"
          :loading="actionLoading"
          approve-comment-enabled
          @approve="handleApprove"
          @reject="handleReject"
        />
        <PermissionButton
          v-else-if="
            current?.settlementStatus === SETTLEMENT_STATUS.DRAFT ||
            current?.settlementStatus === SETTLEMENT_STATUS.REJECTED
          "
          :roles="WAGE_MANAGE_ROLES"
          type="warning"
          plain
          @click="confirmDrawerRecalculate"
        >
          重新计算
        </PermissionButton>
      </template>
    </el-drawer>

    <!-- 明细金额调整 -->
    <FormDialog
      v-model:visible="adjustDialog.visible.value"
      :title="adjustDialog.title.value"
      :model="adjustDialog.model.value"
      :rules="adjustRules"
      :submit-loading="adjustDialog.submitLoading.value"
      width="480px"
      @submit="adjustDialog.handleSubmit"
    >
      <el-form-item label="明细">
        <span>{{ adjustDialog.model.value.employeeText }}</span>
      </el-form-item>
      <el-form-item label="系统计算额">
        <span>{{ adjustDialog.model.value.calculatedAmount.toFixed(2) }} 元</span>
      </el-form-item>
      <el-form-item label="调整后金额" prop="adjustedAmount">
        <el-input-number
          v-model="adjustDialog.model.value.adjustedAmount"
          :min="0"
          :precision="2"
          :step="1"
          controls-position="right"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="调整原因" prop="reason">
        <el-input
          v-model="adjustDialog.model.value.reason"
          type="textarea"
          :rows="3"
          maxlength="255"
          placeholder="必填，记入审计日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 工资汇总 -->
    <el-drawer v-model="summaryVisible" title="已审核工资汇总" size="640px" destroy-on-close>
      <div class="summary-query">
        <el-date-picker
          v-model="summaryRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-button type="primary" :loading="summaryLoading" @click="loadSummaries">
          查询
        </el-button>
      </div>
      <el-tabs v-model="summaryTab">
        <el-tab-pane label="按员工" name="employee">
          <el-table v-loading="summaryLoading" :data="employeeSummaries" border size="small">
            <el-table-column prop="employeeNo" label="工号" width="110" />
            <el-table-column prop="employeeName" label="姓名" min-width="110" />
            <el-table-column label="合格数" width="100" align="right">
              <template #default="{ row }">{{ formatNumber(row.qualifiedQuantity) }}</template>
            </el-table-column>
            <el-table-column label="不良数" width="100" align="right">
              <template #default="{ row }">{{ formatNumber(row.defectQuantity) }}</template>
            </el-table-column>
            <el-table-column label="工资（元）" width="120" align="right">
              <template #default="{ row }">{{ row.totalAmount.toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="按工序" name="process">
          <el-table v-loading="summaryLoading" :data="processSummaries" border size="small">
            <el-table-column prop="processCode" label="工序编码" width="120" />
            <el-table-column prop="processName" label="工序名称" min-width="120" />
            <el-table-column label="合格数" width="100" align="right">
              <template #default="{ row }">{{ formatNumber(row.qualifiedQuantity) }}</template>
            </el-table-column>
            <el-table-column label="不良数" width="100" align="right">
              <template #default="{ row }">{{ formatNumber(row.defectQuantity) }}</template>
            </el-table-column>
            <el-table-column label="工资（元）" width="120" align="right">
              <template #default="{ row }">{{ row.totalAmount.toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}

.dialog-tip {
  margin-bottom: 16px;
}

.drawer-desc {
  margin-bottom: 16px;
}

.drawer-tip {
  margin-bottom: 12px;
}

.drawer-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

.amount-adjusted {
  font-weight: 600;
  color: var(--el-color-warning);
}

.summary-query {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
