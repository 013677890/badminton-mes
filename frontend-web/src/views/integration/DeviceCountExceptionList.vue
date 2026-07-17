<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { formatSnapshot, parseSnapshot } from '@/utils/format'
import { statusMapToOptions } from '@/constants/production'
import {
  DEVICE_EXCEPTION_HANDLE_STATUS,
  DEVICE_EXCEPTION_HANDLE_STATUS_MAP,
  EXCEPTION_TYPE_OPTIONS,
  EXCEPTION_TYPE_TEXT,
  INTEGRATION_MANAGE_ROLES,
} from '@/constants/integration'
import {
  getDeviceCountExceptionPage,
  ignoreDeviceCountException,
  retryDeviceCountException,
} from '@/api/integration/deviceException'
import type {
  DeviceCountException,
  DeviceCountExceptionPageParams,
  DeviceCountWriteReq,
  IntegrationWriteResult,
} from '@/api/integration/deviceException'

defineOptions({ name: 'DeviceCountExceptionList' })

// ---------- 列表 ----------

const filterFields: FilterField[] = [
  { prop: 'sourceSystem', label: '来源系统', type: 'input' },
  { prop: 'equipmentCode', label: '设备编码', type: 'input' },
  {
    prop: 'exceptionType',
    label: '异常类型',
    type: 'select',
    options: EXCEPTION_TYPE_OPTIONS,
  },
  {
    prop: 'handleStatus',
    label: '处理状态',
    type: 'select',
    options: statusMapToOptions(DEVICE_EXCEPTION_HANDLE_STATUS_MAP),
  },
  { prop: 'timeRange', label: '采集时间', type: 'dateRange', span: 8 },
]

