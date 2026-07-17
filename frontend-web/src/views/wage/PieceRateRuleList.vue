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
import { ENABLE_STATUS_MAP, ENABLE_STATUS_OPTIONS } from '@/constants/production'
import { WAGE_MANAGE_ROLES, WAGE_RULE_CHANGE_TYPE_TEXT } from '@/constants/wage'
import { loadProcessOptions, loadProductOptions } from '@/api/production/options'
import {
  createRule,
  deleteRule,
  getRuleChangeLogPage,
  getRulePage,
  updateRule,
  updateRuleStatus,
} from '@/api/wage/rule'
import type { PieceRateRule, PieceRateRulePageParams, WageRuleChangeLog } from '@/api/wage/rule'

defineOptions({ name: 'PieceRateRuleList' })

// ---------- 下拉选项 ----------

const processOptions = ref<OptionItem[]>([])
const productOptions = ref<OptionItem[]>([])

const filterFields = ref<FilterField[]>([
  { prop: 'processId', label: '工序', type: 'select', options: [] },
  { prop: 'productId', label: '产品', type: 'select', options: [] },
  { prop: 'status', label: '状态', type: 'select', options: ENABLE_STATUS_OPTIONS },
  {
    prop: 'effectiveDate',
    label: '生效日期',
    type: 'date',
    placeholder: '查询该日期生效的规则',
  },
])

