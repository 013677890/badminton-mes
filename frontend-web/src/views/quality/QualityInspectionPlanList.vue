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
  INSPECTION_TYPE_OPTIONS,
  INSPECTION_TYPE_TEXT,
  JUDGMENT_METHOD_OPTIONS,
  PLAN_STATUS,
  PLAN_STATUS_MAP,
  PLAN_STATUS_OPTIONS,
  QUALITY_WRITE_ROLES,
} from '@/constants/quality'
import { loadInspectionItemOptions } from '@/api/quality/options'
import {
  auditInspectionPlan,
  createInspectionPlan,
  createInspectionPlanVersion,
  deleteInspectionPlan,
  disableInspectionPlan,
  getInspectionPlanPage,
  updateInspectionPlan,
} from '@/api/quality/inspectionPlan'
import type {
  InspectionPlan,
  InspectionPlanItemSaveReq,
  InspectionPlanPageParams,
  InspectionPlanSaveReq,
} from '@/api/quality/inspectionPlan'

defineOptions({ name: 'QualityInspectionPlanList' })

const itemOptions = ref<OptionItem[]>([])
onMounted(async () => {
  itemOptions.value = await loadInspectionItemOptions()
})

const filterFields: FilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '编码或名称' },
  { prop: 'inspectionType', label: '检验类型', type: 'select', options: INSPECTION_TYPE_OPTIONS },
  { prop: 'planStatus', label: '方案状态', type: 'select', options: PLAN_STATUS_OPTIONS },
]

