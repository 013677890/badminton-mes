<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import ProTable from '@/components/base/ProTable.vue'
import DescList from '@/components/base/DescList.vue'
import ApprovalActionBar from '@/components/business/ApprovalActionBar.vue'
import BatchToolbar from '@/components/business/BatchToolbar.vue'
import ConfigForm from '@/components/business/ConfigForm.vue'
import ImportExport from '@/components/business/ImportExport.vue'
import StatusCardGrid from '@/components/business/StatusCardGrid.vue'
import StatusTimeline from '@/components/business/StatusTimeline.vue'
import TabDetailPage from '@/components/business/TabDetailPage.vue'
import TreeManager from '@/components/business/TreeManager.vue'
import type {
  BatchAction,
  ColumnDef,
  ConfigChangeLog,
  ConfigGroup,
  DescItem,
  ImportResult,
  StatusCardItem,
  StatusMap,
  StatusNode,
  TabItem,
  TreeNodeData,
  TreeOperation,
} from '@/types/components'

defineOptions({ name: 'BusinessDemo' })

// ---------- StatusTimeline：工单状态流转 ----------

const orderStatusMap: StatusMap = {
  CREATED: { type: 'info', text: '已创建' },
  APPROVED: { type: 'primary', text: '已审核' },
  STARTED: { type: 'warning', text: '生产中' },
  REPORTED: { type: 'success', text: '已报工' },
  FINISHED: { type: 'success', text: '已完工' },
}

const timelineNodes: StatusNode[] = [
  {
    status: 'CREATED',
    time: '2026-07-10 08:30:12',
    operator: '张计划',
    remark: '工单 MO-20260710-001 创建，计划数量 1200 打',
  },
  { status: 'APPROVED', time: '2026-07-10 09:02:45', operator: '李主管' },
  { status: 'STARTED', time: '2026-07-10 09:30:00', operator: '王班长', remark: '一号产线开工' },
  {
    status: 'REPORTED',
    time: '2026-07-11 17:20:31',
    operator: '赵操作员',
    remark: '累计报工 600 打，不良 3 打',
  },
  { status: 'FINISHED', time: '2026-07-12 16:45:08', operator: '王班长', remark: '完工 1198 打' },
]

// ---------- ApprovalActionBar ----------

function handleApprove(comment?: string) {
  ElMessage.success(comment ? `审核通过，意见：${comment}` : '审核通过')
}

function handleReject(reason: string) {
  ElMessage.warning(`已驳回，原因：${reason}`)
}

// ---------- TreeManager：设备类别树 ----------

const deviceTree: TreeNodeData[] = [
  {
    id: 1,
    label: '生产设备',
    children: [
      {
        id: 11,
        label: '注塑机',
        children: [
          { id: 111, label: '80T 注塑机' },
          { id: 112, label: '120T 注塑机', disabled: true },
        ],
      },
      { id: 12, label: '植毛机', children: [{ id: 121, label: '16 站植毛机' }] },
    ],
  },
  {
    id: 2,
    label: '检测设备',
    children: [{ id: 21, label: '动态稳定测试机' }],
  },
]

const treeOpText: Record<TreeOperation, string> = {
  add: '新增',
  edit: '编辑',
  disable: '停用/启用',
  delete: '删除',
}

const selectedTreeLabel = ref('-')

function handleTreeOperate(op: TreeOperation, node: TreeNodeData | null) {
  ElMessage.info(`节点操作：${treeOpText[op]} → ${node ? node.label : '根级'}`)
}

// ---------- StatusCardGrid：产线实时状态 ----------

const lineStatusMap: StatusMap = {
  RUNNING: { type: 'success', text: '运行中' },
  IDLE: { type: 'info', text: '待机' },
  FAULT: { type: 'danger', text: '故障' },
}

function buildLineCard(
  key: string,
  title: string,
  subtitle: string,
  status: string,
  plan: number,
  done: number,
): StatusCardItem {
  return {
    key,
    title,
    subtitle,
    status,
    metrics: [
      { label: '计划', value: plan, unit: '打' },
      { label: '完成', value: done, unit: '打' },
      { label: '达成率', value: plan > 0 ? Math.round((done / plan) * 100) : 0, unit: '%' },
    ],
  }
}

const lineCards: StatusCardItem[] = [
  buildLineCard('L1', '一号产线', '球头注塑', 'RUNNING', 1200, 860),
  buildLineCard('L2', '二号产线', '羽毛植毛', 'RUNNING', 1000, 720),
  buildLineCard('L3', '三号产线', '整球胶合', 'FAULT', 900, 310),
  buildLineCard('L4', '四号产线', '试打检验', 'RUNNING', 800, 655),
  buildLineCard('L5', '五号产线', '包装装箱', 'IDLE', 0, 0),
  buildLineCard('L6', '六号产线', '备用产线', 'IDLE', 0, 0),
]

