<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, DetailColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import MasterDetailForm from '@/components/business/MasterDetailForm.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  BOM_STATUS,
  BOM_STATUS_MAP,
  BOM_WRITE_ROLES,
  statusMapToOptions,
} from '@/constants/production'
import { loadMaterialOptions, loadProductOptions } from '@/api/production/options'
import {
  activateBom,
  createBom,
  createBomNewVersion,
  deleteBom,
  disableBom,
  getBom,
  getBomPage,
  updateBom,
} from '@/api/production/bom'
import type { Bom, BomPageParams } from '@/api/production/bom'

defineOptions({ name: 'BomList' })

// ---------- 下拉选项 ----------

const productOptions = ref<OptionItem[]>([])
const materialOptions = ref<OptionItem[]>([])

const filterFields = ref<FilterField[]>([
  { prop: 'bomCode', label: 'BOM 编码', type: 'input' },
  { prop: 'productId', label: '产品', type: 'select', options: [] },
  { prop: 'bomStatus', label: '状态', type: 'select', options: statusMapToOptions(BOM_STATUS_MAP) },
])

onMounted(async () => {
  try {
    const [products, materials] = await Promise.all([loadProductOptions(), loadMaterialOptions()])
    productOptions.value = products
    materialOptions.value = materials
    const field = filterFields.value.find((item) => item.prop === 'productId')
    if (field) field.options = products
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

// ---------- 列表 ----------

const columns: ColumnDef<Bom>[] = [
  { prop: 'bomCode', label: 'BOM 编码', width: 150 },
  {
    prop: 'productName',
    label: '产品',
    minWidth: 180,
    formatter: (row) => `${row.productCode} ${row.productName}`,
  },
  { prop: 'version', label: '版本', width: 90 },
  { prop: 'bomStatus', label: '状态', width: 90, statusMap: BOM_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<Bom>[] = [
  { key: 'view', label: '查看' },
  {
    key: 'edit',
    label: '编辑',
    roles: BOM_WRITE_ROLES,
    show: (row) => row.bomStatus === BOM_STATUS.DRAFT,
  },
  {
    key: 'activate',
    label: '生效',
    type: 'success',
    roles: BOM_WRITE_ROLES,
    confirm: '生效后同产品旧生效版本将被替代，确认？',
    show: (row) => row.bomStatus === BOM_STATUS.DRAFT,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: BOM_WRITE_ROLES,
    confirm: '停用后新工单不可引用该 BOM，确认？',
    show: (row) => row.bomStatus === BOM_STATUS.EFFECTIVE,
  },
  {
    key: 'newVersion',
    label: '新版本',
    roles: BOM_WRITE_ROLES,
    show: (row) => row.bomStatus !== BOM_STATUS.DRAFT,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: BOM_WRITE_ROLES,
    confirm: '仅草稿可删除，确认删除？',
    show: (row) => row.bomStatus === BOM_STATUS.DRAFT,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  Bom,
  BomPageParams
>({ fetcher: getBomPage })

// ---------- 主从编辑弹窗（嵌套表单，不复用 FormDialog） ----------

interface BomDetailRow extends Record<string, unknown> {
  materialId: number | null
  quantity: number | null
  lossRate: number
}

interface BomMaster extends Record<string, unknown> {
  bomCode: string
  productId: number | null
  version: string
}

type EditorMode = 'create' | 'edit' | 'view'

const editorVisible = ref(false)
const editorMode = ref<EditorMode>('create')
const editorLoading = ref(false)
const submitLoading = ref(false)
const editingId = ref<number>()
const editingLockVersion = ref(0)
const master = ref<BomMaster>({ bomCode: '', productId: null, version: '' })
const details = ref<BomDetailRow[]>([])
/** 泛型组件的 InstanceType 推导受限，仅声明用到的暴露方法 */
const mdfRef = ref<{ validate: () => Promise<boolean> }>()

const editorTitles: Record<EditorMode, string> = {
  create: '新增 BOM',
  edit: '编辑 BOM（草稿）',
  view: 'BOM 详情',
}

const masterRules = {
  bomCode: [
    { required: true, message: '请输入 BOM 编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]+$/, message: '仅限字母、数字、点、下划线和连字符', trigger: 'blur' },
  ],
}

const detailColumns: DetailColumnDef<BomDetailRow>[] = [
  { prop: 'materialId', label: '物料', required: true, minWidth: 220 },
  { prop: 'quantity', label: '标准用量', required: true, width: 160 },
  { prop: 'lossRate', label: '损耗率（%）', required: true, width: 160 },
]

function materialLabel(materialId: unknown): string {
  const found = materialOptions.value.find((opt) => opt.value === materialId)
  return found ? found.label : String(materialId ?? '-')
}

function newDetailRow(): BomDetailRow {
  return { materialId: null, quantity: null, lossRate: 0 }
}

function openCreate() {
  editorMode.value = 'create'
  editingId.value = undefined
  master.value = { bomCode: '', productId: null, version: 'V1.0' }
  details.value = [newDetailRow()]
  editorVisible.value = true
}

async function openExisting(row: Bom, mode: EditorMode) {
  editorMode.value = mode
  editingId.value = row.id
  editorVisible.value = true
  editorLoading.value = true
  try {
    // 拉聚合详情，拿最新明细与 lockVersion（列表行可能不含明细）
    const bom = await getBom(row.id)
    editingLockVersion.value = bom.lockVersion
    master.value = { bomCode: bom.bomCode, productId: bom.productId, version: bom.version }
    details.value = bom.details.map((item) => ({
      materialId: item.materialId,
      quantity: item.quantity,
      lossRate: item.lossRate,
    }))
  } catch {
    editorVisible.value = false
  } finally {
    editorLoading.value = false
  }
}

async function handleEditorSubmit() {
  const valid = await mdfRef.value?.validate()
  if (!valid) return
  for (const [index, row] of details.value.entries()) {
    if (!row.quantity || row.quantity <= 0) {
      ElMessage.warning(`第 ${index + 1} 行标准用量必须大于 0`)
      return
    }
  }
  const payload = {
    bomCode: master.value.bomCode,
    productId: master.value.productId!,
    version: master.value.version,
    details: details.value.map((row) => ({
      materialId: row.materialId!,
      quantity: row.quantity!,
      lossRate: row.lossRate,
    })),
  }
  submitLoading.value = true
  try {
    if (editorMode.value === 'create') {
      await createBom(payload)
      ElMessage.success('BOM 已创建（草稿）')
    } else {
      await updateBom(editingId.value!, { ...payload, lockVersion: editingLockVersion.value })
      ElMessage.success('BOM 已更新')
    }
    editorVisible.value = false
    await refresh()
  } catch {
    // 提示由拦截器弹出
  } finally {
    submitLoading.value = false
  }
}

// ---------- 新版本弹窗 ----------

interface NewVersionForm {
  sourceId?: number
  sourceLockVersion?: number
  bomCode: string
  version: string
}

const newVersionDialog = useFormDialog<NewVersionForm>(
  () => ({ bomCode: '', version: '' }),
  {
    titles: { create: '创建新版本' },
    submit: async (model) => {
      await createBomNewVersion(model.sourceId!, {
        lockVersion: model.sourceLockVersion!,
        bomCode: model.bomCode,
        version: model.version,
      })
      ElMessage.success('新版本已创建（草稿），可编辑后生效')
    },
    onSuccess: refresh,
  },
)

const newVersionRules = {
  bomCode: [
    { required: true, message: '请输入新 BOM 编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  version: [
    { required: true, message: '请输入新版本号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]+$/, message: '仅限字母、数字、点、下划线和连字符', trigger: 'blur' },
  ],
}

// ---------- 行操作 ----------

async function handleRowAction(key: string, row: Bom) {
  try {
    if (key === 'view') {
      await openExisting(row, 'view')
    } else if (key === 'edit') {
      await openExisting(row, 'edit')
    } else if (key === 'activate') {
      await activateBom(row.id, row.lockVersion)
      ElMessage.success('BOM 已生效')
      await refresh()
    } else if (key === 'disable') {
      await disableBom(row.id, row.lockVersion)
      ElMessage.success('BOM 已停用')
      await refresh()
    } else if (key === 'newVersion') {
      newVersionDialog.open('create', {
        sourceId: row.id,
        sourceLockVersion: row.lockVersion,
        bomCode: `${row.bomCode}-N`,
        version: '',
      })
    } else if (key === 'delete') {
      await deleteBom(row.id, row.lockVersion)
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
    <PageHeader
      title="BOM 管理"
      description="产品物料清单：草稿 → 生效 → 停用，同产品同版本唯一，工单下达按生效 BOM 展开物料需求"
    />
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
        <PermissionButton :roles="BOM_WRITE_ROLES" type="primary" @click="openCreate">
          新增 BOM
        </PermissionButton>
      </template>
    </FilterTable>

    <!-- 主从编辑：MasterDetailForm 自带 el-form，不嵌套 FormDialog -->
    <el-dialog
      v-model="editorVisible"
      :title="editorTitles[editorMode]"
      width="860px"
      destroy-on-close
      :close-on-click-modal="false"
      append-to-body
    >
      <el-scrollbar max-height="62vh">
        <MasterDetailForm
          ref="mdfRef"
          v-loading="editorLoading"
          v-model:details="details"
          :master-model="master"
          :rules="masterRules"
          :detail-columns="detailColumns"
          detail-title="物料明细"
          :create-detail-row="newDetailRow"
          :readonly="editorMode === 'view'"
          :min-details="1"
        >
          <template #master>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="BOM 编码" prop="bomCode">
                  <el-input
                    v-model="master.bomCode"
                    :disabled="editorMode === 'edit'"
                    maxlength="32"
                    placeholder="如 BOM-SC-A1"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="产品" prop="productId">
                  <el-select v-model="master.productId" filterable placeholder="请选择产品">
                    <el-option
                      v-for="opt in productOptions"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="版本号" prop="version">
                  <el-input v-model="master.version" maxlength="16" placeholder="如 V1.0" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <template #detail-row-materialId="{ row }">
            <el-select
              v-if="editorMode !== 'view'"
              v-model="row.materialId"
              filterable
              placeholder="选择物料"
            >
              <el-option
                v-for="opt in materialOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <span v-else>{{ materialLabel(row.materialId) }}</span>
          </template>

          <template #detail-row-quantity="{ row }">
            <el-input-number
              v-if="editorMode !== 'view'"
              v-model="row.quantity"
              :min="0.0001"
              :precision="4"
              :step="1"
              controls-position="right"
              class="full-width"
            />
            <span v-else>{{ row.quantity }}</span>
          </template>

          <template #detail-row-lossRate="{ row }">
            <el-input-number
              v-if="editorMode !== 'view'"
              v-model="row.lossRate"
              :min="0"
              :max="100"
              :precision="2"
              :step="0.5"
              controls-position="right"
              class="full-width"
            />
            <span v-else>{{ row.lossRate }}%</span>
          </template>
        </MasterDetailForm>
      </el-scrollbar>
      <template #footer>
        <el-button @click="editorVisible = false">
          {{ editorMode === 'view' ? '关闭' : '取消' }}
        </el-button>
        <el-button
          v-if="editorMode !== 'view'"
          type="primary"
          :loading="submitLoading"
          @click="handleEditorSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <FormDialog
      v-model:visible="newVersionDialog.visible.value"
      :title="newVersionDialog.title.value"
      :model="newVersionDialog.model.value"
      :rules="newVersionRules"
      :submit-loading="newVersionDialog.submitLoading.value"
      width="480px"
      @submit="newVersionDialog.handleSubmit"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="以当前 BOM 为模板复制明细，生成草稿态新版本"
        class="new-version-tip"
      />
      <el-form-item label="新 BOM 编码" prop="bomCode">
        <el-input v-model="newVersionDialog.model.value.bomCode" maxlength="32" />
      </el-form-item>
      <el-form-item label="新版本号" prop="version">
        <el-input v-model="newVersionDialog.model.value.version" maxlength="16" placeholder="如 V2.0" />
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}

.new-version-tip {
  margin-bottom: 16px;
}
</style>
