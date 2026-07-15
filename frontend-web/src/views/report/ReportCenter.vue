<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import type { OptionItem } from '@/types/components'
import { loadLineOptions, loadProductOptions, loadWorkshopOptions } from '@/api/production/options'
import { REPORT_EXPORT_ROLES } from '@/constants/report'
import { SCENE_TASK_STATUS_MAP } from '@/constants/scene'
import {
  exportDefectReport,
  exportProductionReport,
  getDefectPage,
  getDefectSummary,
  getKanbanSnapshot,
  getProductionDetails,
  getProductionSummary,
  getRealtimeOverview,
  getRealtimeTasks,
  getWorkshopPeriodDetails,
  getWorkshopPeriodSummary,
  traceBarcode,
  traceProduct,
} from '@/api/report'
import type { DefectDetail, DefectSummary, ProductTrace, ProductionDetail, ProductionSummary, RealtimeOverview, RealtimeTask, ReportQueryParams } from '@/api/report'

defineOptions({ name: 'ReportCenter' })

const activeTab = ref('realtime')
const workshopOptions = ref<OptionItem[]>([])
const lineOptions = ref<OptionItem[]>([])
const productOptions = ref<OptionItem[]>([])
const scopeFilters = reactive({ workshopId: undefined as number | undefined, lineId: undefined as number | undefined, productId: undefined as number | undefined })
const realtime = ref<RealtimeOverview>()
const realtimeTasks = ref<RealtimeTask[]>([])
const realtimeLoading = ref(false)

function formatDate(date: Date) { return date.toISOString().slice(0, 10) }
const today = new Date()
const weekAgo = new Date(today.getTime() - 6 * 86400000)
const reportFilters = reactive({ dateRange: [formatDate(weekAgo), formatDate(today)] as string[], workshopId: undefined as number | undefined, lineId: undefined as number | undefined, productId: undefined as number | undefined, batchNo: '' })
const reportKind = ref<'production_outputs' | 'workshop_periods'>('production_outputs')
const productionSummary = ref<ProductionSummary>()
const productionRows = ref<ProductionDetail[]>([])
const productionPage = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const productionLoading = ref(false)

const defectSummary = ref<DefectSummary>()
const defectRows = ref<DefectDetail[]>([])
const defectView = ref<'SOURCE' | 'COMPREHENSIVE'>('COMPREHENSIVE')
const defectPage = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const defectLoading = ref(false)

const traceFilters = reactive({ batchCode: '', barcodeValue: '', workOrderNo: '', taskNo: '' })
const traceResult = ref<ProductTrace>()
const traceLoading = ref(false)
const kanbanScope = ref<'central' | 'workshop' | 'line'>('central')
const kanbanId = ref<number>()
const kanbanData = ref<Record<string, any>>()

onMounted(async () => {
  const [workshops, lines, products] = await Promise.all([loadWorkshopOptions(), loadLineOptions(), loadProductOptions()])
  workshopOptions.value = workshops; lineOptions.value = lines; productOptions.value = products
  await loadRealtime()
})

function queryParams(): ReportQueryParams | undefined {
  if (reportFilters.dateRange.length !== 2) { ElMessage.warning('请选择统计时间范围'); return }
  return {
    startTime: `${reportFilters.dateRange[0]}T00:00:00`, endTime: `${reportFilters.dateRange[1]}T23:59:59`,
    workshopId: reportFilters.workshopId, lineId: reportFilters.lineId, productId: reportFilters.productId,
    batchNo: reportFilters.batchNo || undefined,
  }
}

async function loadRealtime() {
  realtimeLoading.value = true
  try { [realtime.value, realtimeTasks.value] = await Promise.all([getRealtimeOverview(scopeFilters), getRealtimeTasks(scopeFilters)]) }
  finally { realtimeLoading.value = false }
}

