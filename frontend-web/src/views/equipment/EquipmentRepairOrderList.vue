<script setup lang="ts">
// 维修工单页按设备和状态跟踪报修、处理和关闭流程，状态转换由后端 CAS 保证。
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { ROLES } from '@/constants/production'
import {
  EQUIPMENT_REPAIR_STATUS_MAP,
  EQUIPMENT_REPAIR_STATUS_OPTIONS,
} from '@/constants/equipment'
import {
  createEquipmentRepairOrder,
  deleteEquipmentRepairOrder,
  getEquipmentRepairOrderPage,
  updateEquipmentRepairOrder,
} from '@/api/equipment/repairOrder'
import type {
  EquipmentRepairOrder,
  EquipmentRepairOrderPageParams,
  EquipmentRepairOrderSaveParams,
  EquipmentRepairStatus,
} from '@/api/equipment/repairOrder'
import {
  loadEquipmentFaultPrincipleOptions,
  loadEquipmentLedgerOptions,
} from '@/api/equipment/options'

defineOptions({ name: 'EquipmentRepairOrderList' })

const REPAIR_WRITE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

const equipmentOptions = ref<OptionItem[]>([])
const faultPrincipleOptions = ref<OptionItem[]>([])
onMounted(async () => {
  const [equipments, principles] = await Promise.all([
    loadEquipmentLedgerOptions(),
    loadEquipmentFaultPrincipleOptions(),
  ])
  equipmentOptions.value = equipments
  faultPrincipleOptions.value = principles
})

const filterFields = computed<FilterField[]>(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '报修单号' },
  { prop: 'equipmentId', label: '设备', type: 'select', options: equipmentOptions.value },
  { prop: 'faultPrincipleId', label: '故障原理', type: 'select', options: faultPrincipleOptions.value },
  { prop: 'repairStatus', label: '维修状态', type: 'select', options: EQUIPMENT_REPAIR_STATUS_OPTIONS },
  { prop: 'reportStartTime', label: '报修开始', type: 'date' },
  { prop: 'reportEndTime', label: '报修结束', type: 'date' },
])

const columns: ColumnDef<EquipmentRepairOrder>[] = [
  { prop: 'repairNo', label: '报修单号', width: 160 },
  {
    prop: 'equipmentName',
    label: '设备',
    minWidth: 140,
    formatter: (row) =>
      equipmentOptions.value.find((o) => o.value === row.equipmentId)?.label ??
      String(row.equipmentId),
  },
  { prop: 'faultDescription', label: '故障描述', minWidth: 180, showOverflowTooltip: true },
  { prop: 'reportTime', label: '报修时间', width: 170 },
  { prop: 'repairStatus', label: '维修状态', width: 100, statusMap: EQUIPMENT_REPAIR_STATUS_MAP },
  { prop: 'repairResult', label: '维修结果', minWidth: 160, showOverflowTooltip: true },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<EquipmentRepairOrder>[] = [
  { key: 'edit', label: '编辑', roles: REPAIR_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: REPAIR_WRITE_ROLES,
    confirm: '确认删除该报修任务？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  EquipmentRepairOrder,
  Omit<EquipmentRepairOrderPageParams, 'pageNo' | 'pageSize'>
>({ fetcher: getEquipmentRepairOrderPage })

interface EquipmentRepairOrderForm {
  id?: number
  repairNo: string | null
  equipmentId: number | null
  faultPrincipleId: number | null
  faultDescription: string
  reportTime: string | null
  repairUserId: number | null
  repairStartTime: string | null
  repairEndTime: string | null
  repairResult: string | null
  repairStatus: EquipmentRepairStatus | null
  remark: string | null
}

const dialog = useFormDialog<EquipmentRepairOrderForm>(
  () => ({
    repairNo: null,
    equipmentId: null,
    faultPrincipleId: null,
    faultDescription: '',
    reportTime: null,
    repairUserId: null,
    repairStartTime: null,
    repairEndTime: null,
    repairResult: null,
    repairStatus: 'REPORTED',
    remark: null,
  }),
  {
    titles: { create: '新增报修任务', edit: '编辑报修任务' },
    submit: async (model, mode) => {
      const payload: EquipmentRepairOrderSaveParams = {
        equipmentId: model.equipmentId!,
        faultPrincipleId: model.faultPrincipleId,
        faultDescription: model.faultDescription,
        reportTime: model.reportTime,
        repairUserId: model.repairUserId,
        repairStartTime: model.repairStartTime,
        repairEndTime: model.repairEndTime,
        repairResult: model.repairResult,
        repairStatus: model.repairStatus,
        remark: model.remark,
      }
      if (mode === 'create') {
        await createEquipmentRepairOrder(payload)
        ElMessage.success('报修任务已创建')
      } else {
        await updateEquipmentRepairOrder(model.id!, payload)
        ElMessage.success('报修任务已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  equipmentId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  faultDescription: [{ required: true, message: '请输入故障描述', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: EquipmentRepairOrder) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row })
    } else if (key === 'delete') {
      await deleteEquipmentRepairOrder(row.id)
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
    <PageHeader title="报修任务" description="设备故障报修工单，跟踪维修全流程" />
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
        <PermissionButton :roles="REPAIR_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增报修
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
      <el-form-item label="报修单号" prop="repairNo">
        <el-input v-model="dialog.model.value.repairNo" disabled placeholder="后端自动生成" />
      </el-form-item>
      <el-form-item label="设备" prop="equipmentId">
        <el-select v-model="dialog.model.value.equipmentId" filterable placeholder="请选择">
          <el-option
            v-for="opt in equipmentOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="故障原理" prop="faultPrincipleId">
        <el-select
          v-model="dialog.model.value.faultPrincipleId"
          clearable
          filterable
          placeholder="选填"
        >
          <el-option
            v-for="opt in faultPrincipleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="故障描述" prop="faultDescription">
        <el-input
          v-model="dialog.model.value.faultDescription"
          type="textarea"
          :rows="3"
          maxlength="500"
        />
      </el-form-item>
      <el-form-item label="报修时间" prop="reportTime">
        <el-date-picker
          v-model="dialog.model.value.reportTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择时间"
        />
      </el-form-item>
      <el-form-item label="维修人ID" prop="repairUserId">
        <el-input-number
          v-model="dialog.model.value.repairUserId"
          :min="1"
          controls-position="right"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="维修开始" prop="repairStartTime">
        <el-date-picker
          v-model="dialog.model.value.repairStartTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="维修结束" prop="repairEndTime">
        <el-date-picker
          v-model="dialog.model.value.repairEndTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="维修结果" prop="repairResult">
        <el-input
          v-model="dialog.model.value.repairResult"
          type="textarea"
          :rows="2"
          maxlength="500"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="维修状态" prop="repairStatus">
        <el-select v-model="dialog.model.value.repairStatus">
          <el-option
            v-for="opt in EQUIPMENT_REPAIR_STATUS_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
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
