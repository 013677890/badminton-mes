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
  EQUIPMENT_MAINTENANCE_RECORD_STATUS_MAP,
  EQUIPMENT_MAINTENANCE_RECORD_STATUS_OPTIONS,
  EQUIPMENT_MAINTENANCE_RESULT_MAP,
  EQUIPMENT_MAINTENANCE_RESULT_OPTIONS,
  MAINTENANCE_RECORD_DELETE_ROLES,
  MAINTENANCE_RECORD_READ_WRITE_ROLES,
} from '@/constants/equipment'
import {
  createEquipmentMaintenanceRecord,
  deleteEquipmentMaintenanceRecord,
  getEquipmentMaintenanceRecordPage,
  updateEquipmentMaintenanceRecord,
} from '@/api/equipment/maintenanceRecord'
import type {
  EquipmentMaintenanceRecord,
  EquipmentMaintenanceRecordPageParams,
  EquipmentMaintenanceRecordSaveParams,
  EquipmentMaintenanceRecordStatus,
  EquipmentMaintenanceResult,
} from '@/api/equipment/maintenanceRecord'
import {
  loadEquipmentLedgerOptions,
  loadEquipmentMaintenancePlanOptions,
} from '@/api/equipment/options'

defineOptions({ name: 'EquipmentMaintenanceRecordList' })

const planOptions = ref<OptionItem[]>([])
const equipmentOptions = ref<OptionItem[]>([])
onMounted(async () => {
  const [plans, equipments] = await Promise.all([
    loadEquipmentMaintenancePlanOptions(),
    loadEquipmentLedgerOptions(),
  ])
  planOptions.value = plans
  equipmentOptions.value = equipments
})

const filterFields = computed<FilterField[]>(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '记录编号' },
  { prop: 'planId', label: '保养计划', type: 'select', options: planOptions.value },
  { prop: 'equipmentId', label: '设备', type: 'select', options: equipmentOptions.value },
  { prop: 'recordStatus', label: '记录状态', type: 'select', options: EQUIPMENT_MAINTENANCE_RECORD_STATUS_OPTIONS },
  { prop: 'maintenanceResult', label: '保养结果', type: 'select', options: EQUIPMENT_MAINTENANCE_RESULT_OPTIONS },
  { prop: 'scheduledStartTime', label: '计划起', type: 'date' },
  { prop: 'scheduledEndTime', label: '计划止', type: 'date' },
])