async function loadProduction(reset = false) {
  const params = queryParams(); if (!params) return
  if (reset) productionPage.pageNo = 1
  productionLoading.value = true
  try {
    const [summary, page] = reportKind.value === 'production_outputs'
      ? await Promise.all([getProductionSummary(params), getProductionDetails({ ...params, pageNo: productionPage.pageNo, pageSize: productionPage.pageSize })])
      : await Promise.all([getWorkshopPeriodSummary(params), getWorkshopPeriodDetails({ ...params, pageNo: productionPage.pageNo, pageSize: productionPage.pageSize })])
    productionSummary.value = summary; productionRows.value = page.list; productionPage.total = page.total
  } finally { productionLoading.value = false }
}

async function loadDefects(reset = false) {
  const params = queryParams(); if (!params) return
  if (reset) defectPage.pageNo = 1
  defectLoading.value = true
  try {
    const [summary, page] = await Promise.all([getDefectSummary(params), getDefectPage({ ...params, view: defectView.value, pageNo: defectPage.pageNo, pageSize: defectPage.pageSize })])
    defectSummary.value = summary; defectRows.value = page.list; defectPage.total = page.total
  } finally { defectLoading.value = false }
}

async function saveDownload(response: Awaited<ReturnType<typeof exportDefectReport>>, fallback: string) {
  const disposition = String(response.headers['content-disposition'] || '')
  const match = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^"';]+)/i)
  const filename = match ? decodeURIComponent(match[1]) : fallback
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a'); anchor.href = url; anchor.download = filename; anchor.click()
  URL.revokeObjectURL(url)
}

async function exportProduction() { const params = queryParams(); if (params) await saveDownload(await exportProductionReport(reportKind.value, params), '生产统计.xlsx') }
async function exportDefects() { const params = queryParams(); if (params) await saveDownload(await exportDefectReport(params), '不良统计.xlsx') }

async function runTrace() {
  if (!traceFilters.batchCode && !traceFilters.barcodeValue && !traceFilters.workOrderNo && !traceFilters.taskNo) { ElMessage.warning('请至少输入一个追溯条件'); return }
  traceLoading.value = true
  try { traceResult.value = traceFilters.barcodeValue && !traceFilters.batchCode && !traceFilters.workOrderNo && !traceFilters.taskNo ? await traceBarcode(traceFilters.barcodeValue) : await traceProduct({ ...traceFilters }) }
  finally { traceLoading.value = false }
}

async function loadKanban() {
  if (kanbanScope.value !== 'central' && !kanbanId.value) { ElMessage.warning('请输入车间或产线 ID'); return }
  kanbanData.value = await getKanbanSnapshot(kanbanScope.value, kanbanId.value)
}
</script>