function handleCardClick(card: StatusCardItem) {
  ElMessage.info(`点击卡片：${card.title}`)
}

// ---------- BatchToolbar + ProTable 联动 ----------

interface OrderRow {
  id: number
  orderNo: string
  product: string
  qty: number
  status: string
}

const orderRows: OrderRow[] = [
  { id: 1, orderNo: 'MO-20260713-001', product: 'A900 比赛级', qty: 1200, status: 'CREATED' },
  { id: 2, orderNo: 'MO-20260713-002', product: 'A700 训练级', qty: 800, status: 'APPROVED' },
  { id: 3, orderNo: 'MO-20260713-003', product: 'A500 娱乐级', qty: 1500, status: 'CREATED' },
  { id: 4, orderNo: 'MO-20260714-001', product: 'A900 比赛级', qty: 600, status: 'STARTED' },
]

const orderColumns: ColumnDef<OrderRow>[] = [
  { prop: 'orderNo', label: '工单号', width: 170 },
  { prop: 'product', label: '产品', minWidth: 140 },
  { prop: 'qty', label: '数量（打）', width: 110, align: 'right' },
  { prop: 'status', label: '状态', width: 100, align: 'center', statusMap: orderStatusMap },
]

const batchActions: BatchAction[] = [
  { key: 'issue', label: '批量下达', type: 'primary' },
  { key: 'delete', label: '批量删除', type: 'danger', confirm: '确认删除选中的工单？删除后不可恢复' },
]

const batchSelection = ref<OrderRow[]>([])
// ProTable 未暴露 clearSelection，靠重建表格清空勾选
const orderTableKey = ref(0)

function handleOrderSelection(rows: OrderRow[]) {
  batchSelection.value = rows
}

function clearBatchSelection() {
  batchSelection.value = []
  orderTableKey.value += 1
}

function handleBatchAction(key: string, selection: Record<string, any>[]) {
  ElMessage.success(`批量操作 [${key}] 处理 ${selection.length} 条工单`)
  clearBatchSelection()
}

// ---------- ConfigForm：现场参数配置 ----------

const configGroups: ConfigGroup[] = [
  {
    title: '现场参数',
    items: [
      {
        key: 'workshopName',
        label: '车间名称',
        type: 'input',
        required: true,
        tip: '用于报表与看板展示',
      },
      { key: 'shiftHours', label: '班次时长（小时）', type: 'number', required: true },
      {
        key: 'autoDispatch',
        label: '自动派工',
        type: 'switch',
        tip: '开启后任务单审核通过自动生成派工单',
      },
    ],
  },
  {
    title: '条码规则',
    items: [
      { key: 'barcodePrefix', label: '条码前缀', type: 'input', required: true },
      {
        key: 'codeMode',
        label: '编码方式',
        type: 'select',
        required: true,
        options: [
          { label: '流水号', value: 'SEQ' },
          { label: '日期 + 流水号', value: 'DATE_SEQ' },
        ],
      },
      { key: 'remark', label: '备注', type: 'textarea', span: 24 },
    ],
  },
]

const defaultConfig: Record<string, any> = {
  workshopName: '一车间',
  shiftHours: 8,
  autoDispatch: true,
  barcodePrefix: 'YMQ',
  codeMode: 'DATE_SEQ',
  remark: '',
}

const configModel = ref<Record<string, any>>({ ...defaultConfig })
const configSaving = ref(false)

const configLogs: ConfigChangeLog[] = [
  { time: '2026-07-12 10:21:00', operator: '李主管', content: '班次时长 12 → 8' },
  { time: '2026-07-08 15:40:12', operator: '系统管理员', content: '开启自动派工' },
  { time: '2026-07-01 09:00:00', operator: '系统管理员', content: '初始化条码前缀 YMQ' },
]

function handleConfigSave(model: Record<string, any>) {
  configSaving.value = true
  setTimeout(() => {
    configSaving.value = false
    ElMessage.success(`保存成功：${JSON.stringify(model)}`)
  }, 500)
}

function handleConfigReset() {
  configModel.value = { ...defaultConfig }
  ElMessage.info('已重置为初始配置')
}

// ---------- TabDetailPage：产品追溯详情 ----------

const traceTabs: TabItem[] = [
  { name: 'base', label: '基础信息' },
  { name: 'process', label: '工序记录' },
  { name: 'quality', label: '质检记录', lazy: false },
]

const activeTab = ref<string>()

const traceDescItems: DescItem[] = [
  { prop: 'barcode', label: '产品条码' },
  { prop: 'model', label: '产品型号' },
  { prop: 'batch', label: '生产批次' },
  { prop: 'operator', label: '操作人电话', mask: true },
]

const traceData: Record<string, any> = {
  barcode: 'YMQ20260712000186',
  model: 'A900 比赛级',
  batch: 'B20260712-01',
  operator: '13812345678',
}

