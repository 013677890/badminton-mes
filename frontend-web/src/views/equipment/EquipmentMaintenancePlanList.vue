<script setup lang="ts">
// 保养计划页维护周期和责任配置，计划与设备、类别的有效性由后端统一判断。
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
  EQUIPMENT_ENABLE_STATUS_MAP,
  EQUIPMENT_ENABLE_STATUS_OPTIONS,
  EQUIPMENT_MAINTENANCE_TYPE_MAP,
  EQUIPMENT_MAINTENANCE_TYPE_OPTIONS,
  MAINTENANCE_PLAN_WRITE_ROLES,
} from '@/constants/equipment'
import {
  createEquipmentMaintenancePlan,
  deleteEquipmentMaintenancePlan,
  getEquipmentMaintenancePlanPage,
  updateEquipmentMaintenancePlan,
} from '@/api/equipment/maintenancePlan'
import type {
  EquipmentMaintenancePlan,
  EquipmentMaintenancePlanPageParams,
  EquipmentMaintenancePlanSaveParams,
  EquipmentMaintenanceType,
} from '@/api/equipment/maintenancePlan'
import { loadEquipmentLedgerOptions } from '@/api/equipment/options'

defineOptions({ name: 'EquipmentMaintenancePlanList' })

const equipmentOptions = ref<OptionItem[]>([])
onMounted(async () => {
  equipmentOptions.value = await loadEquipmentLedgerOptions()
})

const filterFields = computed<FilterField[]>(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '编码或名称' },
  { prop: 'equipmentId', label: '设备', type: 'select', options: equipmentOptions.value },
  { prop: 'maintenanceType', label: '保养类型', type: 'select', options: EQUIPMENT_MAINTENANCE_TYPE_OPTIONS },
  { prop: 'status', label: '启停状态', type: 'select', options: EQUIPMENT_ENABLE_STATUS_OPTIONS },
  { prop: 'nextMaintenanceStartTime', label: '下次保养起', type: 'date' },
  { prop: 'nextMaintenanceEndTime', label: '下次保养止', type: 'date' },
])

const columns: ColumnDef<EquipmentMaintenancePlan>[] = [
  { prop: 'planCode', label: '计划编码', width: 140 },
  { prop: 'planName', label: '计划名称', minWidth: 140 },
  { prop: 'equipmentName', label: '设备', minWidth: 140 },
  {
    prop: 'maintenanceType',
    label: '保养类型',
    width: 110,
    statusMap: EQUIPMENT_MAINTENANCE_TYPE_MAP,
  },
  { prop: 'cycleDays', label: '周期(天)', width: 90, align: 'center' },
  { prop: 'nextMaintenanceTime', label: '下次保养时间', width: 170 },
  { prop: 'status', label: '启停状态', width: 90, statusMap: EQUIPMENT_ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<EquipmentMaintenancePlan>[] = [
  { key: 'edit', label: '编辑', roles: MAINTENANCE_PLAN_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: MAINTENANCE_PLAN_WRITE_ROLES,
    confirm: '确认删除该保养计划？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  EquipmentMaintenancePlan,
  Omit<EquipmentMaintenancePlanPageParams, 'pageNo' | 'pageSize'>
>({ fetcher: getEquipmentMaintenancePlanPage })

interface EquipmentMaintenancePlanForm {
  id?: number
  planCode: string
  planName: string
  equipmentId: number | null
  maintenanceType: EquipmentMaintenanceType | null
  cycleDays: number
  maintenanceContent: string
  responsibleUserId: number | null
  nextMaintenanceTime: string
  remark: string | null
  status: number
}

const dialog = useFormDialog<EquipmentMaintenancePlanForm>(
  () => ({
    planCode: '',
    planName: '',
    equipmentId: null,
    maintenanceType: 'ROUTINE',
    cycleDays: 30,
    maintenanceContent: '',
    responsibleUserId: null,
    nextMaintenanceTime: '',
    remark: null,
    status: 1,
  }),
  {
    titles: { create: '新增保养计划', edit: '编辑保养计划' },
    submit: async (model, mode) => {
      const payload: EquipmentMaintenancePlanSaveParams = {
        planCode: model.planCode,
        planName: model.planName,
        equipmentId: model.equipmentId!,
        maintenanceType: model.maintenanceType,
        cycleDays: model.cycleDays,
        maintenanceContent: model.maintenanceContent,
        responsibleUserId: model.responsibleUserId,
        nextMaintenanceTime: model.nextMaintenanceTime,
        remark: model.remark,
        status: model.status,
      }
      if (mode === 'create') {
        await createEquipmentMaintenancePlan(payload)
        ElMessage.success('保养计划已创建')
      } else {
        await updateEquipmentMaintenancePlan(model.id!, payload)
        ElMessage.success('保养计划已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  planCode: [{ required: true, message: '请输入计划编码', trigger: 'blur' }],
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  equipmentId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  cycleDays: [{ required: true, message: '请输入保养周期', trigger: 'blur' }],
  maintenanceContent: [{ required: true, message: '请输入保养内容', trigger: 'blur' }],
  nextMaintenanceTime: [{ required: true, message: '请选择下次保养时间', trigger: 'change' }],
}

async function handleRowAction(key: string, row: EquipmentMaintenancePlan) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row, status: row.status ?? 1 })
    } else if (key === 'delete') {
      await deleteEquipmentMaintenancePlan(row.id)
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
    <PageHeader title="保养计划" description="设备定期保养计划，按周期自动生成保养任务" />
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
          :roles="MAINTENANCE_PLAN_WRITE_ROLES"
          type="primary"
          @click="dialog.open()"
        >
          新增计划
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
      <el-form-item label="计划编码" prop="planCode">
        <el-input
          v-model="dialog.model.value.planCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 MP-001"
        />
      </el-form-item>
      <el-form-item label="计划名称" prop="planName">
        <el-input v-model="dialog.model.value.planName" maxlength="128" />
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
      <el-form-item label="保养类型" prop="maintenanceType">
        <el-select v-model="dialog.model.value.maintenanceType">
          <el-option
            v-for="opt in EQUIPMENT_MAINTENANCE_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="周期(天)" prop="cycleDays">
        <el-input-number v-model="dialog.model.value.cycleDays" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="保养内容" prop="maintenanceContent">
        <el-input
          v-model="dialog.model.value.maintenanceContent"
          type="textarea"
          :rows="3"
          maxlength="500"
        />
      </el-form-item>
      <el-form-item label="负责人ID" prop="responsibleUserId">
        <el-input-number
          v-model="dialog.model.value.responsibleUserId"
          :min="1"
          controls-position="right"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="下次保养" prop="nextMaintenanceTime">
        <el-date-picker
          v-model="dialog.model.value.nextMaintenanceTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择时间"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="dialog.model.value.remark" type="textarea" :rows="2" maxlength="255" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="dialog.model.value.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
