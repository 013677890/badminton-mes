<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  AxisChartData,
  ColumnDef,
  FilterField,
  GanttRow,
  GanttTask,
  PieDataItem,
  StatusMap,
  TraceNode,
} from '@/types/components'
import type { ECOption } from '@/utils/echarts'
import type { PageParam, PageResult } from '@/utils/request'
import PageHeader from '@/components/base/PageHeader.vue'
import LineChartCard from '@/components/base/charts/LineChartCard.vue'
import BarChartCard from '@/components/base/charts/BarChartCard.vue'
import PieChartCard from '@/components/base/charts/PieChartCard.vue'
import GaugeCard from '@/components/base/charts/GaugeCard.vue'
import QueryChartPanel from '@/components/business/QueryChartPanel.vue'
import GanttSchedule from '@/components/business/GanttSchedule.vue'
import TraceLinkGraph from '@/components/business/TraceLinkGraph.vue'
import { useTable } from '@/composables/useTable'
import { formatNumber } from '@/utils/format'

defineOptions({ name: 'ChartsDemo' })

// ---------- 基础图表卡片 ----------

const DAYS = ['07-08', '07-09', '07-10', '07-11', '07-12', '07-13', '07-14']

const trendData: AxisChartData = {
  categories: DAYS,
  series: [
    { name: '计划产量', data: [4800, 4800, 5000, 5000, 4800, 5200, 5200] },
    { name: '实际产量', data: [4620, 4910, 4890, 5130, 4560, 5080, 4370] },
  ],
}

const workshopData: AxisChartData = {
  categories: ['一车间', '二车间', '三车间'],
  series: [
    { name: 'AS-05 比赛球', data: [1800, 1500, 900], stack: 'total' },
    { name: 'AS-9 训练球', data: [1200, 1600, 1100], stack: 'total' },
    { name: 'E-30 娱乐球', data: [600, 400, 1400], stack: 'total' },
  ],
}

const defectData: PieDataItem[] = [
  { name: '毛片歪斜', value: 42 },
  { name: '胶水溢出', value: 27 },
  { name: '球头开裂', value: 18 },
  { name: '重量超差', value: 13 },
  { name: '其他', value: 8 },
]

const oeeValue = ref(87.5)

// ---------- QueryChartPanel：产量报表 ----------

interface ReportRow {
  id: number
  date: string
  workshop: string
  planQty: number
  actualQty: number
  rate: number
}

const WORKSHOPS = ['一车间', '二车间', '三车间']

/** 模拟报表库：7 天 × 3 车间 */
const reportDb: ReportRow[] = DAYS.flatMap((day, dayIndex) =>
  WORKSHOPS.map((workshop, wsIndex) => {
    const plan = 1500 + wsIndex * 200
    const actual = plan - 180 + ((dayIndex * 7 + wsIndex * 13) % 260)
    return {
      id: dayIndex * 10 + wsIndex,
      date: `2026-${day}`,
      workshop,
      planQty: plan,
      actualQty: actual,
      rate: Math.round((actual / plan) * 1000) / 10,
    }
  }),
)

interface ReportQuery {
  workshop?: string
  dateRange?: [string, string]
}

const reportChart = ref<AxisChartData>()

async function fetchReport(params: ReportQuery & PageParam): Promise<PageResult<ReportRow>> {
  await new Promise((resolve) => setTimeout(resolve, 300))
  const list = reportDb.filter((row) => {
    if (params.workshop && row.workshop !== params.workshop) return false
    if (params.dateRange && (row.date < params.dateRange[0] || row.date > params.dateRange[1])) {
      return false
    }
    return true
  })
  // 图表取全量筛选结果按日汇总（分页只影响明细表）
  const byDate = new Map<string, { plan: number; actual: number }>()
  for (const row of list) {
    const agg = byDate.get(row.date) ?? { plan: 0, actual: 0 }
    agg.plan += row.planQty
    agg.actual += row.actualQty
    byDate.set(row.date, agg)
  }
  const dates = [...byDate.keys()].sort()
  reportChart.value = {
    categories: dates,
    series: [
      { name: '计划产量', data: dates.map((date) => byDate.get(date)!.plan) },
      { name: '实际产量', data: dates.map((date) => byDate.get(date)!.actual) },
    ],
  }
  const start = (params.pageNo - 1) * params.pageSize
  return {
    list: list.slice(start, start + params.pageSize),
    total: list.length,
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  }
}

