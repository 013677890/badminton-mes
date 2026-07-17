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
import { ROLES } from '@/constants/production'
import {
  EQUIPMENT_ENABLE_STATUS_MAP,
  EQUIPMENT_ENABLE_STATUS_OPTIONS,
  EQUIPMENT_STATUS_MAP,
  EQUIPMENT_STATUS_OPTIONS,
} from '@/constants/equipment'
import {
  createEquipmentLedger,
  deleteEquipmentLedger,
  getEquipmentLedgerPage,
  updateEquipmentLedger,
} from '@/api/equipment/ledger'
import type {
  EquipmentLedger,
  EquipmentLedgerPageParams,
  EquipmentLedgerSaveParams,
  EquipmentStatus,
} from '@/api/equipment/ledger'
import {
  loadEquipmentCategoryOptions,
  loadEquipmentManufacturerOptions,
} from '@/api/equipment/options'

defineOptions({ name: 'EquipmentLedgerList' })

// 设备台账页面批量加载类别和制造商选项，编辑/删除请求由后端维护设备引用和状态约束。
const LEDGER_WRITE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

const categoryOptions = ref<OptionItem[]>([])
const manufacturerOptions = ref<OptionItem[]>([])
onMounted(async () => {
  const [categories, manufacturers] = await Promise.all([
    loadEquipmentCategoryOptions(),
    loadEquipmentManufacturerOptions(),
  ])
  categoryOptions.value = categories
  manufacturerOptions.value = manufacturers
})

const filterFields = computed<FilterField[]>(() => [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '编码或名称' },
  { prop: 'categoryId', label: '设备类别', type: 'select', options: categoryOptions.value },
  { prop: 'manufacturerId', label: '制造商', type: 'select', options: manufacturerOptions.value },
  { prop: 'equipmentStatus', label: '设备状态', type: 'select', options: EQUIPMENT_STATUS_OPTIONS },
  { prop: 'workshopId', label: '车间ID', type: 'input' },
  { prop: 'status', label: '启停状态', type: 'select', options: EQUIPMENT_ENABLE_STATUS_OPTIONS },
])

