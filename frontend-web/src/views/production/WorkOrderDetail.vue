<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ColumnDef, DescItem, OptionItem, RowAction, StatusNode } from '@/types/components'
import DescList from '@/components/base/DescList.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import ProTable from '@/components/base/ProTable.vue'
import StatCard from '@/components/base/StatCard.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import StatusTimeline from '@/components/business/StatusTimeline.vue'
import TabDetailPage from '@/components/business/TabDetailPage.vue'
import { useFormDialog } from '@/composables/useFormDialog'
import { useWorkOrderActions } from '@/composables/useWorkOrderActions'
import {
  KIT_ANALYZE_ROLES,
  KIT_STATUS_MAP,
  ROLE_SEED_IDS,
  SHORTAGE_HANDLE_ROLES,
  SHORTAGE_HANDLE_STATUS_MAP,
  SHORTAGE_HANDLE_TYPE_OPTIONS,
  SHORTAGE_HANDLE_TYPE_TEXT,
  WORK_ORDER_CHANGE_TYPE_TEXT,
  WORK_ORDER_SOURCE_TEXT,
  WORK_ORDER_STATUS_MAP,
  WO_EXEC_ROLES,
  WO_PLAN_ROLES,
} from '@/constants/production'
import { loadRoleUserOptions } from '@/api/production/options'
import {
  getWorkOrder,
  getWorkOrderMaterials,
  getWorkOrderStatusLogs,
} from '@/api/production/workOrder'
import type {
  WorkOrder,
  WorkOrderMaterial,
  WorkOrderStatusLog,
} from '@/api/production/workOrder'
import {
  analyzeWorkOrder,
  createShortageHandle,
  getKitResult,
  getShortageHandles,
  resolveShortageHandle,
} from '@/api/production/kit'
import type { KitAnalysisRow, ShortageHandle } from '@/api/production/kit'
import { getDispatchPage } from '@/api/production/dispatch'
import type { DispatchOrder } from '@/api/production/dispatch'
import { DISPATCH_STATUS_MAP } from '@/constants/production'

defineOptions({ name: 'WorkOrderDetail' })

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

// ---------- 工单主档 ----------

const order = ref<WorkOrder>()
const loading = ref(false)

async function loadOrder() {
  loading.value = true
  try {
    order.value = await getWorkOrder(orderId)
  } finally {
    loading.value = false
  }
}

const descItems: DescItem<WorkOrder>[] = [
  { prop: 'workOrderNo', label: '工单号' },
  { prop: 'sourceType', label: '来源', formatter: (row) => WORK_ORDER_SOURCE_TEXT[row.sourceType] ?? String(row.sourceType) },
  { prop: 'productName', label: '产品' },
  { prop: 'spec', label: '规格型号' },
  { prop: 'batchNo', label: '批次号' },
  { prop: 'priority', label: '优先级', formatter: (row) => (row.priority == null ? '—' : `${row.priority}（1 最高 - 9 最低）`) },
  { prop: 'overRatio', label: '超产比例', formatter: (row) => (row.overRatio == null ? '—' : `${row.overRatio}%`) },
  { prop: 'planStartTime', label: '计划开始' },
  { prop: 'planEndTime', label: '计划完成' },
  { prop: 'kitStatus', label: '齐套状态', statusMap: KIT_STATUS_MAP },
  { prop: 'createTime', label: '创建时间' },
  { prop: 'updateTime', label: '更新时间' },
]

const progressPercent = computed(() => {
  if (!order.value || order.value.planQuantity === 0) return 0
  return Math.min(100, Math.round((order.value.finishQuantity / order.value.planQuantity) * 100))
})

// ---------- 状态操作 ----------

const actions = useWorkOrderActions(async () => {
  await loadOrder()
  await loadLogs()
})

const status = computed(() => order.value?.orderStatus)

// ---------- 物料需求 ----------

const materials = ref<WorkOrderMaterial[]>([])
const materialsLoading = ref(false)

const materialColumns: ColumnDef<WorkOrderMaterial>[] = [
  { prop: 'materialCode', label: '物料编码', width: 150 },
  { prop: 'materialName', label: '物料名称', minWidth: 160 },
  { prop: 'requireQuantity', label: '需求数量', width: 120, align: 'right' },
  { prop: 'issuedQuantity', label: '已发数量', width: 120, align: 'right' },
]

async function loadMaterials() {
  materialsLoading.value = true
  try {
    materials.value = await getWorkOrderMaterials(orderId)
  } finally {
    materialsLoading.value = false
  }
}

// ---------- 齐套分析 ----------