const columns: ColumnDef<DeviceCountException>[] = [
  { prop: 'sourceSystem', label: '来源系统', width: 110 },
  { prop: 'externalKey', label: '幂等键', minWidth: 140, showOverflowTooltip: true },
  { prop: 'equipmentCode', label: '设备编码', width: 120 },
  { prop: 'dispatchNo', label: '派工单号', width: 130 },
  { prop: 'processCode', label: '工序编码', width: 120 },
  { prop: 'collectTime', label: '采集时间', width: 170 },
  { prop: 'countValue', label: '计数值', width: 90, align: 'right' },
  {
    prop: 'exceptionType',
    label: '异常类型',
    width: 160,
    formatter: (row) => EXCEPTION_TYPE_TEXT[row.exceptionType] ?? row.exceptionType,
  },
  {
    prop: 'handleStatus',
    label: '处理状态',
    width: 100,
    statusMap: DEVICE_EXCEPTION_HANDLE_STATUS_MAP,
  },
  {
    prop: 'handleTime',
    label: '处理时间',
    width: 170,
    formatter: (row) => row.handleTime ?? '-',
  },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const rowActions: RowAction<DeviceCountException>[] = [
  { key: 'detail', label: '详情' },
  {
    key: 'retry',
    label: '重试',
    type: 'primary',
    roles: INTEGRATION_MANAGE_ROLES,
    show: (row) => row.handleStatus === DEVICE_EXCEPTION_HANDLE_STATUS.PENDING,
  },
  {
    key: 'ignore',
    label: '忽略',
    type: 'warning',
    roles: INTEGRATION_MANAGE_ROLES,
    show: (row) => row.handleStatus === DEVICE_EXCEPTION_HANDLE_STATUS.PENDING,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  DeviceCountException,
  DeviceCountExceptionPageParams
>({ fetcher: getDeviceCountExceptionPage })

/** dateRange 拆为后端 startTime/endTime（yyyy-MM-dd HH:mm:ss） */
function handleQuery(params: Record<string, any>) {
  const { timeRange, ...rest } = params
  if (Array.isArray(timeRange) && timeRange.length === 2) {
    rest.startTime = `${timeRange[0]} 00:00:00`
    rest.endTime = `${timeRange[1]} 23:59:59`
  }
  query(rest as DeviceCountExceptionPageParams)
}

// ---------- 详情抽屉 ----------

const drawerVisible = ref(false)
const current = ref<DeviceCountException>()

function openDetail(row: DeviceCountException) {
  current.value = row
  drawerVisible.value = true
}

// ---------- 忽略（remark 选填） ----------

interface IgnoreForm {
  id?: number
  equipmentText: string
  remark: string
}

const ignoreDialog = useFormDialog<IgnoreForm>(
  () => ({ equipmentText: '', remark: '' }),
  {
    titles: { edit: '忽略异常' },
    submit: async (model) => {
      await ignoreDeviceCountException(model.id!, model.remark || undefined)
      ElMessage.success('已忽略')
    },
    onSuccess: refresh,
  },
)

// ---------- 重试（预填自 requestSnapshot） ----------

interface RetryForm {
  id?: number
  sourceSystem: string
  externalKey: string
  equipmentCode: string
  dispatchNo: string
  processCode: string
  collectTime: string
  countValue: number
}

const retryResult = ref<IntegrationWriteResult>()
const resultVisible = ref(false)

const retryDialog = useFormDialog<RetryForm>(
  () => ({
    sourceSystem: '',
    externalKey: '',
    equipmentCode: '',
    dispatchNo: '',
    processCode: '',
    collectTime: '',
    countValue: 0,
  }),
  {
    titles: { edit: '重试计数写入' },
    submit: async (model) => {
      const payload: DeviceCountWriteReq = {
        sourceSystem: model.sourceSystem,
        externalKey: model.externalKey,
        equipmentCode: model.equipmentCode,
        dispatchNo: model.dispatchNo,
        processCode: model.processCode,
        collectTime: model.collectTime,
        countValue: model.countValue,
      }
      const result = await retryDeviceCountException(model.id!, payload)
      retryResult.value = result
      resultVisible.value = true
    },
    onSuccess: refresh,
  },
)

const retryRules = {
  sourceSystem: [{ required: true, message: '请输入来源系统', trigger: 'blur' }],
  externalKey: [{ required: true, message: '请输入幂等键', trigger: 'blur' }],
  equipmentCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  dispatchNo: [{ required: true, message: '请输入派工单号', trigger: 'blur' }],
  processCode: [{ required: true, message: '请输入工序编码', trigger: 'blur' }],
  collectTime: [{ required: true, message: '请选择采集时间', trigger: 'change' }],
  countValue: [{ required: true, message: '请输入计数值', trigger: 'blur' }],
}

/** 从 requestSnapshot 解析预填重试表单，解析失败降级用异常行字段 */
function openRetry(row: DeviceCountException) {
  const snap = parseSnapshot<DeviceCountWriteReq>(row.requestSnapshot)
  retryDialog.open('edit', {
    id: row.id,
    sourceSystem: snap?.sourceSystem ?? row.sourceSystem,
    externalKey: snap?.externalKey ?? row.externalKey,
    equipmentCode: snap?.equipmentCode ?? row.equipmentCode,
    dispatchNo: snap?.dispatchNo ?? row.dispatchNo,
    processCode: snap?.processCode ?? row.processCode,
    collectTime: snap?.collectTime ?? row.collectTime,
    countValue: snap?.countValue ?? row.countValue,
  })
}

// ---------- 行操作 ----------

async function handleRowAction(key: string, row: DeviceCountException) {
  if (key === 'detail') {
    openDetail(row)
  } else if (key === 'ignore') {
    ignoreDialog.open('edit', {
      id: row.id,
      equipmentText: `${row.equipmentCode} · ${row.dispatchNo}`,
      remark: '',
    })
  } else if (key === 'retry') {
    openRetry(row)
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="设备计数异常池"
      description="设备计数写入匹配失败进入异常池：可忽略或修正后重试，重试表单自原始请求快照预填"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="200"
      show-index
      @query="handleQuery"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    />

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="异常详情"
      size="640px"
      destroy-on-close
    >
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="来源系统">{{ current.sourceSystem }}</el-descriptions-item>
          <el-descriptions-item label="幂等键">{{ current.externalKey }}</el-descriptions-item>
          <el-descriptions-item label="设备编码">{{ current.equipmentCode }}</el-descriptions-item>
          <el-descriptions-item label="派工单号">{{ current.dispatchNo }}</el-descriptions-item>
          <el-descriptions-item label="工序编码">{{ current.processCode }}</el-descriptions-item>
          <el-descriptions-item label="采集时间">{{ current.collectTime }}</el-descriptions-item>
          <el-descriptions-item label="计数值">{{ current.countValue }}</el-descriptions-item>
          <el-descriptions-item label="异常类型">
            {{ EXCEPTION_TYPE_TEXT[current.exceptionType] ?? current.exceptionType }}
          </el-descriptions-item>
          <el-descriptions-item label="错误码">{{ current.errorCode ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息">{{ current.errorMessage ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <StatusTag :status="current.handleStatus" :status-map="DEVICE_EXCEPTION_HANDLE_STATUS_MAP" />
          </el-descriptions-item>
          <el-descriptions-item label="处理人">{{ current.handleBy ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ current.handleTime ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理说明">{{ current.handleRemark ?? '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="snapshot-title">原始请求快照</div>
        <pre class="snapshot-body">{{ formatSnapshot(current.requestSnapshot) }}</pre>
        <template v-if="current.retryRequestSnapshot">
          <div class="snapshot-title">重试请求快照</div>
          <pre class="snapshot-body">{{ formatSnapshot(current.retryRequestSnapshot) }}</pre>
        </template>
      </template>
    </el-drawer>

    <!-- 忽略 -->
    <FormDialog
      v-model:visible="ignoreDialog.visible.value"
      :title="ignoreDialog.title.value"
      :model="ignoreDialog.model.value"
      :submit-loading="ignoreDialog.submitLoading.value"
      width="480px"
      @submit="ignoreDialog.handleSubmit"
    >
      <el-form-item label="异常设备">
        <span>{{ ignoreDialog.model.value.equipmentText }}</span>
      </el-form-item>
      <el-form-item label="处理说明" prop="remark">
        <el-input
          v-model="ignoreDialog.model.value.remark"
          type="textarea"
          :rows="3"
          maxlength="255"
          placeholder="选填"
        />
      </el-form-item>
    </FormDialog>

    <!-- 重试 -->
    <FormDialog
      v-model:visible="retryDialog.visible.value"
      :title="retryDialog.title.value"
      :model="retryDialog.model.value"
      :rules="retryRules"
      :submit-loading="retryDialog.submitLoading.value"
      width="560px"
      @submit="retryDialog.handleSubmit"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="表单已自原始请求快照预填，修正后提交将重新处理计数写入"
        class="dialog-tip"
      />
      <el-form-item label="来源系统" prop="sourceSystem">
        <el-input v-model="retryDialog.model.value.sourceSystem" maxlength="32" />
      </el-form-item>
      <el-form-item label="幂等键" prop="externalKey">
        <el-input v-model="retryDialog.model.value.externalKey" maxlength="64" />
      </el-form-item>
      <el-form-item label="设备编码" prop="equipmentCode">
        <el-input v-model="retryDialog.model.value.equipmentCode" maxlength="32" />
      </el-form-item>
      <el-form-item label="派工单号" prop="dispatchNo">
        <el-input v-model="retryDialog.model.value.dispatchNo" maxlength="32" />
      </el-form-item>
      <el-form-item label="工序编码" prop="processCode">
        <el-input v-model="retryDialog.model.value.processCode" maxlength="32" />
      </el-form-item>
      <el-form-item label="采集时间" prop="collectTime">
        <el-date-picker
          v-model="retryDialog.model.value.collectTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择采集时间"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="计数值" prop="countValue">
        <el-input-number
          v-model="retryDialog.model.value.countValue"
          :min="1"
          controls-position="right"
          class="full-width"
        />
      </el-form-item>
    </FormDialog>

    <!-- 重试结果 -->
    <el-dialog
      v-model="resultVisible"
      title="重试结果"
      width="480px"
      destroy-on-close
      append-to-body
    >
      <template v-if="retryResult">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="处理状态">
            <StatusTag :status="retryResult.status" :status-map="{
              SUCCESS: { type: 'success', text: '成功' },
              FAILED: { type: 'danger', text: '失败' },
              DUPLICATE: { type: 'warning', text: '重复' },
            }" />
          </el-descriptions-item>
          <el-descriptions-item label="业务编号">{{ retryResult.businessNo ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务 ID">{{ retryResult.businessId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误码">{{ retryResult.errorCode ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理说明">{{ retryResult.message ?? '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
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

.snapshot-title {
  margin: 16px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.snapshot-body {
  max-height: 36vh;
  padding: 12px;
  overflow: auto;
  font-family: 'Cascadia Code', Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