onMounted(async () => {
  try {
    const [processes, products] = await Promise.all([loadProcessOptions(), loadProductOptions()])
    processOptions.value = processes
    productOptions.value = products
    const processField = filterFields.value.find((item) => item.prop === 'processId')
    if (processField) processField.options = processes
    const productField = filterFields.value.find((item) => item.prop === 'productId')
    if (productField) productField.options = products
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

function optionLabel(options: OptionItem[], value: number | null): string {
  if (value === null || value === undefined) return '-'
  return String(options.find((opt) => opt.value === value)?.label ?? value)
}

// ---------- 列表 ----------

const columns: ColumnDef<PieceRateRule>[] = [
  {
    prop: 'processId',
    label: '工序',
    minWidth: 150,
    formatter: (row) => optionLabel(processOptions.value, row.processId),
  },
  {
    prop: 'productId',
    label: '产品',
    minWidth: 150,
    formatter: (row) =>
      row.productId === null ? '通用（全部产品）' : optionLabel(productOptions.value, row.productId),
  },
  {
    prop: 'unitPrice',
    label: '单价（元）',
    width: 110,
    align: 'right',
    formatter: (row) => row.unitPrice.toFixed(4),
  },
  {
    prop: 'defectDeductionRate',
    label: '不良扣减率',
    width: 100,
    align: 'right',
    formatter: (row) => `${row.defectDeductionRate}%`,
  },
  { prop: 'effectiveStart', label: '生效起', width: 110 },
  {
    prop: 'effectiveEnd',
    label: '生效止',
    width: 110,
    formatter: (row) => row.effectiveEnd ?? '长期',
  },
  { prop: 'status', label: '状态', width: 80, statusMap: ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<PieceRateRule>[] = [
  { key: 'edit', label: '编辑', roles: WAGE_MANAGE_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: WAGE_MANAGE_ROLES,
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: WAGE_MANAGE_ROLES,
    show: (row) => row.status === 1,
  },
  { key: 'log', label: '日志' },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: WAGE_MANAGE_ROLES,
    confirm: '已被结算引用的规则无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  PieceRateRule,
  PieceRateRulePageParams
>({ fetcher: getRulePage })

// ---------- 新增 / 编辑 ----------

interface RuleForm {
  id?: number
  version?: number
  processId: number | null
  productId: number | null
  unitPrice: number
  defectDeductionRate: number
  /** [start, end]；end 可空表示长期 */
  effectiveStart: string
  effectiveEnd: string | null
  status: number
  changeReason: string
}

const dialog = useFormDialog<RuleForm>(
  () => ({
    processId: null,
    productId: null,
    unitPrice: 0.01,
    defectDeductionRate: 0,
    effectiveStart: '',
    effectiveEnd: null,
    status: 1,
    changeReason: '',
  }),
  {
    titles: { create: '新增计件规则', edit: '编辑计件规则' },
    submit: async (model, mode) => {
      const payload = {
        processId: model.processId!,
        productId: model.productId ?? undefined,
        unitPrice: model.unitPrice,
        defectDeductionRate: model.defectDeductionRate,
        effectiveStart: model.effectiveStart,
        effectiveEnd: model.effectiveEnd ?? undefined,
        status: model.status,
        changeReason: model.changeReason || undefined,
      }
      if (mode === 'create') {
        await createRule(payload)
        ElMessage.success('规则已创建')
      } else {
        await updateRule(model.id!, { ...payload, version: model.version! })
        ElMessage.success('规则已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  processId: [{ required: true, message: '请选择工序', trigger: 'change' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  effectiveStart: [{ required: true, message: '请选择生效开始日期', trigger: 'change' }],
  effectiveEnd: [
    {
      validator: (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
        const { effectiveStart, effectiveEnd } = dialog.model.value
        if (effectiveStart && effectiveEnd && effectiveEnd < effectiveStart) {
          callback(new Error('结束日期不能早于开始日期'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

// ---------- 启停（需原因） ----------

interface StatusForm {
  id?: number
  version?: number
  status: number
  reason: string
}

const statusDialog = useFormDialog<StatusForm>(
  () => ({ status: 1, reason: '' }),
  {
    submit: async (model) => {
      await updateRuleStatus(model.id!, {
        version: model.version!,
        status: model.status,
        reason: model.reason,
      })
      ElMessage.success(model.status === 1 ? '规则已启用' : '规则已停用')
    },
    onSuccess: refresh,
  },
)

const statusDialogTitle = computed(() =>
  statusDialog.model.value.status === 1 ? '启用规则' : '停用规则',
)

const statusRules = {
  reason: [{ required: true, message: '请填写启停原因', trigger: 'blur' }],
}

// ---------- 变更日志抽屉 ----------

const logDrawerVisible = ref(false)
const logRule = ref<PieceRateRule>()
const logs = ref<WageRuleChangeLog[]>([])
const logLoading = ref(false)
const logPagination = ref({ pageNo: 1, pageSize: 10, total: 0 })

function openLogDrawer(row: PieceRateRule) {
  logRule.value = row
  logs.value = []
  logPagination.value = { pageNo: 1, pageSize: 10, total: 0 }
  logDrawerVisible.value = true
  void loadLogs()
}

async function loadLogs() {
  if (!logRule.value) return
  logLoading.value = true
  try {
    const page = await getRuleChangeLogPage(logRule.value.id, {
      pageNo: logPagination.value.pageNo,
      pageSize: logPagination.value.pageSize,
    })
    logs.value = page.list
    logPagination.value.total = page.total
  } finally {
    logLoading.value = false
  }
}

function onLogPageChange(pageNo: number) {
  logPagination.value.pageNo = pageNo
  void loadLogs()
}

/** 快照为 JSON 字符串，展开行内格式化展示 */
function formatSnapshot(snapshot: string | null): string {
  if (!snapshot) return '-'
  try {
    return JSON.stringify(JSON.parse(snapshot), null, 2)
  } catch {
    return snapshot
  }
}

// ---------- 行操作 ----------

async function handleRowAction(key: string, row: PieceRateRule) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row, changeReason: '' })
    } else if (key === 'enable' || key === 'disable') {
      statusDialog.open('edit', {
        id: row.id,
        version: row.version,
        status: key === 'enable' ? 1 : 0,
        reason: '',
      })
    } else if (key === 'log') {
      openLogDrawer(row)
    } else if (key === 'delete') {
      await deleteRule(row.id, row.version)
      ElMessage.success('已删除')
      await refresh()
    }
  } catch {
    // 失败提示由拦截器统一弹出；乐观锁冲突时刷新最新数据
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="计件单价规则"
      description="工序（可细化到产品）的计件单价与不良扣减率；同工序同产品的生效期间不可重叠，结算按作业日期匹配规则"
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
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="WAGE_MANAGE_ROLES" type="primary" @click="dialog.open()">
          新增规则
        </PermissionButton>
      </template>
    </FilterTable>

    <!-- 新增/编辑 -->
    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      label-width="110px"
      @submit="dialog.handleSubmit"
    >
      <el-form-item label="工序" prop="processId">
        <el-select v-model="dialog.model.value.processId" filterable placeholder="请选择工序">
          <el-option
            v-for="opt in processOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产品" prop="productId">
        <el-select
          v-model="dialog.model.value.productId"
          filterable
          clearable
          placeholder="不选则为工序通用规则"
        >
          <el-option
            v-for="opt in productOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <span class="form-tip">产品专用规则优先于通用规则</span>
      </el-form-item>
      <el-form-item label="单价（元）" prop="unitPrice">
        <el-input-number
          v-model="dialog.model.value.unitPrice"
          :min="0.0001"
          :precision="4"
          :step="0.01"
          controls-position="right"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="不良扣减率" prop="defectDeductionRate">
        <el-input-number
          v-model="dialog.model.value.defectDeductionRate"
          :min="0"
          :max="100"
          :precision="2"
          :step="1"
          controls-position="right"
          class="full-width"
        />
        <span class="form-tip">百分比：不良数 × 单价 × 扣减率 从工资中扣除</span>
      </el-form-item>
      <el-form-item label="生效开始" prop="effectiveStart">
        <el-date-picker
          v-model="dialog.model.value.effectiveStart"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="生效结束" prop="effectiveEnd">
        <el-date-picker
          v-model="dialog.model.value.effectiveEnd"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="不选则长期有效"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="dialog.model.value.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="dialog.mode.value === 'edit'" label="变更原因" prop="changeReason">
        <el-input
          v-model="dialog.model.value.changeReason"
          maxlength="255"
          placeholder="选填，记入变更日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 启停（需原因） -->
    <FormDialog
      v-model:visible="statusDialog.visible.value"
      :title="statusDialogTitle"
      :model="statusDialog.model.value"
      :rules="statusRules"
      :submit-loading="statusDialog.submitLoading.value"
      width="480px"
      @submit="statusDialog.handleSubmit"
    >
      <el-alert
        v-if="statusDialog.model.value.status === 0"
        type="warning"
        :closable="false"
        show-icon
        title="停用后结算计算不再匹配该规则"
        class="dialog-tip"
      />
      <el-form-item label="原因" prop="reason">
        <el-input
          v-model="statusDialog.model.value.reason"
          type="textarea"
          :rows="3"
          maxlength="255"
          placeholder="必填，记入变更日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 变更日志 -->
    <el-drawer v-model="logDrawerVisible" title="规则变更日志" size="640px" destroy-on-close>
      <el-table v-loading="logLoading" :data="logs" border size="small">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="log-snapshots">
              <div class="log-snapshot">
                <div class="log-snapshot__title">变更前</div>
                <pre>{{ formatSnapshot(row.beforeSnapshot) }}</pre>
              </div>
              <div class="log-snapshot">
                <div class="log-snapshot__title">变更后</div>
                <pre>{{ formatSnapshot(row.afterSnapshot) }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="operateTime" label="时间" width="170" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            {{ WAGE_RULE_CHANGE_TYPE_TEXT[row.changeType] ?? row.changeType }}
          </template>
        </el-table-column>
        <el-table-column prop="changeReason" label="原因" min-width="140">
          <template #default="{ row }">{{ row.changeReason ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="operateBy" label="操作人" width="90" />
      </el-table>
      <el-pagination
        v-if="logPagination.total > logPagination.pageSize"
        :current-page="logPagination.pageNo"
        :page-size="logPagination.pageSize"
        :total="logPagination.total"
        layout="prev, pager, next"
        class="log-pagination"
        @current-change="onLogPageChange"
      />
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

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.dialog-tip {
  margin-bottom: 16px;
}

.log-snapshots {
  display: flex;
  gap: 16px;
  padding: 8px 16px;
}

.log-snapshot {
  flex: 1;
  min-width: 0;
}

.log-snapshot__title {
  margin-bottom: 4px;
  font-weight: 600;
}

.log-snapshot pre {
  margin: 0;
  padding: 8px;
  overflow: auto;
  font-size: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.log-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
