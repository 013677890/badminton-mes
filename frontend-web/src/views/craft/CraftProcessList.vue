<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { usePermission } from '@/composables/usePermission'
import { ENABLE_STATUS_MAP, ENABLE_STATUS_OPTIONS } from '@/constants/production'
import {
  BOOL_OPTIONS,
  CRAFT_WRITE_ROLES,
  PROCESS_CHANGE_TYPE_TEXT,
  PROCESS_TYPE_OPTIONS,
  PROCESS_TYPE_TEXT,
} from '@/constants/craft'
import {
  createProcess,
  createProcessDefectReason,
  createProcessSop,
  deleteProcess,
  deleteProcessDefectReason,
  deleteProcessSop,
  getProcessChangeLogPage,
  getProcessDefectReasons,
  getProcessPage,
  getProcessSops,
  updateProcess,
  updateProcessDefectReason,
  updateProcessSop,
  updateProcessStatus,
} from '@/api/craft/process'
import type {
  CraftProcess,
  CraftProcessChangeLog,
  CraftProcessDefectReason,
  CraftProcessPageParams,
  CraftProcessSop,
} from '@/api/craft/process'

defineOptions({ name: 'CraftProcessList' })

const { hasRole } = usePermission()
const canWrite = computed(() => hasRole(CRAFT_WRITE_ROLES))

// ---------- 列表 ----------

const filterFields: FilterField[] = [
  { prop: 'processCode', label: '工序编码', type: 'input' },
  { prop: 'processName', label: '工序名称', type: 'input' },
  { prop: 'processType', label: '工序类型', type: 'select', options: PROCESS_TYPE_OPTIONS },
  { prop: 'keyProcess', label: '关键工序', type: 'select', options: BOOL_OPTIONS },
  { prop: 'pieceRateEnabled', label: '计件工序', type: 'select', options: BOOL_OPTIONS },
  { prop: 'status', label: '状态', type: 'select', options: ENABLE_STATUS_OPTIONS },
]

/** 质检/扫码/计件等布尔管控项汇总为一列，避免表格过宽 */
function controlText(row: CraftProcess): string {
  const items: string[] = []
  if (row.keyProcess) items.push('关键')
  if (row.qualityRequired) items.push('质检')
  if (row.scanRequired) items.push('扫码')
  if (row.pieceRateEnabled) items.push('计件')
  return items.length ? items.join(' / ') : '-'
}

const columns: ColumnDef<CraftProcess>[] = [
  { prop: 'processCode', label: '工序编码', width: 120 },
  { prop: 'processName', label: '工序名称', minWidth: 130 },
  {
    prop: 'processType',
    label: '类型',
    width: 90,
    formatter: (row) => PROCESS_TYPE_TEXT[row.processType] ?? row.processType,
  },
  {
    prop: 'standardTimeSeconds',
    label: '标准工时',
    width: 100,
    formatter: (row) => `${row.standardTimeSeconds} 秒`,
  },
  { prop: 'keyProcess', label: '管控项', minWidth: 150, formatter: controlText },
  { prop: 'status', label: '状态', width: 80, statusMap: ENABLE_STATUS_MAP },
  { prop: 'updateTime', label: '更新时间', width: 170 },
]

