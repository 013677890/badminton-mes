<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  BARCODE_CONFIG_ROLES,
  BARCODE_ENABLE_STATUS_MAP,
  BARCODE_ENABLE_STATUS_OPTIONS,
  TEMPLATE_FIELD_TYPE_OPTIONS,
} from '@/constants/barcode'
import {
  createBarcodeTemplate,
  disableBarcodeTemplate,
  enableBarcodeTemplate,
  getBarcodeTemplate,
  getBarcodeTemplatePage,
  updateBarcodeTemplate,
} from '@/api/barcode/template'
import type {
  BarcodeTemplate,
  BarcodeTemplateFieldSaveReq,
  BarcodeTemplatePageParams,
  BarcodeTemplateSaveReq,
} from '@/api/barcode/template'

defineOptions({ name: 'BarcodeTemplateList' })

const filterFields: FilterField[] = [
  { prop: 'templateCode', label: '模板编码', type: 'input' },
  { prop: 'templateName', label: '模板名称', type: 'input' },
  { prop: 'status', label: '状态', type: 'select', options: BARCODE_ENABLE_STATUS_OPTIONS },
]

const columns: ColumnDef<BarcodeTemplate>[] = [
  { prop: 'templateCode', label: '模板编码', width: 140 },
  { prop: 'templateName', label: '模板名称', minWidth: 140 },
  { prop: 'paperWidth', label: '宽(mm)', width: 90, align: 'center' },
  { prop: 'paperHeight', label: '高(mm)', width: 90, align: 'center' },
  { prop: 'version', label: '版本', width: 90, align: 'center' },
  { prop: 'status', label: '状态', width: 80, statusMap: BARCODE_ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<BarcodeTemplate>[] = [
  { key: 'edit', label: '编辑', roles: BARCODE_CONFIG_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '确认启用该标签模板？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BARCODE_CONFIG_ROLES,
    confirm: '停用后不可用于新应用规则，确认？',
    show: (row) => row.status === 1,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  BarcodeTemplate,
  BarcodeTemplatePageParams
>({ fetcher: getBarcodeTemplatePage })

interface TemplateFieldForm extends BarcodeTemplateFieldSaveReq {
  _uid: number
}

let fieldUidSeed = 0

interface TemplateForm {
  id?: number
  templateCode: string
  templateName: string
  paperWidth: number
  paperHeight: number
  fields: TemplateFieldForm[]
}

const dialog = useFormDialog<TemplateForm>(
  () => ({
    templateCode: '',
    templateName: '',
    paperWidth: 60,
    paperHeight: 40,
    fields: [],
  }),
  {
    titles: { create: '新增标签模板', edit: '编辑标签模板' },
    submit: async (model, mode) => {
      if (model.fields.length === 0) {
        ElMessage.warning('请至少添加一个字段')
        throw new Error('empty fields')
      }
      const payload: BarcodeTemplateSaveReq = {
        templateCode: model.templateCode,
        templateName: model.templateName,
        paperWidth: model.paperWidth,
        paperHeight: model.paperHeight,
        fields: model.fields.map(({ _uid: _, ...rest }) => rest),
      }
      if (mode === 'create') {
        await createBarcodeTemplate(payload)
        ElMessage.success('标签模板已创建')
      } else {
        await updateBarcodeTemplate(model.id!, payload)
        ElMessage.success('标签模板已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  templateCode: [
    { required: true, message: '请输入模板编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  paperWidth: [{ required: true, message: '请输入纸张宽度', trigger: 'blur' }],
  paperHeight: [{ required: true, message: '请输入纸张高度', trigger: 'blur' }],
}

function addField() {
  dialog.model.value.fields.push({
    _uid: ++fieldUidSeed,
    fieldName: '',
    fieldType: 2,
    dataSource: '',
    posX: 0,
    posY: 0,
    fontSize: 12,
  })
}

function removeField(index: number) {
  dialog.model.value.fields.splice(index, 1)
}

async function handleRowAction(key: string, row: BarcodeTemplate) {
  try {
    if (key === 'edit') {
      const detail = await getBarcodeTemplate(row.id)
      dialog.open('edit', {
        id: detail.id,
        templateCode: detail.templateCode,
        templateName: detail.templateName,
        paperWidth: detail.paperWidth,
        paperHeight: detail.paperHeight,
        fields: detail.fields.map((field) => ({
          _uid: ++fieldUidSeed,
          fieldName: field.fieldName,
          fieldType: field.fieldType,
          dataSource: field.dataSource,
          posX: field.posX,
          posY: field.posY,
          fontSize: field.fontSize ?? 12,
        })),
      })
    } else if (key === 'enable' || key === 'disable') {
      if (key === 'enable') await enableBarcodeTemplate(row.id)
      else await disableBarcodeTemplate(row.id)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用')
      await refresh()
    }
  } catch {
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="条码模板" description="标签打印模板与字段布局配置，停用即废弃（不删除）" />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="160"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="BARCODE_CONFIG_ROLES" type="primary" @click="dialog.open()">
          新增模板
        </PermissionButton>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      width="900px"
      @submit="dialog.handleSubmit"
    >
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="模板编码" prop="templateCode">
            <el-input
              v-model="dialog.model.value.templateCode"
              :disabled="dialog.mode.value === 'edit'"
              maxlength="32"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="模板名称" prop="templateName">
            <el-input v-model="dialog.model.value.templateName" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="宽(mm)" prop="paperWidth">
            <el-input-number
              v-model="dialog.model.value.paperWidth"
              :min="0.01"
              :precision="2"
              :controls="false"
            />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="高(mm)" prop="paperHeight">
            <el-input-number
              v-model="dialog.model.value.paperHeight"
              :min="0.01"
              :precision="2"
              :controls="false"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">字段配置</el-divider>
      <div class="detail-toolbar">
        <el-button type="primary" size="small" @click="addField">添加字段</el-button>
        <span class="form-tip">须包含至少一个条码或二维码字段</span>
      </div>
      <el-table :data="dialog.model.value.fields" border size="small" max-height="320">
        <el-table-column label="序号" type="index" width="55" align="center" />
        <el-table-column label="字段名称" min-width="130">
          <template #default="{ row }">
            <el-input v-model="row.fieldName" size="small" placeholder="如条码值" />
          </template>
        </el-table-column>
        <el-table-column label="字段类型" width="120">
          <template #default="{ row }">
            <el-select v-model="row.fieldType" size="small">
              <el-option
                v-for="opt in TEMPLATE_FIELD_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数据来源" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.dataSource" size="small" placeholder="如 barcodeValue" />
          </template>
        </el-table-column>
        <el-table-column label="X(mm)" width="85">
          <template #default="{ row }">
            <el-input-number v-model="row.posX" :min="0" :precision="2" :controls="false" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="Y(mm)" width="85">
          <template #default="{ row }">
            <el-input-number v-model="row.posY" :min="0" :precision="2" :controls="false" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="字号" width="80">
          <template #default="{ row }">
            <el-input-number v-model="row.fontSize" :min="1" :max="255" :controls="false" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" size="small" link @click="removeField($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