const columns: ColumnDef<EquipmentLedger>[] = [
  { prop: 'equipmentCode', label: '设备编码', width: 140 },
  { prop: 'equipmentName', label: '设备名称', minWidth: 140 },
  { prop: 'categoryName', label: '类别', minWidth: 120 },
  { prop: 'manufacturerName', label: '制造商', minWidth: 120 },
  { prop: 'equipmentModel', label: '型号', width: 120 },
  { prop: 'equipmentStatus', label: '设备状态', width: 100, statusMap: EQUIPMENT_STATUS_MAP },
  { prop: 'responsiblePerson', label: '负责人', width: 100 },
  { prop: 'status', label: '启停状态', width: 90, statusMap: EQUIPMENT_ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<EquipmentLedger>[] = [
  { key: 'edit', label: '编辑', roles: LEDGER_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: LEDGER_WRITE_ROLES,
    confirm: '确认删除该设备台账？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  EquipmentLedger,
  Omit<EquipmentLedgerPageParams, 'pageNo' | 'pageSize'>
>({ fetcher: getEquipmentLedgerPage })

interface EquipmentLedgerForm {
  id?: number
  equipmentCode: string
  equipmentName: string
  categoryId: number | null
  manufacturerId: number | null
  equipmentModel: string | null
  serialNumber: string | null
  workshopId: number | null
  productionLineId: number | null
  installationLocation: string | null
  purchaseDate: string | null
  commissioningDate: string | null
  equipmentStatus: EquipmentStatus | null
  responsiblePerson: string | null
  remark: string | null
  status: number
}

const dialog = useFormDialog<EquipmentLedgerForm>(
  () => ({
    equipmentCode: '',
    equipmentName: '',
    categoryId: null,
    manufacturerId: null,
    equipmentModel: null,
    serialNumber: null,
    workshopId: null,
    productionLineId: null,
    installationLocation: null,
    purchaseDate: null,
    commissioningDate: null,
    equipmentStatus: 'IDLE',
    responsiblePerson: null,
    remark: null,
    status: 1,
  }),
  {
    titles: { create: '新增设备', edit: '编辑设备' },
    submit: async (model, mode) => {
      const payload: EquipmentLedgerSaveParams = {
        equipmentCode: model.equipmentCode,
        equipmentName: model.equipmentName,
        categoryId: model.categoryId!,
        manufacturerId: model.manufacturerId,
        equipmentModel: model.equipmentModel,
        serialNumber: model.serialNumber,
        workshopId: model.workshopId,
        productionLineId: model.productionLineId,
        installationLocation: model.installationLocation,
        purchaseDate: model.purchaseDate,
        commissioningDate: model.commissioningDate,
        equipmentStatus: model.equipmentStatus,
        responsiblePerson: model.responsiblePerson,
        remark: model.remark,
        status: model.status,
      }
      if (mode === 'create') {
        await createEquipmentLedger(payload)
        ElMessage.success('设备已创建')
      } else {
        await updateEquipmentLedger(model.id!, payload)
        ElMessage.success('设备已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  equipmentCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  equipmentName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择设备类别', trigger: 'change' }],
}

async function handleRowAction(key: string, row: EquipmentLedger) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row })
    } else if (key === 'delete') {
      await deleteEquipmentLedger(row.id)
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
    <PageHeader title="设备台账" description="设备档案主数据，记录设备基本信息与运行状态" />
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
        <PermissionButton :roles="LEDGER_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增设备
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
      <el-form-item label="设备编码" prop="equipmentCode">
        <el-input
          v-model="dialog.model.value.equipmentCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 EQ-001"
        />
      </el-form-item>
      <el-form-item label="设备名称" prop="equipmentName">
        <el-input v-model="dialog.model.value.equipmentName" maxlength="128" />
      </el-form-item>
      <el-form-item label="设备类别" prop="categoryId">
        <el-select v-model="dialog.model.value.categoryId" filterable placeholder="请选择">
          <el-option
            v-for="opt in categoryOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="制造商" prop="manufacturerId">
        <el-select
          v-model="dialog.model.value.manufacturerId"
          clearable
          filterable
          placeholder="选填"
        >
          <el-option
            v-for="opt in manufacturerOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="型号" prop="equipmentModel">
        <el-input v-model="dialog.model.value.equipmentModel" maxlength="64" placeholder="选填" />
      </el-form-item>
      <el-form-item label="序列号" prop="serialNumber">
        <el-input v-model="dialog.model.value.serialNumber" maxlength="64" placeholder="选填" />
      </el-form-item>
      <el-form-item label="车间ID" prop="workshopId">
        <el-input-number
          v-model="dialog.model.value.workshopId"
          :min="1"
          controls-position="right"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="产线ID" prop="productionLineId">
        <el-input-number
          v-model="dialog.model.value.productionLineId"
          :min="1"
          controls-position="right"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="安装位置" prop="installationLocation">
        <el-input
          v-model="dialog.model.value.installationLocation"
          maxlength="128"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="采购日期" prop="purchaseDate">
        <el-date-picker
          v-model="dialog.model.value.purchaseDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="调试日期" prop="commissioningDate">
        <el-date-picker
          v-model="dialog.model.value.commissioningDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="设备状态" prop="equipmentStatus">
        <el-select v-model="dialog.model.value.equipmentStatus">
          <el-option
            v-for="opt in EQUIPMENT_STATUS_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="负责人" prop="responsiblePerson">
        <el-input
          v-model="dialog.model.value.responsiblePerson"
          maxlength="32"
          placeholder="选填"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="dialog.model.value.remark"
          type="textarea"
          :rows="2"
          maxlength="255"
        />
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