const columns: ColumnDef<EquipmentMaintenanceRecord>[] = [
  { prop: 'recordNo', label: '记录编号', width: 160 },
  {
    prop: 'planCode',
    label: '保养计划',
    minWidth: 140,
    formatter: (row) =>
      planOptions.value.find((o) => o.value === row.planId)?.label ?? String(row.planId),
  },
  { prop: 'equipmentName', label: '设备', minWidth: 140 },
  { prop: 'scheduledTime', label: '计划保养时间', width: 170 },
  { prop: 'startTime', label: '开始时间', width: 170 },
  { prop: 'finishTime', label: '完成时间', width: 170 },
  {
    prop: 'maintenanceResult',
    label: '保养结果',
    width: 100,
    statusMap: EQUIPMENT_MAINTENANCE_RESULT_MAP,
  },
  {
    prop: 'recordStatus',
    label: '记录状态',
    width: 100,
    statusMap: EQUIPMENT_MAINTENANCE_RECORD_STATUS_MAP,
  },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<EquipmentMaintenanceRecord>[] = [
  { key: 'edit', label: '编辑', roles: MAINTENANCE_RECORD_READ_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: MAINTENANCE_RECORD_DELETE_ROLES,
    confirm: '确认删除该保养记录？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  EquipmentMaintenanceRecord,
  Omit<EquipmentMaintenanceRecordPageParams, 'pageNo' | 'pageSize'>
>({ fetcher: getEquipmentMaintenanceRecordPage })

interface EquipmentMaintenanceRecordForm {
  id?: number
  planId: number | null
  scheduledTime: string
  startTime: string | null
  finishTime: string | null
  executorUserId: number | null
  maintenanceContent: string
  maintenanceResult: EquipmentMaintenanceResult | null
  recordStatus: EquipmentMaintenanceRecordStatus | null
  abnormalDescription: string | null
  remark: string | null
}

const dialog = useFormDialog<EquipmentMaintenanceRecordForm>(
  () => ({
    planId: null,
    scheduledTime: '',
    startTime: null,
    finishTime: null,
    executorUserId: null,
    maintenanceContent: '',
    maintenanceResult: null,
    recordStatus: 'PENDING',
    abnormalDescription: null,
    remark: null,
  }),
  {
    titles: { create: '新增保养记录', edit: '编辑保养记录' },
    submit: async (model, mode) => {
      const payload: EquipmentMaintenanceRecordSaveParams = {
        planId: model.planId!,
        scheduledTime: model.scheduledTime,
        startTime: model.startTime,
        finishTime: model.finishTime,
        executorUserId: model.executorUserId,
        maintenanceContent: model.maintenanceContent,
        maintenanceResult: model.maintenanceResult,
        recordStatus: model.recordStatus,
        abnormalDescription: model.abnormalDescription,
        remark: model.remark,
      }
      if (mode === 'create') {
        await createEquipmentMaintenanceRecord(payload)
        ElMessage.success('保养记录已创建')
      } else {
        await updateEquipmentMaintenanceRecord(model.id!, payload)
        ElMessage.success('保养记录已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  planId: [{ required: true, message: '请选择保养计划', trigger: 'change' }],
  scheduledTime: [{ required: true, message: '请选择计划保养时间', trigger: 'change' }],
  maintenanceContent: [{ required: true, message: '请输入保养内容', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: EquipmentMaintenanceRecord) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row })
    } else if (key === 'delete') {
      await deleteEquipmentMaintenanceRecord(row.id)
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
    <PageHeader title="保养记录" description="设备保养执行记录，跟踪保养完成情况与异常" />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="140"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton
          :roles="MAINTENANCE_RECORD_READ_WRITE_ROLES"
          type="primary"
          @click="dialog.open()"
        >
          新增记录
        </PermissionButton>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      @submit="dialog.handleSubmit"
    >
      <el-form-item label="保养计划" prop="planId">
        <el-select v-model="dialog.model.value.planId" filterable placeholder="请选择">
          <el-option
            v-for="opt in planOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="计划保养" prop="scheduledTime">
        <el-date-picker
          v-model="dialog.model.value.scheduledTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择时间"
        />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          v-model="dialog.model.value.startTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="完成时间" prop="finishTime">
        <el-date-picker
          v-model="dialog.model.value.finishTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="执行人ID" prop="executorUserId">
        <el-input-number
          v-model="dialog.model.value.executorUserId"
          :min="1"
          controls-position="right"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="保养内容" prop="maintenanceContent">
        <el-input
          v-model="dialog.model.value.maintenanceContent"
          type="textarea"
          :rows="3"
          maxlength="500"
        />
      </el-form-item>
      <el-form-item label="保养结果" prop="maintenanceResult">
        <el-select v-model="dialog.model.value.maintenanceResult" clearable placeholder="选填">
          <el-option
            v-for="opt in EQUIPMENT_MAINTENANCE_RESULT_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="记录状态" prop="recordStatus">
        <el-select v-model="dialog.model.value.recordStatus">
          <el-option
            v-for="opt in EQUIPMENT_MAINTENANCE_RECORD_STATUS_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="异常描述" prop="abnormalDescription">
        <el-input
          v-model="dialog.model.value.abnormalDescription"
          type="textarea"
          :rows="2"
          maxlength="500"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="dialog.model.value.remark" type="textarea" :rows="2" maxlength="255" />
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