<template>
  <div class="page-container">
    <PageHeader title="报表分析" description="实时生产、产出统计、不良分析、全链路追溯与看板数据" />
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="实时生产" name="realtime">
          <el-form :inline="true" :model="scopeFilters"><el-form-item label="车间"><el-select v-model="scopeFilters.workshopId" clearable><el-option v-for="item in workshopOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="产线"><el-select v-model="scopeFilters.lineId" clearable><el-option v-for="item in lineOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="产品"><el-select v-model="scopeFilters.productId" clearable><el-option v-for="item in productOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-button type="primary" @click="loadRealtime">刷新</el-button></el-form>
          <el-row v-loading="realtimeLoading" :gutter="12" class="metrics"><el-col v-for="item in [{ l: '生产中任务', v: realtime?.activeTaskCount }, { l: '暂停任务', v: realtime?.pausedTaskCount }, { l: '良品数', v: realtime?.goodQuantity }, { l: '不良数', v: realtime?.defectQuantity }, { l: '运行设备', v: realtime?.runningEquipmentCount }, { l: '未关闭安灯', v: realtime?.openAndonCount }]" :key="item.l" :xs="12" :md="4"><div class="metric"><span>{{ item.l }}</span><strong>{{ item.v ?? '-' }}</strong></div></el-col></el-row>
          <el-alert v-if="realtime?.warnings?.length" :title="realtime.warnings.join('；')" type="warning" :closable="false" />
          <el-table :data="realtimeTasks" border><el-table-column prop="taskNo" label="任务号" width="150" /><el-table-column prop="workOrderNo" label="工单号" width="150" /><el-table-column prop="productName" label="产品" min-width="150" /><el-table-column prop="lineName" label="产线" width="130" /><el-table-column prop="planQuantity" label="计划" /><el-table-column prop="goodQuantity" label="良品" /><el-table-column prop="defectQuantity" label="不良" /><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.taskStatus" :map="SCENE_TASK_STATUS_MAP" /></template></el-table-column></el-table>
        </el-tab-pane>

        <el-tab-pane label="生产统计" name="production">
          <el-form :inline="true" :model="reportFilters"><el-form-item label="统计口径"><el-radio-group v-model="reportKind"><el-radio-button value="production_outputs">产出报表</el-radio-button><el-radio-button value="workshop_periods">车间时段</el-radio-button></el-radio-group></el-form-item><el-form-item label="日期"><el-date-picker v-model="reportFilters.dateRange" type="daterange" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="车间"><el-select v-model="reportFilters.workshopId" clearable><el-option v-for="item in workshopOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="产线"><el-select v-model="reportFilters.lineId" clearable><el-option v-for="item in lineOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="loadProduction(true)">统计</el-button><PermissionButton :roles="REPORT_EXPORT_ROLES" @click="exportProduction">导出</PermissionButton></el-form-item></el-form>
          <el-descriptions v-if="productionSummary" :column="4" border class="summary"><el-descriptions-item label="计划数量">{{ productionSummary.planQuantity }}</el-descriptions-item><el-descriptions-item label="投入数量">{{ productionSummary.inputQuantity }}</el-descriptions-item><el-descriptions-item label="良品数量">{{ productionSummary.goodQuantity }}</el-descriptions-item><el-descriptions-item label="不良数量">{{ productionSummary.defectQuantity }}</el-descriptions-item><el-descriptions-item label="返工数量">{{ productionSummary.reworkQuantity }}</el-descriptions-item><el-descriptions-item label="完工数量">{{ productionSummary.finishQuantity }}</el-descriptions-item><el-descriptions-item label="完成率">{{ productionSummary.completionRate }}%</el-descriptions-item><el-descriptions-item label="不良率">{{ productionSummary.defectRate }}%</el-descriptions-item></el-descriptions>
          <el-table v-loading="productionLoading" :data="productionRows" border><el-table-column prop="reportNo" label="报工单号" width="160" /><el-table-column prop="taskNo" label="任务号" width="150" /><el-table-column prop="productName" label="产品" min-width="150" /><el-table-column prop="batchNo" label="批次" width="130" /><el-table-column prop="processName" label="工序" width="120" /><el-table-column prop="netInputQuantity" label="净投入" /><el-table-column prop="netGoodQuantity" label="净良品" /><el-table-column prop="netDefectQuantity" label="净不良" /><el-table-column prop="reportTime" label="报工时间" width="170" /></el-table>
          <el-pagination class="pagination" background layout="total, prev, pager, next" :total="productionPage.total" :current-page="productionPage.pageNo" :page-size="productionPage.pageSize" @current-change="(page: number) => { productionPage.pageNo = page; loadProduction() }" />
        </el-tab-pane>

        <el-tab-pane label="不良分析" name="defects">
          <el-form :inline="true"><el-form-item label="日期"><el-date-picker v-model="reportFilters.dateRange" type="daterange" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="视图"><el-radio-group v-model="defectView"><el-radio-button value="COMPREHENSIVE">综合去重</el-radio-button><el-radio-button value="SOURCE">来源明细</el-radio-button></el-radio-group></el-form-item><el-form-item><el-button type="primary" @click="loadDefects(true)">分析</el-button><PermissionButton :roles="REPORT_EXPORT_ROLES" @click="exportDefects">导出</PermissionButton></el-form-item></el-form>
          <el-descriptions v-if="defectSummary" :column="4" border class="summary"><el-descriptions-item label="现场不良">{{ defectSummary.sceneDefectQuantity }}</el-descriptions-item><el-descriptions-item label="质量不良">{{ defectSummary.qualityDefectQuantity }}</el-descriptions-item><el-descriptions-item label="返修复检不良">{{ defectSummary.repairRecheckDefectQuantity }}</el-descriptions-item><el-descriptions-item label="综合不良">{{ defectSummary.comprehensiveDefectQuantity }}</el-descriptions-item><el-descriptions-item label="来源记录">{{ defectSummary.sourceRecordCount }}</el-descriptions-item><el-descriptions-item label="去重数量">{{ defectSummary.mergedDuplicateCount }}</el-descriptions-item><el-descriptions-item label="现场不良率">{{ defectSummary.sceneDefectRate }}%</el-descriptions-item><el-descriptions-item label="综合不良率">{{ defectSummary.comprehensiveDefectRate }}%</el-descriptions-item></el-descriptions>
          <el-table v-loading="defectLoading" :data="defectRows" border><el-table-column prop="sourceType" label="来源" width="120" /><el-table-column prop="taskNo" label="任务号" width="140" /><el-table-column prop="productName" label="产品" min-width="150" /><el-table-column prop="batchNo" label="批次" width="130" /><el-table-column prop="processName" label="工序" width="120" /><el-table-column prop="defectName" label="不良项" min-width="130" /><el-table-column prop="netQuantity" label="净数量" /><el-table-column prop="detectedTime" label="发现时间" width="170" /></el-table>
          <el-pagination class="pagination" background layout="total, prev, pager, next" :total="defectPage.total" :current-page="defectPage.pageNo" :page-size="defectPage.pageSize" @current-change="(page: number) => { defectPage.pageNo = page; loadDefects() }" />
        </el-tab-pane>

        <el-tab-pane label="产品追溯" name="trace">
          <el-form :inline="true" :model="traceFilters"><el-form-item label="批次"><el-input v-model="traceFilters.batchCode" /></el-form-item><el-form-item label="条码"><el-input v-model="traceFilters.barcodeValue" /></el-form-item><el-form-item label="工单号"><el-input v-model="traceFilters.workOrderNo" /></el-form-item><el-form-item label="任务号"><el-input v-model="traceFilters.taskNo" /></el-form-item><el-button type="primary" :loading="traceLoading" @click="runTrace">追溯</el-button></el-form>
          <template v-if="traceResult"><el-alert :title="`数据完整性：${traceResult.dataCompleteness || '-'}${traceResult.warnings?.length ? '；' + traceResult.warnings.join('；') : ''}`" type="info" :closable="false" /><el-tabs class="trace-tabs"><el-tab-pane v-for="item in [{ n: 'barcodes', l: '条码' }, { n: 'processHistories', l: '工序履历' }, { n: 'workReports', l: '报工' }, { n: 'materials', l: '物料' }, { n: 'qualityDefects', l: '质量不良' }, { n: 'repairRecords', l: '返修' }, { n: 'equipmentStatuses', l: '设备' }, { n: 'andonExceptions', l: '安灯' }]" :key="item.n" :label="`${item.l} (${(traceResult as any)[item.n]?.length || 0})`"><pre class="json-result">{{ JSON.stringify((traceResult as any)[item.n], null, 2) }}</pre></el-tab-pane></el-tabs></template>
        </el-tab-pane>

        <el-tab-pane label="看板快照" name="kanban"><el-form :inline="true"><el-form-item label="范围"><el-radio-group v-model="kanbanScope"><el-radio-button value="central">中央</el-radio-button><el-radio-button value="workshop">车间</el-radio-button><el-radio-button value="line">产线</el-radio-button></el-radio-group></el-form-item><el-form-item v-if="kanbanScope !== 'central'" label="对象 ID"><el-input-number v-model="kanbanId" :min="1" /></el-form-item><el-button type="primary" @click="loadKanban">加载快照</el-button></el-form><pre v-if="kanbanData" class="json-result">{{ JSON.stringify(kanbanData, null, 2) }}</pre></el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.metrics { margin: 12px 0; }.metric { display: flex; flex-direction: column; gap: 8px; padding: 14px; background: var(--el-fill-color-light); border-radius: 6px; }.metric span { color: var(--el-text-color-secondary); }.metric strong { font-size: 24px; }.summary { margin: 12px 0; }.pagination { margin-top: 16px; justify-content: flex-end; }.trace-tabs { margin-top: 12px; }.json-result { max-height: 520px; overflow: auto; padding: 12px; background: var(--el-fill-color-light); white-space: pre-wrap; }
</style>
