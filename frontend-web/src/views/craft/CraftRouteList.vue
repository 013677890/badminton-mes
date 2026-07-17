<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, DetailColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import MasterDetailForm from '@/components/business/MasterDetailForm.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { ROUTE_STATUS, ROUTE_STATUS_MAP, statusMapToOptions } from '@/constants/production'
import {
  CRAFT_WRITE_ROLES,
  ROUTE_CHANGE_TYPE_TEXT,
  ROUTE_SOURCE_OPTIONS,
  ROUTE_SOURCE_TEXT,
} from '@/constants/craft'
import { loadProcessOptions, loadProductOptions } from '@/api/production/options'
import {
  approveRoute,
  createRoute,
  createRouteVersion,
  deleteRoute,
  disableRoute,
  getRoute,
  getRouteChangeLogPage,
  getRoutePage,
  updateRoute,
} from '@/api/craft/route'
import type { CraftRoute, CraftRouteChangeLog, CraftRoutePageParams } from '@/api/craft/route'

defineOptions({ name: 'CraftRouteList' })

// ---------- 下拉选项 ----------

const productOptions = ref<OptionItem[]>([])
const processOptions = ref<OptionItem[]>([])

onMounted(async () => {
  try {
    const [products, processes] = await Promise.all([loadProductOptions(), loadProcessOptions()])
    productOptions.value = products
    processOptions.value = processes
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

// ---------- 列表 ----------

const filterFields: FilterField[] = [
  { prop: 'routingCode', label: '路线编码', type: 'input' },
  { prop: 'routingName', label: '路线名称', type: 'input' },
  {
    prop: 'routingStatus',
    label: '状态',
    type: 'select',
    options: statusMapToOptions(ROUTE_STATUS_MAP),
  },
  { prop: 'sourceType', label: '来源', type: 'select', options: ROUTE_SOURCE_OPTIONS },
]

const columns: ColumnDef<CraftRoute>[] = [
  { prop: 'routingCode', label: '路线编码', width: 130 },
  { prop: 'routingName', label: '路线名称', minWidth: 140 },
  { prop: 'routingVersion', label: '版本', width: 80 },
  {
    prop: 'products',
    label: '适用产品',
    minWidth: 160,
    showOverflowTooltip: true,
    formatter: (row) =>
      row.products?.length
        ? row.products.map((item) => item.productCode ?? item.productId).join('、')
        : '-',
  },
  {
    prop: 'steps',
    label: '工序数',
    width: 80,
    align: 'center',
    formatter: (row) => String(row.steps?.length ?? 0),
  },
  {
    prop: 'sourceType',
    label: '来源',
    width: 90,
    formatter: (row) => ROUTE_SOURCE_TEXT[row.sourceType] ?? String(row.sourceType),
  },
  { prop: 'routingStatus', label: '状态', width: 80, statusMap: ROUTE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<CraftRoute>[] = [
  { key: 'view', label: '查看' },
  {
    key: 'edit',
    label: '编辑',
    roles: CRAFT_WRITE_ROLES,
    show: (row) => row.routingStatus === ROUTE_STATUS.DRAFT,
  },
  {
    key: 'approve',
    label: '审核生效',
    type: 'success',
    roles: CRAFT_WRITE_ROLES,
    show: (row) => row.routingStatus === ROUTE_STATUS.DRAFT,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: CRAFT_WRITE_ROLES,
    show: (row) => row.routingStatus === ROUTE_STATUS.EFFECTIVE,
  },
  {
    key: 'newVersion',
    label: '新版本',
    roles: CRAFT_WRITE_ROLES,
    show: (row) => row.routingStatus === ROUTE_STATUS.EFFECTIVE,
  },
  { key: 'log', label: '日志' },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: CRAFT_WRITE_ROLES,
    confirm: '仅草稿可删除，确认删除？',
    show: (row) => row.routingStatus === ROUTE_STATUS.DRAFT,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  CraftRoute,
  CraftRoutePageParams
>({ fetcher: getRoutePage })

// ---------- 主从编辑弹窗 ----------

interface StepRow extends Record<string, unknown> {
  processId: number | null
  /** 查看模式回显（停用工序不在下拉里） */
  processLabel: string
  stationId: number | null
  equipmentCategoryId: number | null
  inspectNode: boolean
  sopId: number | null
  qualityPlanId: number | null
}

interface RouteMaster extends Record<string, unknown> {
  routingCode: string
  routingName: string
  routingVersion: string
  sourceType: number
  productIds: number[]
}

type EditorMode = 'create' | 'edit' | 'view'

const editorVisible = ref(false)
const editorMode = ref<EditorMode>('create')
const editorLoading = ref(false)
const submitLoading = ref(false)
const editingId = ref<number>()
const editingVersion = ref(0)
const master = ref<RouteMaster>(newMaster())
const steps = ref<StepRow[]>([])
/** 查看模式补充展示（含停用产品），编辑模式直接用 productOptions */
const productDisplayOptions = ref<OptionItem[]>([])
/** 泛型组件的 InstanceType 推导受限，仅声明用到的暴露方法 */
const mdfRef = ref<{ validate: () => Promise<boolean> }>()

const editorTitles: Record<EditorMode, string> = {
  create: '新增工艺路线',
  edit: '编辑工艺路线（草稿）',
  view: '工艺路线详情',
}

function newMaster(): RouteMaster {
  return { routingCode: '', routingName: '', routingVersion: 'V1.0', sourceType: 1, productIds: [] }
}

function newStepRow(): StepRow {
  return {
    processId: null,
    processLabel: '',
    stationId: null,
    equipmentCategoryId: null,
    inspectNode: false,
    sopId: null,
    qualityPlanId: null,
  }
}

const masterRules = {
  routingCode: [
    { required: true, message: '请输入路线编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  routingName: [{ required: true, message: '请输入路线名称', trigger: 'blur' }],
  routingVersion: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]+$/, message: '仅限字母、数字、点、下划线和连字符', trigger: 'blur' },
  ],
  productIds: [{ required: true, message: '请选择适用产品', trigger: 'change' }],
}

const stepColumns: DetailColumnDef<StepRow>[] = [
  { prop: 'processId', label: '工序', required: true, minWidth: 200 },
  { prop: 'inspectNode', label: '检验节点', width: 90, align: 'center' },
  { prop: 'stationId', label: '工位 ID', width: 120 },
  { prop: 'equipmentCategoryId', label: '设备类别 ID', width: 130 },
  { prop: 'sopId', label: 'SOP ID', width: 120 },
  { prop: 'qualityPlanId', label: '质检方案 ID', width: 130 },
]

function processLabel(row: StepRow): string {
  const found = processOptions.value.find((opt) => opt.value === row.processId)
  return found ? found.label : row.processLabel || String(row.processId ?? '-')
}

function openCreate() {
  editorMode.value = 'create'
  editingId.value = undefined
  master.value = newMaster()
  steps.value = [newStepRow()]
  productDisplayOptions.value = productOptions.value
  editorVisible.value = true
}

async function openExisting(row: CraftRoute, mode: EditorMode) {
  editorMode.value = mode
  editingId.value = row.id
  editorVisible.value = true
  editorLoading.value = true
  try {
    // 拉聚合详情，拿最新步骤/产品与乐观锁版本（列表行可能滞后）
    const route = await getRoute(row.id)
    editingVersion.value = route.version
    master.value = {
      routingCode: route.routingCode,
      routingName: route.routingName,
      routingVersion: route.routingVersion,
      sourceType: route.sourceType,
      productIds: route.products.map((item) => item.productId),
    }
    steps.value = [...route.steps]
      .sort((a, b) => a.sequenceNo - b.sequenceNo)
      .map((step) => ({
        processId: step.processId,
        processLabel: `${step.processCode} ${step.processName}`,
        stationId: step.stationId,
        equipmentCategoryId: step.equipmentCategoryId,
        inspectNode: step.inspectNode,
        sopId: step.sopId,
        qualityPlanId: step.qualityPlanId,
      }))
    // 路线可能挂着已停用产品（不在启用下拉里），合并进展示选项避免回显成裸 ID
    const missing = route.products
      .filter((item) => !productOptions.value.some((opt) => opt.value === item.productId))
      .map((item) => ({
        label: `${item.productCode ?? item.productId} ${item.productName ?? ''}`.trim(),
        value: item.productId,
      }))
    productDisplayOptions.value = [...productOptions.value, ...missing]
  } catch {
    editorVisible.value = false
  } finally {
    editorLoading.value = false
  }
}

async function handleEditorSubmit() {
  const valid = await mdfRef.value?.validate()
  if (!valid) return
  const payload = {
    routingCode: master.value.routingCode,
    routingName: master.value.routingName,
    routingVersion: master.value.routingVersion,
    sourceType: master.value.sourceType,
    productIds: master.value.productIds,
    // 顺序号按行序连续生成，拖拽/删行后无需手工重排
    steps: steps.value.map((row, index) => ({
      sequenceNo: index + 1,
      processId: row.processId!,
      stationId: row.stationId ?? undefined,
      equipmentCategoryId: row.equipmentCategoryId ?? undefined,
      inspectNode: row.inspectNode,
      sopId: row.sopId ?? undefined,
      qualityPlanId: row.qualityPlanId ?? undefined,
    })),
  }
  submitLoading.value = true
  try {
    if (editorMode.value === 'create') {
      await createRoute(payload)
      ElMessage.success('工艺路线已创建（草稿）')
    } else {
      await updateRoute(editingId.value!, { ...payload, version: editingVersion.value })
      ElMessage.success('工艺路线已更新')
    }
    editorVisible.value = false
    await refresh()
  } catch {
    // 提示由拦截器弹出
  } finally {
    submitLoading.value = false
  }
}

// ---------- 审核生效 / 停用（需原因） ----------

interface ActionForm {
  id?: number
  version?: number
  action: 'approve' | 'disable'
  reason: string
}

const actionDialog = useFormDialog<ActionForm>(
  () => ({ action: 'approve', reason: '' }),
  {
    submit: async (model) => {
      const payload = { version: model.version!, reason: model.reason }
      if (model.action === 'approve') {
        await approveRoute(model.id!, payload)
        ElMessage.success('路线已生效，成为适用产品的默认路线')
      } else {
        await disableRoute(model.id!, payload)
        ElMessage.success('路线已停用')
      }
    },
    onSuccess: refresh,
  },
)

const actionDialogTitle = computed(() =>
  actionDialog.model.value.action === 'approve' ? '审核生效' : '停用路线',
)

const actionRules = {
  reason: [{ required: true, message: '请填写原因', trigger: 'blur' }],
}

// ---------- 新版本 ----------

interface NewVersionForm {
  sourceId?: number
  sourceVersion?: number
  newRoutingVersion: string
  reason: string
}

const newVersionDialog = useFormDialog<NewVersionForm>(
  () => ({ newRoutingVersion: '', reason: '' }),
  {
    titles: { create: '创建新版本' },
    submit: async (model) => {
      await createRouteVersion(model.sourceId!, {
        version: model.sourceVersion!,
        newRoutingVersion: model.newRoutingVersion,
        reason: model.reason,
      })
      ElMessage.success('新版本已创建（草稿），可编辑后审核生效')
    },
    onSuccess: refresh,
  },
)

const newVersionRules = {
  newRoutingVersion: [
    { required: true, message: '请输入新版本号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]+$/, message: '仅限字母、数字、点、下划线和连字符', trigger: 'blur' },
  ],
  reason: [{ required: true, message: '请填写原因', trigger: 'blur' }],
}

// ---------- 变更日志抽屉 ----------

const logDrawerVisible = ref(false)
const logRoute = ref<CraftRoute>()
const logs = ref<CraftRouteChangeLog[]>([])
const logLoading = ref(false)
const logPagination = ref({ pageNo: 1, pageSize: 10, total: 0 })

function openLogDrawer(row: CraftRoute) {
  logRoute.value = row
  logs.value = []
  logPagination.value = { pageNo: 1, pageSize: 10, total: 0 }
  logDrawerVisible.value = true
  void loadLogs()
}

async function loadLogs() {
  if (!logRoute.value) return
  logLoading.value = true
  try {
    const page = await getRouteChangeLogPage(logRoute.value.id, {
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

async function handleRowAction(key: string, row: CraftRoute) {
  try {
    if (key === 'view') {
      await openExisting(row, 'view')
    } else if (key === 'edit') {
      await openExisting(row, 'edit')
    } else if (key === 'approve' || key === 'disable') {
      actionDialog.open('edit', { id: row.id, version: row.version, action: key, reason: '' })
    } else if (key === 'newVersion') {
      newVersionDialog.open('create', {
        sourceId: row.id,
        sourceVersion: row.version,
        newRoutingVersion: '',
        reason: '',
      })
    } else if (key === 'log') {
      openLogDrawer(row)
    } else if (key === 'delete') {
      await deleteRoute(row.id, row.version)
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
      title="工艺路线"
      description="产品加工的工序序列：草稿 → 审核生效 → 停用；生效路线成为适用产品的默认路线，供工单与派工引用"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="300"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="CRAFT_WRITE_ROLES" type="primary" @click="openCreate">
          新增路线
        </PermissionButton>
      </template>
    </FilterTable>

    <!-- 主从编辑：MasterDetailForm 自带 el-form，不复用 FormDialog -->
    <el-dialog
      v-model="editorVisible"
      :title="editorTitles[editorMode]"
      width="960px"
      destroy-on-close
      :close-on-click-modal="false"
      append-to-body
    >
      <el-scrollbar max-height="62vh">
        <MasterDetailForm
          ref="mdfRef"
          v-loading="editorLoading"
          v-model:details="steps"
          :master-model="master"
          :rules="masterRules"
          :detail-columns="stepColumns"
          detail-title="工序步骤"
          :create-detail-row="newStepRow"
          :readonly="editorMode === 'view'"
          :min-details="1"
        >
          <template #master>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="路线编码" prop="routingCode">
                  <el-input
                    v-model="master.routingCode"
                    :disabled="editorMode === 'edit'"
                    maxlength="32"
                    placeholder="如 RT-SC-A1"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="路线名称" prop="routingName">
                  <el-input v-model="master.routingName" maxlength="64" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="版本号" prop="routingVersion">
                  <el-input v-model="master.routingVersion" maxlength="16" placeholder="如 V1.0" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="16">
                <el-form-item label="适用产品" prop="productIds">
                  <el-select
                    v-model="master.productIds"
                    multiple
                    filterable
                    placeholder="可多选，生效后成为这些产品的默认路线"
                  >
                    <el-option
                      v-for="opt in productDisplayOptions"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="来源" prop="sourceType">
                  <el-radio-group v-model="master.sourceType">
                    <el-radio
                      v-for="opt in ROUTE_SOURCE_OPTIONS"
                      :key="opt.value"
                      :value="opt.value"
                    >
                      {{ opt.label }}
                    </el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <template #detail-row-processId="{ row }">
            <el-select
              v-if="editorMode !== 'view'"
              v-model="row.processId"
              filterable
              placeholder="选择工序"
            >
              <el-option
                v-for="opt in processOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <span v-else>{{ processLabel(row as StepRow) }}</span>
          </template>

          <template #detail-row-inspectNode="{ row }">
            <el-switch v-if="editorMode !== 'view'" v-model="row.inspectNode" />
            <span v-else>{{ row.inspectNode ? '是' : '否' }}</span>
          </template>

          <template #detail-row-stationId="{ row }">
            <el-input-number
              v-if="editorMode !== 'view'"
              v-model="row.stationId"
              :min="1"
              controls-position="right"
              class="full-width"
              placeholder="选填"
            />
            <span v-else>{{ row.stationId ?? '-' }}</span>
          </template>

          <template #detail-row-equipmentCategoryId="{ row }">
            <el-input-number
              v-if="editorMode !== 'view'"
              v-model="row.equipmentCategoryId"
              :min="1"
              controls-position="right"
              class="full-width"
              placeholder="空则继承工序"
            />
            <span v-else>{{ row.equipmentCategoryId ?? '-' }}</span>
          </template>

          <template #detail-row-sopId="{ row }">
            <el-input-number
              v-if="editorMode !== 'view'"
              v-model="row.sopId"
              :min="1"
              controls-position="right"
              class="full-width"
              placeholder="选填"
            />
            <span v-else>{{ row.sopId ?? '-' }}</span>
          </template>

          <template #detail-row-qualityPlanId="{ row }">
            <el-input-number
              v-if="editorMode !== 'view'"
              v-model="row.qualityPlanId"
              :min="1"
              controls-position="right"
              class="full-width"
              placeholder="选填"
            />
            <span v-else>{{ row.qualityPlanId ?? '-' }}</span>
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

    <!-- 审核生效 / 停用 -->
    <FormDialog
      v-model:visible="actionDialog.visible.value"
      :title="actionDialogTitle"
      :model="actionDialog.model.value"
      :rules="actionRules"
      :submit-loading="actionDialog.submitLoading.value"
      width="480px"
      @submit="actionDialog.handleSubmit"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        :title="
          actionDialog.model.value.action === 'approve'
            ? '生效后同产品旧默认路线将被替代，新工单按本路线展开工序'
            : '停用后新工单不可再引用该路线，在制工单不受影响'
        "
        class="dialog-tip"
      />
      <el-form-item label="原因" prop="reason">
        <el-input
          v-model="actionDialog.model.value.reason"
          type="textarea"
          :rows="3"
          maxlength="255"
          placeholder="必填，记入变更日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 新版本 -->
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
        title="以当前生效路线为模板复制产品与步骤，生成草稿态新版本"
        class="dialog-tip"
      />
      <el-form-item label="新版本号" prop="newRoutingVersion">
        <el-input
          v-model="newVersionDialog.model.value.newRoutingVersion"
          maxlength="16"
          placeholder="如 V2.0"
        />
      </el-form-item>
      <el-form-item label="原因" prop="reason">
        <el-input
          v-model="newVersionDialog.model.value.reason"
          type="textarea"
          :rows="3"
          maxlength="255"
          placeholder="必填，记入变更日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 变更日志 -->
    <el-drawer
      v-model="logDrawerVisible"
      :title="`变更日志：${logRoute?.routingCode ?? ''}（${logRoute?.routingVersion ?? ''}）`"
      size="640px"
      destroy-on-close
    >
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
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            {{ ROUTE_CHANGE_TYPE_TEXT[row.changeType] ?? row.changeType }}
          </template>
        </el-table-column>
        <el-table-column prop="changeReason" label="原因" min-width="140">
          <template #default="{ row }">{{ row.changeReason ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="operatorId" label="操作人" width="90" />
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
