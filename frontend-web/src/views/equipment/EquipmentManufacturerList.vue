<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
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
} from '@/constants/equipment'
import {
  createEquipmentManufacturer,
  deleteEquipmentManufacturer,
  getEquipmentManufacturerPage,
  updateEquipmentManufacturer,
} from '@/api/equipment/manufacturer'
import type {
  EquipmentManufacturer,
  EquipmentManufacturerPageParams,
  EquipmentManufacturerSaveParams,
} from '@/api/equipment/manufacturer'

defineOptions({ name: 'EquipmentManufacturerList' })

const MANUFACTURER_WRITE_ROLES = [ROLES.ADMIN, ROLES.WORKSHOP_MANAGER]

const filterFields: FilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '编码或名称' },
  { prop: 'status', label: '状态', type: 'select', options: EQUIPMENT_ENABLE_STATUS_OPTIONS },
]

const columns: ColumnDef<EquipmentManufacturer>[] = [
  { prop: 'manufacturerCode', label: '制造商编码', width: 140 },
  { prop: 'manufacturerName', label: '制造商名称', minWidth: 160 },
  { prop: 'contactPerson', label: '联系人', width: 100 },
  { prop: 'contactPhone', label: '联系电话', width: 130 },
  { prop: 'address', label: '地址', minWidth: 180, showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 80, statusMap: EQUIPMENT_ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<EquipmentManufacturer>[] = [
  { key: 'edit', label: '编辑', roles: MANUFACTURER_WRITE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: MANUFACTURER_WRITE_ROLES,
    confirm: '确认删除该制造商？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  EquipmentManufacturer,
  Omit<EquipmentManufacturerPageParams, 'pageNo' | 'pageSize'>
>({ fetcher: getEquipmentManufacturerPage })

interface EquipmentManufacturerForm {
  id?: number
  manufacturerCode: string
  manufacturerName: string
  contactPerson: string | null
  contactPhone: string | null
  contactEmail: string | null
  address: string | null
  website: string | null
  remark: string | null
  status: number
}

const dialog = useFormDialog<EquipmentManufacturerForm>(
  () => ({
    manufacturerCode: '',
    manufacturerName: '',
    contactPerson: null,
    contactPhone: null,
    contactEmail: null,
    address: null,
    website: null,
    remark: null,
    status: 1,
  }),
  {
    titles: { create: '新增制造商', edit: '编辑制造商' },
    submit: async (model, mode) => {
      const payload: EquipmentManufacturerSaveParams = {
        manufacturerCode: model.manufacturerCode,
        manufacturerName: model.manufacturerName,
        contactPerson: model.contactPerson,
        contactPhone: model.contactPhone,
        contactEmail: model.contactEmail,
        address: model.address,
        website: model.website,
        remark: model.remark,
        status: model.status,
      }
      if (mode === 'create') {
        await createEquipmentManufacturer(payload)
        ElMessage.success('制造商已创建')
      } else {
        await updateEquipmentManufacturer(model.id!, payload)
        ElMessage.success('制造商已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  manufacturerCode: [{ required: true, message: '请输入制造商编码', trigger: 'blur' }],
  manufacturerName: [{ required: true, message: '请输入制造商名称', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: EquipmentManufacturer) {
  try {
    if (key === 'edit') {
      dialog.open('edit', { ...row })
    } else if (key === 'delete') {
      await deleteEquipmentManufacturer(row.id)
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
    <PageHeader title="设备制造商" description="设备供应商档案，记录联系方式与资质信息" />
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
          :roles="MANUFACTURER_WRITE_ROLES"
          type="primary"
          @click="dialog.open()"
        >
          新增制造商
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
      <el-form-item label="制造商编码" prop="manufacturerCode">
        <el-input
          v-model="dialog.model.value.manufacturerCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 MF-001"
        />
      </el-form-item>
      <el-form-item label="制造商名称" prop="manufacturerName">
        <el-input v-model="dialog.model.value.manufacturerName" maxlength="128" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactPerson">
        <el-input v-model="dialog.model.value.contactPerson" maxlength="32" placeholder="选填" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="dialog.model.value.contactPhone" maxlength="20" placeholder="选填" />
      </el-form-item>
      <el-form-item label="联系邮箱" prop="contactEmail">
        <el-input v-model="dialog.model.value.contactEmail" maxlength="64" placeholder="选填" />
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model="dialog.model.value.address" maxlength="255" placeholder="选填" />
      </el-form-item>
      <el-form-item label="网址" prop="website">
        <el-input v-model="dialog.model.value.website" maxlength="128" placeholder="选填" />
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