const kitRows = ref<KitAnalysisRow[]>([])
const kitLoading = ref(false)
const analyzing = ref(false)

const kitColumns: ColumnDef<KitAnalysisRow>[] = [
  { prop: 'materialCode', label: '物料编码', width: 150 },
  { prop: 'materialName', label: '物料名称', minWidth: 150 },
  { prop: 'requireQuantity', label: '需求', width: 100, align: 'right' },
  { prop: 'availableQuantity', label: '可用库存', width: 110, align: 'right' },
  { prop: 'transitQuantity', label: '在途', width: 100, align: 'right' },
  { prop: 'shortageQuantity', label: '欠料', width: 100, align: 'right' },
  { prop: 'kitStatus', label: '状态', width: 96, statusMap: KIT_STATUS_MAP },
  { prop: 'analysisTime', label: '分析时间', width: 170 },
]

async function loadKit() {
  kitLoading.value = true
  try {
    kitRows.value = await getKitResult(orderId)
  } finally {
    kitLoading.value = false
  }
}

async function handleAnalyze() {
  analyzing.value = true
  try {
    await analyzeWorkOrder(orderId)
    ElMessage.success('齐套分析完成')
    await Promise.all([loadKit(), loadOrder()])
  } catch {
    // 未下达等业务错误由拦截器提示
  } finally {
    analyzing.value = false
  }
}

// ---------- 欠料处理记录 ----------

const handles = ref<ShortageHandle[]>([])
const handlesLoading = ref(false)
const handlerOptions = ref<OptionItem[]>([])

const handleColumns: ColumnDef<ShortageHandle>[] = [
  { prop: 'materialCode', label: '物料编码', width: 140 },
  { prop: 'materialName', label: '物料名称', minWidth: 140 },
  { prop: 'handleType', label: '处理方式', width: 100, formatter: (row) => SHORTAGE_HANDLE_TYPE_TEXT[row.handleType] ?? String(row.handleType) },
  { prop: 'expectedArrivalDate', label: '预计到料', width: 110 },
  { prop: 'handleRemark', label: '处理说明', minWidth: 140 },
  { prop: 'handleStatus', label: '状态', width: 90, statusMap: SHORTAGE_HANDLE_STATUS_MAP },
  { prop: 'createTime', label: '登记时间', width: 170 },
]

const handleRowActions: RowAction<ShortageHandle>[] = [
  {
    key: 'resolve',
    label: '标记解决',
    type: 'success',
    roles: SHORTAGE_HANDLE_ROLES,
    confirm: '确认该欠料已解决？',
    show: (row) => row.handleStatus === 0,
  },
]

async function loadHandles() {
  handlesLoading.value = true
  try {
    handles.value = await getShortageHandles(orderId)
  } finally {
    handlesLoading.value = false
  }
}

async function handleHandleAction(key: string, row: ShortageHandle) {
  if (key !== 'resolve') return
  try {
    await resolveShortageHandle(row.id)
    ElMessage.success('已标记解决')
  } finally {
    await loadHandles()
  }
}

/** 欠料行 → 登记弹窗的物料候选 */
const shortageMaterialOptions = computed<OptionItem[]>(() =>
  kitRows.value
    .filter((row) => row.shortageQuantity > 0)
    .map((row) => ({
      label: `${row.materialCode} ${row.materialName}（欠 ${row.shortageQuantity}）`,
      value: row.materialId,
    })),
)

interface HandleForm {
  materialId: number | null
  handleType: number
  handlerId: number | null
  expectedArrivalDate: string
  handleRemark: string
}

const handleDialog = useFormDialog<HandleForm>(
  () => ({
    materialId: null,
    handleType: 1,
    handlerId: null,
    expectedArrivalDate: '',
    handleRemark: '',
  }),
  {
    titles: { create: '登记欠料处理' },
    submit: async (model) => {
      await createShortageHandle({
        workOrderId: orderId,
        materialId: model.materialId!,
        handleType: model.handleType,
        handlerId: model.handlerId!,
        expectedArrivalDate: model.expectedArrivalDate || undefined,
        handleRemark: model.handleRemark || undefined,
      })
      ElMessage.success('处理记录已登记')
    },
    onSuccess: () => void loadHandles(),
  },
)

const handleRules = {
  materialId: [{ required: true, message: '请选择欠料物料', trigger: 'change' }],
  handleType: [{ required: true, message: '请选择处理方式', trigger: 'change' }],
  handlerId: [{ required: true, message: '请选择责任人', trigger: 'change' }],
}