const reportFilters: FilterField[] = [
  {
    prop: 'workshop',
    label: '车间',
    type: 'select',
    options: WORKSHOPS.map((name) => ({ label: name, value: name })),
  },
  { prop: 'dateRange', label: '日期', type: 'dateRange', span: 8 },
]

const reportColumns: ColumnDef<ReportRow>[] = [
  { prop: 'date', label: '日期', width: 130 },
  { prop: 'workshop', label: '车间', width: 120 },
  {
    prop: 'planQty',
    label: '计划产量（打）',
    align: 'right',
    formatter: (row) => formatNumber(row.planQty),
  },
  {
    prop: 'actualQty',
    label: '实际产量（打）',
    align: 'right',
    formatter: (row) => formatNumber(row.actualQty),
  },
  { prop: 'rate', label: '达成率', align: 'right', formatter: (row) => `${row.rate}%` },
]

const report = useTable<ReportRow, ReportQuery>({ fetcher: fetchReport, defaultPageSize: 10 })

/** 计划柱状 + 实际折线的组合图，展示 chartOption 直配 ECOption 的方式 */
const reportOption = computed<ECOption | undefined>(() => {
  const data = reportChart.value
  if (!data) return undefined
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 8, right: 16, top: 32, bottom: 32, containLabel: true },
    xAxis: { type: 'category', data: data.categories },
    yAxis: { type: 'value', name: '打' },
    series: [
      { type: 'bar', name: '计划产量', data: data.series[0]!.data, barMaxWidth: 28 },
      { type: 'line', name: '实际产量', data: data.series[1]!.data, smooth: true },
    ],
  }
})

// ---------- GanttSchedule：派工排程 ----------

const GANTT_STATUS: StatusMap = {
  PLANNED: { type: 'info', text: '待开工' },
  RUNNING: { type: 'warning', text: '进行中' },
  DONE: { type: 'success', text: '已完工' },
}

const ganttRows: GanttRow[] = [
  { key: 'line-1', label: '插毛一线' },
  { key: 'line-2', label: '插毛二线' },
  { key: 'line-3', label: '扎线线' },
  { key: 'line-4', label: '包装线' },
]

const ganttTasks: GanttTask[] = [
  { id: 1, rowKey: 'line-1', name: 'WO20260714001 AS-05', start: '2026-07-14 08:00', end: '2026-07-14 11:30', status: 'DONE' },
  { id: 2, rowKey: 'line-1', name: 'WO20260714004 AS-40', start: '2026-07-14 12:30', end: '2026-07-14 17:00', status: 'RUNNING' },
  { id: 3, rowKey: 'line-2', name: 'WO20260714002 AS-9', start: '2026-07-14 08:30', end: '2026-07-14 14:00', status: 'RUNNING' },
  { id: 4, rowKey: 'line-2', name: 'WO20260714006 E-30', start: '2026-07-14 14:30', end: '2026-07-14 19:00', status: 'PLANNED' },
  { id: 5, rowKey: 'line-3', name: 'WO20260713005 扎线', start: '2026-07-14 09:00', end: '2026-07-14 12:00', status: 'DONE' },
  { id: 6, rowKey: 'line-3', name: 'WO20260714001 扎线', start: '2026-07-14 13:00', end: '2026-07-14 18:00', status: 'PLANNED' },
  { id: 7, rowKey: 'line-4', name: 'WO20260713002 包装', start: '2026-07-14 10:00', end: '2026-07-14 15:30', status: 'RUNNING' },
]

function handleTaskClick(task: GanttTask) {
  ElMessage.info(`点击任务：${task.name}（${task.start} ~ ${task.end}）`)
}

// ---------- TraceLinkGraph：批次追溯 ----------

const traceDirection = ref<'forward' | 'backward'>('forward')

const forwardRoot: TraceNode = {
  id: 'GM-20260703-A',
  name: '鹅毛批次 GM-0703-A',
  type: 'material',
  meta: { 供应商: '皖南羽绒', 数量: '12 万片' },
  children: [
    {
      id: 'OP-CM-01',
      name: '插毛工序 #1',
      type: 'process',
      meta: { 工单: 'WO20260714001', 操作工: '李四' },
      children: [
        {
          id: 'QC-001',
          name: '插毛首检',
          type: 'quality',
          meta: { 结果: '合格', 检验员: '王五' },
          children: [
            {
              id: 'OP-ZX-01',
              name: '扎线工序',
              type: 'process',
              meta: { 工单: 'WO20260714001' },
              children: [
                {
                  id: 'FP-0714-A',
                  name: '成品批次 FP-0714-A',
                  type: 'product',
                  meta: { 型号: 'AS-05', 数量: '450 打' },
                },
              ],
            },
          ],
        },
      ],
    },
    {
      id: 'OP-CM-02',
      name: '插毛工序 #2',
      type: 'process',
      meta: { 工单: 'WO20260714002', 操作工: '赵六' },
      children: [
        {
          id: 'FP-0714-B',
          name: '成品批次 FP-0714-B',
          type: 'product',
          meta: { 型号: 'AS-9', 数量: '380 打' },
        },
      ],
    },
  ],
}