const rowActions: RowAction<CraftProcess>[] = [
  { key: 'maintain', label: '维护' },
  { key: 'edit', label: '编辑', roles: CRAFT_WRITE_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: CRAFT_WRITE_ROLES,
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: CRAFT_WRITE_ROLES,
    show: (row) => row.status === 1,
  },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: CRAFT_WRITE_ROLES,
    confirm: '被工艺路线引用的工序无法删除，确认删除？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  CraftProcess,
  CraftProcessPageParams
>({ fetcher: getProcessPage })

// ---------- 新增 / 编辑工序 ----------

interface ProcessForm {
  id?: number
  version?: number
  processCode: string
  processName: string
  processType: string
  standardTimeSeconds: number
  keyProcess: boolean
  qualityRequired: boolean
  scanRequired: boolean
  pieceRateEnabled: boolean
  equipmentCategoryId: number | null
  qualityPlanId: number | null
  remark: string
  changeReason: string
}

const dialog = useFormDialog<ProcessForm>(
  () => ({
    processCode: '',
    processName: '',
    processType: 'PROCESSING',
    standardTimeSeconds: 60,
    keyProcess: false,
    qualityRequired: false,
    scanRequired: false,
    pieceRateEnabled: false,
    equipmentCategoryId: null,
    qualityPlanId: null,
    remark: '',
    changeReason: '',
  }),
  {
    titles: { create: '新增工序', edit: '编辑工序' },
    submit: async (model, mode) => {
      const payload = {
        processCode: model.processCode,
        processName: model.processName,
        processType: model.processType,
        standardTimeSeconds: model.standardTimeSeconds,
        keyProcess: model.keyProcess,
        qualityRequired: model.qualityRequired,
        scanRequired: model.scanRequired,
        pieceRateEnabled: model.pieceRateEnabled,
        equipmentCategoryId: model.equipmentCategoryId ?? undefined,
        qualityPlanId: model.qualityPlanId ?? undefined,
        remark: model.remark || undefined,
        changeReason: model.changeReason || undefined,
      }
      if (mode === 'create') {
        await createProcess(payload)
        ElMessage.success('工序已创建')
      } else {
        await updateProcess(model.id!, { ...payload, version: model.version! })
        ElMessage.success('工序已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  processCode: [
    { required: true, message: '请输入工序编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  processName: [{ required: true, message: '请输入工序名称', trigger: 'blur' }],
  processType: [{ required: true, message: '请选择工序类型', trigger: 'change' }],
  standardTimeSeconds: [{ required: true, message: '请输入标准工时', trigger: 'blur' }],
  qualityPlanId: [
    {
      validator: (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
        if (dialog.model.value.qualityRequired && !dialog.model.value.qualityPlanId) {
          callback(new Error('需要质检的工序必须绑定质检方案'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

// ---------- 启停（需填写原因） ----------

interface StatusForm {
  id?: number
  version?: number
  status: number
  reason: string
}

const statusDialog = useFormDialog<StatusForm>(
  () => ({ status: 1, reason: '' }),
  {
    submit: async (model) => {
      await updateProcessStatus(model.id!, {
        version: model.version!,
        status: model.status,
        reason: model.reason,
      })
      ElMessage.success(model.status === 1 ? '工序已启用' : '工序已停用')
    },
    onSuccess: refresh,
  },
)

const statusDialogTitle = computed(() =>
  statusDialog.model.value.status === 1 ? '启用工序' : '停用工序',
)

const statusRules = {
  reason: [{ required: true, message: '请填写启停原因', trigger: 'blur' }],
}

async function handleRowAction(key: string, row: CraftProcess) {
  try {
    if (key === 'maintain') {
      openDrawer(row)
    } else if (key === 'edit') {
      dialog.open('edit', {
        ...row,
        equipmentCategoryId: row.equipmentCategoryId,
        qualityPlanId: row.qualityPlanId,
        remark: row.remark ?? '',
        changeReason: '',
      })
    } else if (key === 'enable' || key === 'disable') {
      statusDialog.open('edit', {
        id: row.id,
        version: row.version,
        status: key === 'enable' ? 1 : 0,
        reason: '',
      })
    } else if (key === 'delete') {
      await deleteProcess(row.id, row.version)
      ElMessage.success('已删除')
      await refresh()
    }
  } catch {
    // 失败提示由拦截器统一弹出；乐观锁冲突时刷新最新数据
    await refresh()
  }
}

// ---------- 维护抽屉（SOP / 不良原因 / 变更日志） ----------

const drawerVisible = ref(false)
const activeTab = ref('sop')
const currentProcess = ref<CraftProcess>()

function openDrawer(row: CraftProcess) {
  currentProcess.value = row
  activeTab.value = 'sop'
  drawerVisible.value = true
  void loadSops()
  void loadReasons()
  logs.value = []
  logPagination.value = { pageNo: 1, pageSize: 10, total: 0 }
  void loadLogs()
}

// ----- SOP 子表 -----

const sops = ref<CraftProcessSop[]>([])
const sopLoading = ref(false)

async function loadSops() {
  if (!currentProcess.value) return
  sopLoading.value = true
  try {
    sops.value = await getProcessSops(currentProcess.value.id)
  } finally {
    sopLoading.value = false
  }
}

interface SopForm {
  id?: number
  version?: number
  sopCode: string
  sopName: string
  sopVersion: string
  fileUrl: string
  status: number
  changeReason: string
}

const sopDialog = useFormDialog<SopForm>(
  () => ({ sopCode: '', sopName: '', sopVersion: 'V1.0', fileUrl: '', status: 1, changeReason: '' }),
  {
    titles: { create: '新增 SOP', edit: '编辑 SOP' },
    submit: async (model, mode) => {
      const processId = currentProcess.value!.id
      const payload = {
        sopCode: model.sopCode,
        sopName: model.sopName,
        sopVersion: model.sopVersion,
        fileUrl: model.fileUrl,
        status: model.status,
        changeReason: model.changeReason || undefined,
      }
      if (mode === 'create') {
        await createProcessSop(processId, payload)
        ElMessage.success('SOP 已创建')
      } else {
        await updateProcessSop(processId, model.id!, { ...payload, version: model.version! })
        ElMessage.success('SOP 已更新，引用该 SOP 的工艺路线需确认重新绑定')
      }
    },
    onSuccess: () => {
      void loadSops()
      void loadLogs()
    },
  },
)

const sopRules = {
  sopCode: [
    { required: true, message: '请输入 SOP 编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  sopName: [{ required: true, message: '请输入 SOP 名称', trigger: 'blur' }],
  sopVersion: [{ required: true, message: '请输入 SOP 版本', trigger: 'blur' }],
  fileUrl: [{ required: true, message: '请输入 SOP 文件地址', trigger: 'blur' }],
}

async function handleDeleteSop(row: CraftProcessSop) {
  try {
    await ElMessageBox.confirm('被工艺路线引用的 SOP 无法删除，确认删除？', '删除 SOP', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteProcessSop(currentProcess.value!.id, row.id, row.version)
    ElMessage.success('已删除')
  } catch {
    // 提示由拦截器弹出
  }
  await loadSops()
  await loadLogs()
}

// ----- 不良原因子表 -----

const reasons = ref<CraftProcessDefectReason[]>([])
const reasonLoading = ref(false)

async function loadReasons() {
  if (!currentProcess.value) return
  reasonLoading.value = true
  try {
    reasons.value = await getProcessDefectReasons(currentProcess.value.id)
  } finally {
    reasonLoading.value = false
  }
}

interface ReasonForm {
  id?: number
  version?: number
  reasonCode: string
  reasonName: string
  status: number
  changeReason: string
}

const reasonDialog = useFormDialog<ReasonForm>(
  () => ({ reasonCode: '', reasonName: '', status: 1, changeReason: '' }),
  {
    titles: { create: '新增不良原因', edit: '编辑不良原因' },
    submit: async (model, mode) => {
      const processId = currentProcess.value!.id
      const payload = {
        reasonCode: model.reasonCode,
        reasonName: model.reasonName,
        status: model.status,
        changeReason: model.changeReason || undefined,
      }
      if (mode === 'create') {
        await createProcessDefectReason(processId, payload)
        ElMessage.success('不良原因已创建')
      } else {
        await updateProcessDefectReason(processId, model.id!, {
          ...payload,
          version: model.version!,
        })
        ElMessage.success('不良原因已更新')
      }
    },
    onSuccess: () => {
      void loadReasons()
      void loadLogs()
    },
  },
)

const reasonRules = {
  reasonCode: [
    { required: true, message: '请输入原因编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  reasonName: [{ required: true, message: '请输入原因名称', trigger: 'blur' }],
}

async function handleDeleteReason(row: CraftProcessDefectReason) {
  try {
    await ElMessageBox.confirm('已被报工引用的原因建议停用而非删除，确认删除？', '删除不良原因', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteProcessDefectReason(currentProcess.value!.id, row.id, row.version)
    ElMessage.success('已删除')
  } catch {
    // 提示由拦截器弹出
  }
  await loadReasons()
  await loadLogs()
}

// ----- 变更日志 -----

const logs = ref<CraftProcessChangeLog[]>([])
const logLoading = ref(false)
const logPagination = ref({ pageNo: 1, pageSize: 10, total: 0 })

async function loadLogs() {
  if (!currentProcess.value) return
  logLoading.value = true
  try {
    const page = await getProcessChangeLogPage(currentProcess.value.id, {
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
</script>

<template>
  <div class="page">
    <PageHeader
      title="工序管理"
      description="工序主档与 SOP、不良原因维护；关键工序强制报工与人员记录，质检工序需绑定质检方案"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="240"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="CRAFT_WRITE_ROLES" type="primary" @click="dialog.open()">
          新增工序
        </PermissionButton>
      </template>
    </FilterTable>

    <!-- 新增/编辑工序 -->
    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      label-width="110px"
      @submit="dialog.handleSubmit"
    >
      <el-form-item label="工序编码" prop="processCode">
        <el-input
          v-model="dialog.model.value.processCode"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 OP-GLUE"
        />
      </el-form-item>
      <el-form-item label="工序名称" prop="processName">
        <el-input v-model="dialog.model.value.processName" maxlength="64" />
      </el-form-item>
      <el-form-item label="工序类型" prop="processType">
        <el-select v-model="dialog.model.value.processType">
          <el-option
            v-for="opt in PROCESS_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标准工时（秒）" prop="standardTimeSeconds">
        <el-input-number
          v-model="dialog.model.value.standardTimeSeconds"
          :min="1"
          :max="86400"
          controls-position="right"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="关键工序" prop="keyProcess">
        <el-switch v-model="dialog.model.value.keyProcess" />
        <span class="form-tip">关键工序强制报工并记录作业人员</span>
      </el-form-item>
      <el-form-item label="需要质检" prop="qualityRequired">
        <el-switch v-model="dialog.model.value.qualityRequired" />
      </el-form-item>
      <el-form-item
        v-if="dialog.model.value.qualityRequired"
        label="质检方案 ID"
        prop="qualityPlanId"
      >
        <el-input-number
          v-model="dialog.model.value.qualityPlanId"
          :min="1"
          controls-position="right"
          class="full-width"
          placeholder="质量模块的质检方案主键"
        />
      </el-form-item>
      <el-form-item label="需要扫码" prop="scanRequired">
        <el-switch v-model="dialog.model.value.scanRequired" />
      </el-form-item>
      <el-form-item label="计件工序" prop="pieceRateEnabled">
        <el-switch v-model="dialog.model.value.pieceRateEnabled" />
        <span class="form-tip">开启后可配置计件单价规则</span>
      </el-form-item>
      <el-form-item label="设备类别 ID" prop="equipmentCategoryId">
        <el-input-number
          v-model="dialog.model.value.equipmentCategoryId"
          :min="1"
          controls-position="right"
          class="full-width"
          placeholder="选填，设备模块的类别主键"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="dialog.model.value.remark" type="textarea" :rows="2" maxlength="255" />
      </el-form-item>
      <el-form-item v-if="dialog.mode.value === 'edit'" label="变更原因" prop="changeReason">
        <el-input
          v-model="dialog.model.value.changeReason"
          maxlength="255"
          placeholder="选填，记入变更日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 启停（需原因） -->
    <FormDialog
      v-model:visible="statusDialog.visible.value"
      :title="statusDialogTitle"
      :model="statusDialog.model.value"
      :rules="statusRules"
      :submit-loading="statusDialog.submitLoading.value"
      width="480px"
      @submit="statusDialog.handleSubmit"
    >
      <el-alert
        v-if="statusDialog.model.value.status === 0"
        type="warning"
        :closable="false"
        show-icon
        title="停用后新工艺路线不可再引用该工序"
        class="dialog-tip"
      />
      <el-form-item label="原因" prop="reason">
        <el-input
          v-model="statusDialog.model.value.reason"
          type="textarea"
          :rows="3"
          maxlength="255"
          placeholder="必填，记入变更日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 维护抽屉：SOP / 不良原因 / 变更日志 -->
    <el-drawer
      v-model="drawerVisible"
      :title="`工序维护：${currentProcess?.processCode ?? ''} ${currentProcess?.processName ?? ''}`"
      size="720px"
      destroy-on-close
    >
      <el-tabs v-model="activeTab">
        <el-tab-pane label="SOP" name="sop">
          <div class="tab-toolbar">
            <PermissionButton
              :roles="CRAFT_WRITE_ROLES"
              type="primary"
              size="small"
              @click="sopDialog.open()"
            >
              新增 SOP
            </PermissionButton>
          </div>
          <el-table v-loading="sopLoading" :data="sops" border size="small">
            <el-table-column prop="sopCode" label="SOP 编码" width="120" />
            <el-table-column prop="sopName" label="名称" min-width="130" />
            <el-table-column prop="sopVersion" label="版本" width="80" />
            <el-table-column label="文件" width="80">
              <template #default="{ row }">
                <el-link type="primary" :href="row.fileUrl" target="_blank">查看</el-link>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <StatusTag :status="row.status" :status-map="ENABLE_STATUS_MAP" />
              </template>
            </el-table-column>
            <el-table-column v-if="canWrite" label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="sopDialog.open('edit', { ...row, changeReason: '' })"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="handleDeleteSop(row as CraftProcessSop)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="不良原因" name="reason">
          <div class="tab-toolbar">
            <PermissionButton
              :roles="CRAFT_WRITE_ROLES"
              type="primary"
              size="small"
              @click="reasonDialog.open()"
            >
              新增不良原因
            </PermissionButton>
          </div>
          <el-table v-loading="reasonLoading" :data="reasons" border size="small">
            <el-table-column prop="reasonCode" label="原因编码" width="130" />
            <el-table-column prop="reasonName" label="原因名称" min-width="150" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <StatusTag :status="row.status" :status-map="ENABLE_STATUS_MAP" />
              </template>
            </el-table-column>
            <el-table-column v-if="canWrite" label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="reasonDialog.open('edit', { ...row, changeReason: '' })"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="handleDeleteReason(row as CraftProcessDefectReason)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="变更日志" name="log">
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
                {{ PROCESS_CHANGE_TYPE_TEXT[row.changeType] ?? row.changeType }}
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
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <!-- SOP 编辑弹窗 -->
    <FormDialog
      v-model:visible="sopDialog.visible.value"
      :title="sopDialog.title.value"
      :model="sopDialog.model.value"
      :rules="sopRules"
      :submit-loading="sopDialog.submitLoading.value"
      width="520px"
      @submit="sopDialog.handleSubmit"
    >
      <el-form-item label="SOP 编码" prop="sopCode">
        <el-input
          v-model="sopDialog.model.value.sopCode"
          :disabled="sopDialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 SOP-GLUE-01"
        />
      </el-form-item>
      <el-form-item label="SOP 名称" prop="sopName">
        <el-input v-model="sopDialog.model.value.sopName" maxlength="64" />
      </el-form-item>
      <el-form-item label="SOP 版本" prop="sopVersion">
        <el-input v-model="sopDialog.model.value.sopVersion" maxlength="16" placeholder="如 V1.0" />
      </el-form-item>
      <el-form-item label="文件地址" prop="fileUrl">
        <el-input
          v-model="sopDialog.model.value.fileUrl"
          maxlength="255"
          placeholder="SOP 文档/图片/视频 URL"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="sopDialog.model.value.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="sopDialog.mode.value === 'edit'" label="变更原因" prop="changeReason">
        <el-input
          v-model="sopDialog.model.value.changeReason"
          maxlength="255"
          placeholder="选填，记入变更日志"
        />
      </el-form-item>
    </FormDialog>

    <!-- 不良原因编辑弹窗 -->
    <FormDialog
      v-model:visible="reasonDialog.visible.value"
      :title="reasonDialog.title.value"
      :model="reasonDialog.model.value"
      :rules="reasonRules"
      :submit-loading="reasonDialog.submitLoading.value"
      width="480px"
      @submit="reasonDialog.handleSubmit"
    >
      <el-form-item label="原因编码" prop="reasonCode">
        <el-input
          v-model="reasonDialog.model.value.reasonCode"
          :disabled="reasonDialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 DR-FEATHER-BROKEN"
        />
      </el-form-item>
      <el-form-item label="原因名称" prop="reasonName">
        <el-input v-model="reasonDialog.model.value.reasonName" maxlength="64" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="reasonDialog.model.value.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="reasonDialog.mode.value === 'edit'" label="变更原因" prop="changeReason">
        <el-input
          v-model="reasonDialog.model.value.changeReason"
          maxlength="255"
          placeholder="选填，记入变更日志"
        />
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

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.dialog-tip {
  margin-bottom: 16px;
}

.tab-toolbar {
  margin-bottom: 12px;
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