async function openHandleDialog() {
  if (shortageMaterialOptions.value.length === 0) {
    ElMessage.info('当前分析结果无欠料物料，请先执行齐套分析')
    return
  }
  if (handlerOptions.value.length === 0) {
    try {
      handlerOptions.value = await loadRoleUserOptions(ROLE_SEED_IDS.PMC)
    } catch {
      // 责任人选项加载失败不阻塞弹窗
    }
  }
  handleDialog.open('create', { materialId: shortageMaterialOptions.value[0].value as number })
}

// ---------- 派工单 ----------

const dispatches = ref<DispatchOrder[]>([])
const dispatchesLoading = ref(false)

const dispatchColumns: ColumnDef<DispatchOrder>[] = [
  { prop: 'dispatchNo', label: '派工单号', width: 170 },
  { prop: 'lineName', label: '产线', minWidth: 120 },
  { prop: 'shiftName', label: '班次', width: 80 },
  { prop: 'planDate', label: '排产日期', width: 110 },
  { prop: 'planQuantity', label: '计划数', width: 90, align: 'right' },
  { prop: 'planStartTime', label: '计划开始', width: 160 },
  { prop: 'planEndTime', label: '计划结束', width: 160 },
  { prop: 'dispatchStatus', label: '状态', width: 90, statusMap: DISPATCH_STATUS_MAP },
]

async function loadDispatches() {
  dispatchesLoading.value = true
  try {
    const page = await getDispatchPage({ workOrderId: orderId, pageNo: 1, pageSize: 100 })
    dispatches.value = page.list
  } finally {
    dispatchesLoading.value = false
  }
}

// ---------- 状态日志 ----------

const logs = ref<WorkOrderStatusLog[]>([])
const logsLoading = ref(false)

const logNodes = computed<StatusNode[]>(() =>
  logs.value.map((log) => ({
    status: String(log.toStatus),
    time: log.operateTime,
    operator: `操作人 #${log.operateBy}`,
    remark: [
      WORK_ORDER_CHANGE_TYPE_TEXT[log.changeType],
      log.fromStatus != null
        ? `${WORK_ORDER_STATUS_MAP[log.fromStatus]?.text ?? log.fromStatus} → ${WORK_ORDER_STATUS_MAP[log.toStatus]?.text ?? log.toStatus}`
        : undefined,
      log.changeReason ?? undefined,
    ]
      .filter(Boolean)
      .join('；'),
  })),
)

async function loadLogs() {
  logsLoading.value = true
  try {
    logs.value = await getWorkOrderStatusLogs(orderId)
  } finally {
    logsLoading.value = false
  }
}

// ---------- Tab 懒加载 ----------

const tabs = [
  { name: 'materials', label: '物料需求' },
  { name: 'kit', label: '齐套分析' },
  { name: 'dispatch', label: '派工单' },
  { name: 'logs', label: '状态日志' },
]

const loadedTabs = new Set<string>()

function handleTabChange(name: string) {
  if (loadedTabs.has(name)) return
  loadedTabs.add(name)
  if (name === 'materials') void loadMaterials()
  else if (name === 'kit') {
    void loadKit()
    void loadHandles()
  } else if (name === 'dispatch') void loadDispatches()
  else if (name === 'logs') void loadLogs()
}