function handleTabChange(name: string) {
  const label = traceTabs.find((tab) => tab.name === name)?.label ?? name
  ElMessage.info(`切换到「${label}」`)
}

// ---------- ImportExport：工单导入 / 报表导出 ----------

const importing = ref(false)
const exporting = ref(false)
const importResult = ref<ImportResult | null>(null)

function handleImport(file: File) {
  importing.value = true
  importResult.value = null
  // 模拟后端导入耗时与回执
  setTimeout(() => {
    importing.value = false
    importResult.value = {
      successCount: 18,
      failCount: 2,
      errors: [
        { row: 3, message: '产品编码不存在：P-XX-01' },
        { row: 7, message: '计划数量必须为正整数' },
      ],
    }
    ElMessage.success(`文件「${file.name}」导入完成`)
  }, 800)
}

function handleExport() {
  exporting.value = true
  setTimeout(() => {
    exporting.value = false
    ElMessage.success('报表导出成功（模拟）')
  }, 800)
}
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="业务通用组件"
      description="StatusTimeline / ApprovalActionBar / TreeManager / StatusCardGrid / BatchToolbar / ConfigForm / TabDetailPage / ImportExport 可交互演示"
    />

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="business-demo__col-card">
          <template #header>StatusTimeline 状态时间轴 —— 工单流转履历</template>
          <StatusTimeline :nodes="timelineNodes" :status-map="orderStatusMap" reverse />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="business-demo__col-card">
          <template #header>
            TreeManager 树管理 —— 设备类别（当前选中：{{ selectedTreeLabel }}）
          </template>
          <TreeManager
            :data="deviceTree"
            title="设备类别"
            @node-click="selectedTreeLabel = $event.label"
            @node-operate="handleTreeOperate"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>ApprovalActionBar 审批操作栏 —— 驳回必填意见，通过可选意见</template>
      <ApprovalActionBar
        approve-comment-enabled
        @approve="handleApprove"
        @reject="handleReject"
      >
        <template #extra>
          <el-button plain @click="ElMessage.info('左侧附加按钮（#extra 插槽）')">
            导出审批单
          </el-button>
        </template>
      </ApprovalActionBar>
    </el-card>

    <el-card shadow="never">
      <template #header>StatusCardGrid 状态卡片网格 —— 产线实时状态（6 卡 3 态）</template>
      <StatusCardGrid
        :cards="lineCards"
        :status-map="lineStatusMap"
        :columns="3"
        @card-click="handleCardClick"
      />
    </el-card>

    <el-card shadow="never">
      <template #header>BatchToolbar 批量操作 —— 与 ProTable 勾选联动</template>
      <BatchToolbar
        :selection="batchSelection"
        :actions="batchActions"
        class="business-demo__batch"
        @batch-action="handleBatchAction"
        @clear="clearBatchSelection"
      />
      <ProTable
        :key="orderTableKey"
        :columns="orderColumns"
        :data="orderRows"
        selectable
        @selection-change="handleOrderSelection"
      />
    </el-card>

    <el-card shadow="never">
      <template #header>ConfigForm 配置表单 —— 分组 + 必填校验 + 变更日志</template>
      <ConfigForm
        v-model="configModel"
        :groups="configGroups"
        :logs="configLogs"
        :save-loading="configSaving"
        @save="handleConfigSave"
        @reset="handleConfigReset"
      />
    </el-card>

    <el-card shadow="never">
      <template #header>TabDetailPage 详情页壳 —— 懒加载 Tab（当前：{{ activeTab }}）</template>
      <TabDetailPage v-model:active="activeTab" :tabs="traceTabs" @tab-change="handleTabChange">
        <template #tab-base>
          <DescList :items="traceDescItems" :data="traceData" />
        </template>
        <template #tab-process>
          <p class="business-demo__tab-text">
            工序记录（lazy 默认 true）：首次切换到本 Tab 才渲染，之后保持挂载不丢状态。
          </p>
        </template>
        <template #tab-quality>
          <p class="business-demo__tab-text">
            质检记录（lazy=false）：页面加载即渲染，适合首屏必看内容。
          </p>
        </template>
      </TabDetailPage>
    </el-card>

    <el-card shadow="never">
      <template #header>ImportExport 导入导出 —— 模拟导入回执（成功 18 / 失败 2）</template>
      <ImportExport
        template-url="about:blank"
        :importing="importing"
        :exporting="exporting"
        :import-result="importResult"
        @import="handleImport"
        @export="handleExport"
        @template-download="ElMessage.info('触发模板下载')"
      />
    </el-card>
  </div>
</template>

<style scoped>
.business-demo__col-card {
  height: 100%;
}

.business-demo__batch {
  margin-bottom: 12px;
}

.business-demo__tab-text {
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--el-text-color-regular);
}
</style>