const backwardRoot: TraceNode = {
  id: 'FP-0714-A',
  name: '成品批次 FP-0714-A',
  type: 'product',
  meta: { 型号: 'AS-05', 数量: '450 打' },
  children: [
    {
      id: 'OP-ZX-01',
      name: '扎线工序',
      type: 'process',
      meta: { 工单: 'WO20260714001' },
      children: [
        {
          id: 'GM-20260703-A',
          name: '鹅毛批次 GM-0703-A',
          type: 'material',
          meta: { 供应商: '皖南羽绒' },
        },
        {
          id: 'QT-20260701-B',
          name: '球头批次 QT-0701-B',
          type: 'material',
          meta: { 供应商: '闽南软木' },
        },
      ],
    },
  ],
}

const traceRoot = computed(() =>
  traceDirection.value === 'forward' ? forwardRoot : backwardRoot,
)

function handleNodeClick(node: TraceNode) {
  ElMessage.info(`点击节点：${node.name}（${node.id}）`)
}
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="图表组件示例"
      description="ChartWrapper 系列卡片 + QueryChartPanel 报表三段式 + GanttSchedule 排程甘特 + TraceLinkGraph 追溯链路"
    />

    <el-row :gutter="16" class="charts-demo__cards">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <LineChartCard
            title="近 7 日产量趋势"
            :data="trendData"
            unit="打"
            area
            height="280px"
          />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <BarChartCard title="今日车间产量（按产品堆叠）" :data="workshopData" unit="打" height="280px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <PieChartCard title="本周不良分布" :data="defectData" donut height="280px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <GaugeCard title="设备综合稼动率" :value="oeeValue" name="OEE" height="280px">
            <template #extra>
              <el-button size="small" @click="oeeValue = Math.round(Math.random() * 400 + 550) / 10">
                模拟刷新
              </el-button>
            </template>
          </GaugeCard>
        </el-card>
      </el-col>
    </el-row>

    <el-divider content-position="left">QueryChartPanel — 查询 + 图表 + 明细三段式报表</el-divider>
    <QueryChartPanel
      :filter-fields="reportFilters"
      :chart-option="reportOption"
      chart-title="产量达成（计划 vs 实际）"
      :chart-loading="report.loading.value"
      :columns="reportColumns"
      :data="report.data.value"
      :loading="report.loading.value"
      :pagination="report.pagination.value"
      table-title="产量明细"
      show-index
      @query="report.query"
      @reset="report.reset"
      @page-change="report.onPageChange"
    >
      <template #col-rate="{ row }">
        <span :class="row.rate >= 100 ? 'charts-demo__rate--ok' : 'charts-demo__rate--low'">
          {{ row.rate }}%
        </span>
      </template>
    </QueryChartPanel>

    <el-divider content-position="left">GanttSchedule — 派工排程甘特（点击任务条）</el-divider>
    <el-card shadow="never">
      <GanttSchedule
        :rows="ganttRows"
        :tasks="ganttTasks"
        :status-map="GANTT_STATUS"
        @task-click="handleTaskClick"
      />
    </el-card>

    <el-divider content-position="left">TraceLinkGraph — 批次追溯链路（点击节点）</el-divider>
    <el-card shadow="never">
      <el-radio-group v-model="traceDirection" class="charts-demo__trace-switch">
        <el-radio-button value="forward">正向追溯</el-radio-button>
        <el-radio-button value="backward">反向追溯</el-radio-button>
      </el-radio-group>
      <TraceLinkGraph
        :root="traceRoot"
        :direction="traceDirection"
        @node-click="handleNodeClick"
      />
    </el-card>
  </div>
</template>

<style scoped>
.charts-demo__cards :deep(.el-card) {
  margin-bottom: 16px;
}

.charts-demo__rate--ok {
  color: var(--el-color-success);
}

.charts-demo__rate--low {
  color: var(--el-color-danger);
}

.charts-demo__trace-switch {
  margin-bottom: 12px;
}
</style>