onMounted(() => {
  if (!Number.isInteger(orderId) || orderId <= 0) {
    ElMessage.error('无效的工单 id')
    router.replace('/production/work-orders')
    return
  }
  void loadOrder()
  handleTabChange('materials')
})
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader :title="order ? `工单 ${order.workOrderNo}` : '工单详情'" show-back>
      <template #extra>
        <div v-if="order" class="header-actions">
          <StatusTag :status="order.orderStatus" :status-map="WORK_ORDER_STATUS_MAP" />
          <PermissionButton
            v-if="status === 0"
            :roles="WO_PLAN_ROLES"
            type="success"
            @click="actions.release(orderId)"
          >
            下达
          </PermissionButton>
          <PermissionButton
            v-if="status === 1 || status === 2"
            :roles="WO_EXEC_ROLES"
            type="warning"
            @click="actions.pause(orderId)"
          >
            暂停
          </PermissionButton>
          <PermissionButton
            v-if="status === 3"
            :roles="WO_EXEC_ROLES"
            type="success"
            @click="actions.resume(orderId)"
          >
            恢复
          </PermissionButton>
          <PermissionButton
            v-if="status === 1 || status === 2"
            :roles="WO_EXEC_ROLES"
            type="primary"
            @click="actions.finish(orderId)"
          >
            完工
          </PermissionButton>
          <PermissionButton
            v-if="status === 4"
            :roles="WO_PLAN_ROLES"
            @click="actions.close(orderId)"
          >
            关闭
          </PermissionButton>
          <PermissionButton
            v-if="status === 0 || status === 1"
            :roles="WO_PLAN_ROLES"
            type="danger"
            plain
            @click="actions.cancel(orderId)"
          >
            作废
          </PermissionButton>
        </div>
      </template>
    </PageHeader>

    <el-row v-if="order" :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never"><StatCard label="计划数量" :value="order.planQuantity" unit="个" /></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never"><StatCard label="已派数量" :value="order.dispatchedQuantity" unit="个" /></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never"><StatCard label="投入数量" :value="order.inputQuantity" unit="个" /></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never"><StatCard label="完工数量" :value="order.finishQuantity" unit="个" /></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never"><StatCard label="不良数量" :value="order.defectQuantity" unit="个" /></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never">
          <div class="progress-card">
            <div class="progress-card__label">完工进度</div>
            <el-progress type="circle" :percentage="progressPercent" :width="64" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="order" shadow="never" class="section">
      <DescList :items="descItems" :data="order" :column="3" title="基本信息" />
    </el-card>

    <el-card shadow="never" class="section">
      <TabDetailPage :tabs="tabs" @tab-change="handleTabChange">
        <template #tab-materials>
          <el-alert
            v-if="order && order.orderStatus === 0"
            type="info"
            :closable="false"
            show-icon
            title="工单下达后按生效 BOM 生成物料需求"
            class="tab-alert"
          />
          <ProTable :columns="materialColumns" :data="materials" :loading="materialsLoading" />
        </template>

        <template #tab-kit>
          <div class="tab-toolbar">
            <PermissionButton
              :roles="KIT_ANALYZE_ROLES"
              type="primary"
              :loading="analyzing"
              @click="handleAnalyze"
            >
              执行齐套分析
            </PermissionButton>
            <PermissionButton :roles="SHORTAGE_HANDLE_ROLES" @click="openHandleDialog">
              登记欠料处理
            </PermissionButton>
          </div>
          <ProTable :columns="kitColumns" :data="kitRows" :loading="kitLoading" />
          <el-divider content-position="left">欠料处理记录</el-divider>
          <ProTable
            :columns="handleColumns"
            :data="handles"
            :loading="handlesLoading"
            :row-actions="handleRowActions"
            :action-width="110"
            @row-action="handleHandleAction"
          />
        </template>

        <template #tab-dispatch>
          <div class="tab-toolbar">
            <el-button type="primary" plain @click="router.push('/production/dispatch-orders')">
              前往派工管理
            </el-button>
          </div>
          <ProTable :columns="dispatchColumns" :data="dispatches" :loading="dispatchesLoading" />
        </template>

        <template #tab-logs>
          <div v-loading="logsLoading" class="logs-wrap">
            <StatusTimeline
              v-if="logNodes.length"
              :nodes="logNodes"
              :status-map="WORK_ORDER_STATUS_MAP"
            />
            <el-empty v-else description="暂无状态日志" />
          </div>
        </template>
      </TabDetailPage>
    </el-card>

    <FormDialog
      v-model:visible="handleDialog.visible.value"
      :title="handleDialog.title.value"
      :model="handleDialog.model.value"
      :rules="handleRules"
      :submit-loading="handleDialog.submitLoading.value"
      width="520px"
      @submit="handleDialog.handleSubmit"
    >
      <el-form-item label="欠料物料" prop="materialId">
        <el-select v-model="handleDialog.model.value.materialId" filterable>
          <el-option
            v-for="opt in shortageMaterialOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="处理方式" prop="handleType">
        <el-radio-group v-model="handleDialog.model.value.handleType">
          <el-radio v-for="opt in SHORTAGE_HANDLE_TYPE_OPTIONS" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="责任人" prop="handlerId">
        <el-select
          v-model="handleDialog.model.value.handlerId"
          filterable
          placeholder="PMC 计划员"
        >
          <el-option
            v-for="opt in handlerOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="预计到料" prop="expectedArrivalDate">
        <el-date-picker
          v-model="handleDialog.model.value.expectedArrivalDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选填"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="处理说明" prop="handleRemark">
        <el-input
          v-model="handleDialog.model.value.handleRemark"
          type="textarea"
          :rows="2"
          maxlength="255"
          show-word-limit
          placeholder="选填"
        />
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-row .el-card {
  height: 100%;
}

.progress-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.progress-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.section {
  margin-bottom: 16px;
}

.tab-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.tab-alert {
  margin-bottom: 12px;
}

.logs-wrap {
  min-height: 120px;
  padding: 8px 4px;
}

.full-width {
  width: 100%;
}
</style>