const columns: ColumnDef<InspectionPlan>[] = [
  { prop: 'planCode', label: '方案编码', width: 130 },
  { prop: 'planName', label: '方案名称', minWidth: 140 },
  {
    prop: 'inspectionType',
    label: '检验类型',
    width: 100,
    formatter: (row) => INSPECTION_TYPE_TEXT[row.inspectionType] ?? row.inspectionType,
  },
  { prop: 'versionNo', label: '版本', width: 70, align: 'center' },
  { prop: 'planStatus', label: '状态', width: 80, statusMap: PLAN_STATUS_MAP },
  { prop: 'effectiveDate', label: '生效日期', width: 120 },
  {
    prop: 'defaultFlag',
    label: '默认方案',
    width: 90,
    align: 'center',
    formatter: (row) => (row.defaultFlag ? '是' : '否'),
  },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<InspectionPlan>[] = [
  {
    key: 'edit',
    label: '编辑',
    roles: QUALITY_WRITE_ROLES,
    show: (row) => row.planStatus === PLAN_STATUS.DRAFT,
  },
  {
    key: 'audit',
    label: '审核生效',
    type: 'success',
    roles: QUALITY_WRITE_ROLES,
    confirm: '审核生效后不可修改，确认？',
    show: (row) => row.planStatus === PLAN_STATUS.DRAFT,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: QUALITY_WRITE_ROLES,
    show: (row) => row.planStatus === PLAN_STATUS.EFFECTIVE,
  },
  {
    key: 'version',
    label: '新版本',
    roles: QUALITY_WRITE_ROLES,
    show: (row) => row.planStatus !== PLAN_STATUS.DRAFT,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: QUALITY_WRITE_ROLES,
    show: (row) => row.planStatus === PLAN_STATUS.DRAFT,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  InspectionPlan,
  InspectionPlanPageParams
>({ fetcher: getInspectionPlanPage })

interface PlanItemForm extends InspectionPlanItemSaveReq {
  _uid: number
}

let itemUidSeed = 0

interface PlanForm {
  id?: number
  planCode: string
  planName: string
  inspectionType: string
  effectiveDate: string
  defaultFlag: boolean
  remark: string
  items: PlanItemForm[]
}

const dialog = useFormDialog<PlanForm>(
  () => ({
    planCode: '',
    planName: '',
    inspectionType: 'FIRST_ARTICLE',
    effectiveDate: '',
    defaultFlag: false,
    remark: '',
    items: [],
  }),
  {
    titles: { create: '新增检验方案', edit: '编辑检验方案' },
    submit: async (model, mode) => {
      if (model.items.length === 0) {
        ElMessage.warning('请至少添加一个检验项目')
        throw new Error('empty items')
      }
      const payload: InspectionPlanSaveReq = {
        planCode: model.planCode,
        planName: model.planName,
        inspectionType: model.inspectionType,
        effectiveDate: model.effectiveDate || undefined,
        defaultFlag: model.defaultFlag,
        remark: model.remark || undefined,
        items: model.items.map(({ _uid: _, ...rest }) => rest),
      }
      if (mode === 'create') {
        await createInspectionPlan(payload)
        ElMessage.success('检验方案草稿已创建')
      } else {
        await updateInspectionPlan(model.id!, payload)
        ElMessage.success('检验方案已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  planCode: [{ required: true, message: '请输入方案编码', trigger: 'blur' }],
  planName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  inspectionType: [{ required: true, message: '请选择检验类型', trigger: 'change' }],
}

/** 添加检验项目明细行 */
function addPlanItem() {
  dialog.model.value.items.push({
    _uid: ++itemUidSeed,
    inspectionItemId: undefined,
    sortOrder: dialog.model.value.items.length + 1,
    sampleQuantity: 1,
    requiredFlag: true,
    standardValue: undefined,
    lowerLimit: undefined,
    upperLimit: undefined,
    judgmentMethod: 'RANGE',
  })
}

/** 删除检验项目明细行 */
function removePlanItem(index: number) {
  dialog.model.value.items.splice(index, 1)
}

async function handleRowAction(key: string, row: InspectionPlan) {
  if (key === 'edit') {
    const detail = await getInspectionPlanDetail(row.id)
    dialog.open('edit', {
      id: detail.id,
      planCode: detail.planCode,
      planName: detail.planName,
      inspectionType: detail.inspectionType,
      effectiveDate: detail.effectiveDate ?? '',
      defaultFlag: detail.defaultFlag,
      remark: detail.remark ?? '',
      items: detail.items.map((item) => ({
        _uid: ++itemUidSeed,
        inspectionItemId: item.inspectionItemId,
        sortOrder: item.sortOrder,
        sampleQuantity: item.sampleQuantity,
        requiredFlag: item.requiredFlag,
        standardValue: item.standardValue ?? undefined,
        lowerLimit: item.lowerLimit ?? undefined,
        upperLimit: item.upperLimit ?? undefined,
        judgmentMethod: item.judgmentMethod,
      })),
    })
  } else if (key === 'audit') {
    await auditInspectionPlan(row.id)
    ElMessage.success('方案已审核生效')
    await refresh()
  } else if (key === 'disable') {
    await ElMessageBox.confirm('停用后不可用于新建检验单，确认？', '停用确认', {
      type: 'warning',
    })
    await disableInspectionPlan(row.id)
    ElMessage.success('方案已停用')
    await refresh()
  } else if (key === 'version') {
    const newId = await createInspectionPlanVersion(row.id)
    ElMessage.success(`新版本草稿已创建，方案 ID: ${newId}`)
    await refresh()
  } else if (key === 'delete') {
    await deleteInspectionPlan(row.id)
    ElMessage.success('已删除')
    await refresh()
  }
}

/** 打开编辑时按 id 拉取方案详情（含明细） */
async function getInspectionPlanDetail(id: number) {
  const { getInspectionPlan } = await import('@/api/quality/inspectionPlan')
  return getInspectionPlan(id)
}
</script>

<template>
  <div class="page">
    <PageHeader title="检验方案" description="按检验类型配置检验项目和判定标准，支持版本管理" />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="260"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="QUALITY_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增方案
        </PermissionButton>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      width="860px"
      @submit="dialog.handleSubmit"
    >
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="方案编码" prop="planCode">
            <el-input
              v-model="dialog.model.value.planCode"
              :disabled="dialog.mode.value === 'edit'"
              maxlength="32"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="方案名称" prop="planName">
            <el-input v-model="dialog.model.value.planName" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="检验类型" prop="inspectionType">
            <el-select v-model="dialog.model.value.inspectionType">
              <el-option
                v-for="opt in INSPECTION_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="生效日期" prop="effectiveDate">
            <el-date-picker
              v-model="dialog.model.value.effectiveDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选填"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="默认方案" prop="defaultFlag">
            <el-switch v-model="dialog.model.value.defaultFlag" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">检验项目明细</el-divider>
      <div class="detail-toolbar">
        <el-button type="primary" size="small" @click="addPlanItem">添加项目</el-button>
      </div>
      <el-table :data="dialog.model.value.items" border size="small" max-height="320">
        <el-table-column label="序号" type="index" width="55" align="center" />
        <el-table-column label="检验项目" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.inspectionItemId" filterable placeholder="选择项目">
              <el-option
                v-for="opt in itemOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.sortOrder" :min="1" :controls="false" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="抽样数" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.sampleQuantity" :min="1" :controls="false" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="判定方式" width="130">
          <template #default="{ row }">
            <el-select v-model="row.judgmentMethod" size="small">
              <el-option
                v-for="opt in JUDGMENT_METHOD_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="标准值" width="100">
          <template #default="{ row }">
            <el-input v-model="row.standardValue" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="下限" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.lowerLimit" :controls="false" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="上限" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.upperLimit" :controls="false" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="必检" width="60" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.requiredFlag" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" size="small" link @click="removePlanItem($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-form-item label="备注" prop="remark" class="mt-12">
        <el-input v-model="dialog.model.value.remark" type="textarea" maxlength="255" />
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
.detail-toolbar {
  margin-bottom: 8px;
}
.mt-12 {
  margin-top: 12px;
}
</style>
