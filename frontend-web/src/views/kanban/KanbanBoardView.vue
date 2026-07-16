<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import StatCard from '@/components/base/StatCard.vue'
import BarChartCard from '@/components/base/charts/BarChartCard.vue'
import GaugeCard from '@/components/base/charts/GaugeCard.vue'
import PieChartCard from '@/components/base/charts/PieChartCard.vue'
import {
  getCentralKanban,
  getLineKanban,
  getRealtimeTasks,
  getWorkshopKanban,
} from '@/api/report'
import type {
  KanbanSnapshot,
  RealtimeProductionOverview,
  RealtimeProductionTask,
} from '@/api/report'
import { getProfile } from '@/api/auth'
import { loadLineOptions, loadWorkshopOptions } from '@/api/production/options'
import { ADMIN_ROLE, useUserStore } from '@/stores/user'
import type { AxisChartData, OptionItem, PieDataItem } from '@/types/components'
import { formatDateTime, formatNumber } from '@/utils/format'

defineOptions({ name: 'KanbanBoardView' })

type KanbanMode = 'line' | 'workshop' | 'central'

interface LineSummary {
  key: string
  lineName: string
  taskCount: number
  planQuantity: number
  finishQuantity: number
  goodQuantity: number
  defectQuantity: number
  completionRate: number
  passRate: number
  abnormal: boolean
}

interface MetricCard {
  label: string
  value: number | string
  unit?: string
  icon: string
  color: string
}

const props = defineProps<{ mode: KanbanMode }>()
const userStore = useUserStore()

const EMPTY_OVERVIEW: RealtimeProductionOverview = {
  activeTaskCount: 0,
  pausedTaskCount: 0,
  abnormalBatchCount: 0,
  planQuantity: 0,
  inputQuantity: 0,
  goodQuantity: 0,
  defectQuantity: 0,
  equipmentTotalCount: 0,
  runningEquipmentCount: 0,
  unavailableEquipmentCount: 0,
  openAndonCount: 0,
  criticalAndonCount: 0,
  warnings: [],
}

const boardRef = ref<HTMLElement>()
const snapshot = ref<KanbanSnapshot | null>(null)
const tasks = ref<RealtimeProductionTask[]>([])
const workshopOptions = ref<OptionItem[]>([])
const lineOptions = ref<OptionItem[]>([])
const workshopId = ref<number>()
const lineId = ref<number>()
const loading = ref(false)
const optionLoading = ref(false)
const loadError = ref('')
const autoRefresh = ref(true)
const isFullscreen = ref(false)
const currentTime = ref(new Date())
const lastSuccessAt = ref<Date | null>(null)

let refreshTimer: number | undefined
let clockTimer: number | undefined

const modeConfig = computed(() => {
  const config: Record<KanbanMode, { title: string; description: string }> = {
    line: {
      title: '产线电子看板',
      description: '聚焦当前任务、工单达成、直通质量、设备及安灯异常',
    },
    workshop: {
      title: '车间电子看板',
      description: '汇总车间各产线进度、质量表现、产能和异常状态',
    },
    central: {
      title: '生产运营中控看板',
      description: '全局展示生产、质量、设备和异常核心指标',
    },
  }
  return config[props.mode]
})

const overview = computed(() => snapshot.value?.overview ?? EMPTY_OVERVIEW)
const productionTotals = computed(() =>
  tasks.value.reduce(
    (totals, task) => {
      totals.planQuantity += task.planQuantity ?? 0
      totals.finishQuantity += task.finishQuantity ?? 0
      totals.goodQuantity += task.goodQuantity ?? 0
      totals.defectQuantity += task.defectQuantity ?? 0
      return totals
    },
    { planQuantity: 0, finishQuantity: 0, goodQuantity: 0, defectQuantity: 0 },
  ),
)
const completionRate = computed(() =>
  rate(productionTotals.value.finishQuantity, productionTotals.value.planQuantity),
)
const passRate = computed(() =>
  rate(
    productionTotals.value.goodQuantity,
    productionTotals.value.goodQuantity + productionTotals.value.defectQuantity,
  ),
)
const defectRate = computed(() =>
  rate(
    productionTotals.value.defectQuantity,
    productionTotals.value.goodQuantity + productionTotals.value.defectQuantity,
  ),
)
const equipmentRunningRate = computed(() =>
  rate(overview.value.runningEquipmentCount, overview.value.equipmentTotalCount),
)

const scopeName = computed(() => {
  if (props.mode === 'central') return '全部授权范围'
  const options = props.mode === 'line' ? lineOptions.value : workshopOptions.value
  const value = props.mode === 'line' ? lineId.value : workshopId.value
  return options.find((item) => item.value === value)?.label ?? '尚未选择'
})

const metricCards = computed<MetricCard[]>(() => {
  const cards: MetricCard[] = [
    {
      label: '计划数量',
      value: productionTotals.value.planQuantity,
      unit: '件',
      icon: 'Tickets',
      color: '#3b82f6',
    },
    {
      label: '完成数量',
      value: productionTotals.value.finishQuantity,
      unit: '件',
      icon: 'Box',
      color: '#10b981',
    },
    {
      label: '计划达成率',
      value: formatPercent(completionRate.value),
      icon: 'TrendCharts',
      color: completionRate.value >= 90 ? '#10b981' : '#f59e0b',
    },
    {
      label: '直通率',
      value: formatPercent(passRate.value),
      icon: 'CircleCheck',
      color: passRate.value >= 95 ? '#10b981' : '#f59e0b',
    },
    {
      label: '未关闭安灯',
      value: overview.value.openAndonCount,
      unit: '条',
      icon: 'Warning',
      color: overview.value.openAndonCount > 0 ? '#ef4444' : '#10b981',
    },
    {
      label: '设备运行率',
      value: formatPercent(equipmentRunningRate.value),
      icon: 'Odometer',
      color: equipmentRunningRate.value >= 85 ? '#10b981' : '#f59e0b',
    },
  ]
  if (props.mode === 'central') {
    cards.push(
      { label: '设备 OEE', value: '--', icon: 'DataAnalysis', color: '#64748b' },
      { label: '能源消耗', value: '--', icon: 'Lightning', color: '#64748b' },
    )
  }
  return cards
})

const currentTask = computed(() => tasks.value[0])

const lineSummaries = computed<LineSummary[]>(() => {
  const grouped = new Map<string, Omit<LineSummary, 'completionRate' | 'passRate'>>()
  for (const task of tasks.value) {
    const key = String(task.lineId ?? task.lineName ?? 'unassigned')
    const current = grouped.get(key) ?? {
      key,
      lineName: task.lineName ?? '未分配产线',
      taskCount: 0,
      planQuantity: 0,
      finishQuantity: 0,
      goodQuantity: 0,
      defectQuantity: 0,
      abnormal: false,
    }
    current.taskCount += 1
    current.planQuantity += task.planQuantity ?? 0
    current.finishQuantity += task.finishQuantity ?? 0
    current.goodQuantity += task.goodQuantity ?? 0
    current.defectQuantity += task.defectQuantity ?? 0
    current.abnormal ||= task.abnormal
    grouped.set(key, current)
  }
  return Array.from(grouped.values())
    .map((item) => ({
      ...item,
      completionRate: rate(item.finishQuantity, item.planQuantity),
      passRate: rate(item.goodQuantity, item.goodQuantity + item.defectQuantity),
    }))
    .sort((a, b) => Number(b.abnormal) - Number(a.abnormal) || a.completionRate - b.completionRate)
})

const lineComparisonData = computed<AxisChartData | undefined>(() => {
  if (lineSummaries.value.length === 0) return undefined
  return {
    categories: lineSummaries.value.map((item) => item.lineName),
    series: [
      { name: '计划数量', data: lineSummaries.value.map((item) => item.planQuantity) },
      { name: '完成数量', data: lineSummaries.value.map((item) => item.finishQuantity) },
    ],
  }
})

const qualityData = computed<PieDataItem[]>(() => [
  { name: '合格', value: productionTotals.value.goodQuantity },
  { name: '不良', value: productionTotals.value.defectQuantity },
])

const warnings = computed(() => {
  const result = [
    ...(snapshot.value?.sourceWarnings ?? []),
    ...(snapshot.value?.overview?.warnings ?? []),
  ]
  if (props.mode === 'central') {
    result.push('设备 OEE 与能源指标尚无后端数据源，当前以“--”明确标识，未使用估算值。')
  }
  return Array.from(new Set(result))
})

function rate(numerator: number, denominator: number): number {
  if (!denominator) return 0
  return Number(((numerator / denominator) * 100).toFixed(2))
}

function formatPercent(value: number): string {
  return `${value.toFixed(1)}%`
}

function progressPercentage(task: RealtimeProductionTask): number {
  return Math.min(rate(task.finishQuantity ?? 0, task.planQuantity ?? 0), 100)
}

function hasScope(): boolean {
  if (props.mode === 'line') return lineId.value !== undefined
  if (props.mode === 'workshop') return workshopId.value !== undefined
  return true
}

async function fetchSnapshot(): Promise<KanbanSnapshot> {
  if (props.mode === 'line') return getLineKanban(lineId.value!)
  if (props.mode === 'workshop') return getWorkshopKanban(workshopId.value!)
  return getCentralKanban()
}

async function refreshNow() {
  if (!hasScope() || loading.value) return
  loading.value = true
  try {
    const params = {
      workshopId: props.mode === 'central' ? undefined : workshopId.value,
      lineId: props.mode === 'line' ? lineId.value : undefined,
    }
    const [nextSnapshot, nextTasks] = await Promise.all([
      fetchSnapshot(),
      getRealtimeTasks(params),
    ])
    snapshot.value = nextSnapshot
    tasks.value = nextTasks
    loadError.value = ''
    lastSuccessAt.value = new Date(nextSnapshot.lastRefreshTime ?? Date.now())
  } catch {
    loadError.value = snapshot.value
      ? '本次刷新失败，当前继续展示上一次成功获取的数据。'
      : '看板数据加载失败，请检查服务状态后重试。'
  } finally {
    loading.value = false
  }
}

async function loadLines(selectFirst = true) {
  lineOptions.value = await loadLineOptions(workshopId.value)
  if (selectFirst) {
    const firstValue = lineOptions.value[0]?.value
    lineId.value = typeof firstValue === 'number' ? firstValue : undefined
  }
}

async function initialize() {
  optionLoading.value = true
  snapshot.value = null
  tasks.value = []
  loadError.value = ''
  workshopId.value = undefined
  lineId.value = undefined
  try {
    if (props.mode !== 'central') {
      workshopOptions.value = await loadWorkshopOptions()
      let allowedWorkshopId: number | null = null
      let allowedLineId: number | null = null
      if (!userStore.roleCodes.includes(ADMIN_ROLE)) {
        const profile = await getProfile()
        allowedWorkshopId = profile.workshopId
        allowedLineId = profile.lineId
        if (allowedWorkshopId !== null) {
          workshopOptions.value = workshopOptions.value.filter(
            (option) => option.value === allowedWorkshopId,
          )
          if (workshopOptions.value.length === 0) {
            workshopOptions.value = [{ label: `授权车间 #${allowedWorkshopId}`, value: allowedWorkshopId }]
          }
        }
      }
      const firstWorkshop = allowedWorkshopId ?? workshopOptions.value[0]?.value
      workshopId.value = typeof firstWorkshop === 'number' ? firstWorkshop : undefined
      if (props.mode === 'line' && workshopId.value !== undefined) {
        await loadLines()
        if (allowedLineId !== null) {
          lineOptions.value = lineOptions.value.filter((option) => option.value === allowedLineId)
          if (lineOptions.value.length === 0) {
            lineOptions.value = [{ label: `授权产线 #${allowedLineId}`, value: allowedLineId }]
          }
          lineId.value = allowedLineId
        }
      }
    }
  } catch {
    loadError.value = '看板范围选项加载失败，请稍后重试。'
  } finally {
    optionLoading.value = false
  }
  await refreshNow()
}

async function handleWorkshopChange() {
  if (props.mode === 'line') {
    optionLoading.value = true
    try {
      await loadLines()
    } finally {
      optionLoading.value = false
    }
  }
  await refreshNow()
}

async function handleLineChange() {
  await refreshNow()
}

function restartRefreshTimer() {
  if (refreshTimer !== undefined) window.clearInterval(refreshTimer)
  refreshTimer = undefined
  if (autoRefresh.value) {
    refreshTimer = window.setInterval(() => void refreshNow(), 60_000)
  }
}

async function toggleFullscreen() {
  if (!document.fullscreenElement) {
    await boardRef.value?.requestFullscreen()
  } else {
    await document.exitFullscreen()
  }
}

function handleFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === boardRef.value
}

watch(autoRefresh, restartRefreshTimer)
watch(
  () => props.mode,
  () => void initialize(),
)

onMounted(() => {
  clockTimer = window.setInterval(() => {
    currentTime.value = new Date()
  }, 1_000)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  restartRefreshTimer()
  void initialize()
})

onBeforeUnmount(() => {
  if (refreshTimer !== undefined) window.clearInterval(refreshTimer)
  if (clockTimer !== undefined) window.clearInterval(clockTimer)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<template>
  <div ref="boardRef" class="kanban" :class="{ 'kanban--fullscreen': isFullscreen }">
    <header class="kanban__header">
      <div>
        <div class="kanban__eyebrow">BADMINTON MES · {{ scopeName }}</div>
        <h1>{{ modeConfig.title }}</h1>
        <p>{{ modeConfig.description }}</p>
      </div>
      <div class="kanban__header-right">
        <div class="kanban__clock">{{ formatDateTime(currentTime) }}</div>
        <div class="kanban__controls">
          <el-select
            v-if="mode !== 'central'"
            v-model="workshopId"
            :loading="optionLoading"
            placeholder="选择车间"
            class="kanban__select"
            @change="handleWorkshopChange"
          >
            <el-option
              v-for="option in workshopOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select
            v-if="mode === 'line'"
            v-model="lineId"
            :loading="optionLoading"
            placeholder="选择产线"
            class="kanban__select"
            @change="handleLineChange"
          >
            <el-option
              v-for="option in lineOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-switch v-model="autoRefresh" inline-prompt active-text="自动" inactive-text="暂停" />
          <el-button :loading="loading" @click="refreshNow">立即刷新</el-button>
          <el-button @click="toggleFullscreen">{{ isFullscreen ? '退出全屏' : '全屏展示' }}</el-button>
        </div>
        <div class="kanban__refresh-meta">
          <el-tag
            :type="snapshot?.dataStatus === 'PARTIAL' || overview.dataStatus === 'PARTIAL' ? 'warning' : 'success'"
            size="small"
          >
            {{ overview.dataStatus ?? snapshot?.dataStatus ?? '等待数据' }}
          </el-tag>
          <span>每 60 秒刷新</span>
          <span>最后成功：{{ lastSuccessAt ? formatDateTime(lastSuccessAt) : '--' }}</span>
        </div>
      </div>
    </header>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      :closable="false"
      show-icon
      class="kanban__alert"
    />

    <el-alert
      v-if="overview.criticalAndonCount > 0"
      :title="`存在 ${overview.criticalAndonCount} 条严重安灯异常，请立即处置`"
      type="error"
      :closable="false"
      show-icon
      class="kanban__alert kanban__alert--critical"
    />

    <main v-loading="loading && !snapshot" class="kanban__body">
      <el-empty v-if="!snapshot && !loading" description="暂无可展示的看板数据" />
      <template v-else>
        <el-row :gutter="14" class="kanban__metrics">
          <el-col
            v-for="card in metricCards"
            :key="card.label"
            :xs="12"
            :sm="8"
            :md="mode === 'central' ? 6 : 4"
          >
            <StatCard
              :label="card.label"
              :value="card.value"
              :unit="card.unit"
              :icon="card.icon"
              :icon-color="card.color"
            />
          </el-col>
        </el-row>

        <el-row :gutter="14" class="kanban__section-row">
          <el-col :xs="24" :lg="7">
            <el-card shadow="never" class="kanban__panel">
              <GaugeCard
                title="计划达成"
                name="达成率"
                :value="completionRate"
                height="280px"
              />
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="9">
            <el-card shadow="never" class="kanban__panel">
              <BarChartCard
                :title="mode === 'line' ? '当前任务计划与产出' : '各产线计划与产出'"
                :data="lineComparisonData"
                height="280px"
                unit="件"
              />
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="8">
            <el-card shadow="never" class="kanban__panel">
              <PieChartCard
                title="质量构成"
                :data="qualityData"
                :center-text="formatPercent(passRate)"
                height="280px"
                donut
              />
              <div class="kanban__quality-note">综合不良率 {{ formatPercent(defectRate) }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row v-if="mode === 'line'" :gutter="14" class="kanban__section-row">
          <el-col :span="24">
            <el-card shadow="never" class="kanban__panel">
              <template #header>
                <div class="kanban__panel-title">
                  <span>当前生产任务</span>
                  <el-tag v-if="currentTask?.abnormal" type="danger" effect="dark">异常</el-tag>
                  <el-tag v-else-if="currentTask" type="success">生产中</el-tag>
                  <el-tag v-else type="info">待生产 / 空闲</el-tag>
                </div>
              </template>
              <el-empty v-if="!currentTask" description="当前产线暂无在制任务" />
              <template v-else>
                <el-descriptions :column="4" border>
                  <el-descriptions-item label="任务号">{{ currentTask.taskNo ?? '--' }}</el-descriptions-item>
                  <el-descriptions-item label="工单号">{{ currentTask.workOrderNo ?? '--' }}</el-descriptions-item>
                  <el-descriptions-item label="产品">{{ currentTask.productName ?? '--' }}</el-descriptions-item>
                  <el-descriptions-item label="批次">{{ currentTask.batchNo ?? '--' }}</el-descriptions-item>
                  <el-descriptions-item label="计划数量">{{ formatNumber(currentTask.planQuantity) }}</el-descriptions-item>
                  <el-descriptions-item label="投入数量">{{ formatNumber(currentTask.inputQuantity) }}</el-descriptions-item>
                  <el-descriptions-item label="合格数量">{{ formatNumber(currentTask.goodQuantity) }}</el-descriptions-item>
                  <el-descriptions-item label="不良数量">{{ formatNumber(currentTask.defectQuantity) }}</el-descriptions-item>
                </el-descriptions>
                <div class="kanban__task-progress">
                  <span>任务进度</span>
                  <el-progress
                    :percentage="progressPercentage(currentTask)"
                    :status="currentTask.abnormal ? 'exception' : undefined"
                    :stroke-width="18"
                  />
                </div>
              </template>
            </el-card>
          </el-col>
        </el-row>

        <el-row v-else :gutter="14" class="kanban__section-row">
          <el-col :span="24">
            <el-card shadow="never" class="kanban__panel">
              <template #header>
                <div class="kanban__panel-title">
                  <span>产线运行概览</span>
                  <small>异常产线优先，其次按达成率从低到高排列</small>
                </div>
              </template>
              <el-table :data="lineSummaries" border stripe max-height="420">
                <el-table-column label="产线" prop="lineName" min-width="160" />
                <el-table-column label="在制任务" prop="taskCount" width="100" align="right" />
                <el-table-column label="计划" prop="planQuantity" width="110" align="right" />
                <el-table-column label="完成数量" prop="finishQuantity" width="110" align="right" />
                <el-table-column label="不良" prop="defectQuantity" width="90" align="right" />
                <el-table-column label="达成率" min-width="220">
                  <template #default="{ row }">
                    <el-progress
                      :percentage="Math.min(row.completionRate, 100)"
                      :status="row.abnormal ? 'exception' : row.completionRate >= 90 ? 'success' : undefined"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="直通率" width="100" align="right">
                  <template #default="{ row }">{{ formatPercent(row.passRate) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.abnormal" type="danger" effect="dark">异常</el-tag>
                    <el-tag v-else-if="row.completionRate < 60" type="warning">关注</el-tag>
                    <el-tag v-else type="success">正常</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="14" class="kanban__section-row">
          <el-col :xs="24" :lg="12">
            <el-card shadow="never" class="kanban__panel kanban__status-panel">
              <template #header><span class="kanban__panel-heading">设备状态</span></template>
              <div class="kanban__status-grid">
                <div><strong>{{ overview.equipmentTotalCount }}</strong><span>设备总数</span></div>
                <div class="success"><strong>{{ overview.runningEquipmentCount }}</strong><span>运行设备</span></div>
                <div :class="{ danger: overview.unavailableEquipmentCount > 0 }">
                  <strong>{{ overview.unavailableEquipmentCount }}</strong><span>不可用设备</span>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="12">
            <el-card shadow="never" class="kanban__panel kanban__status-panel">
              <template #header><span class="kanban__panel-heading">异常与数据完整度</span></template>
              <div class="kanban__status-grid kanban__status-grid--compact">
                <div :class="{ danger: overview.openAndonCount > 0 }">
                  <strong>{{ overview.openAndonCount }}</strong><span>未关闭安灯</span>
                </div>
                <div :class="{ danger: overview.criticalAndonCount > 0 }">
                  <strong>{{ overview.criticalAndonCount }}</strong><span>严重安灯</span>
                </div>
                <div :class="{ danger: overview.abnormalBatchCount > 0 }">
                  <strong>{{ overview.abnormalBatchCount }}</strong><span>异常批次</span>
                </div>
              </div>
              <ul v-if="warnings.length" class="kanban__warnings">
                <li v-for="warning in warnings" :key="warning">{{ warning }}</li>
              </ul>
              <div v-else class="kanban__all-clear">当前无数据源告警</div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </main>
  </div>
</template>

<style scoped>
.kanban {
  min-height: calc(100vh - 124px);
  padding: 18px;
  overflow: auto;
  color: #172033;
  background:
    radial-gradient(circle at 8% 0%, rgb(59 130 246 / 12%), transparent 28%),
    radial-gradient(circle at 92% 8%, rgb(16 185 129 / 10%), transparent 24%),
    #f3f6fb;
}

.kanban--fullscreen {
  min-height: 100vh;
}

.kanban__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 24px;
  background: rgb(255 255 255 / 92%);
  border: 1px solid rgb(148 163 184 / 22%);
  border-radius: 12px;
  box-shadow: 0 14px 40px rgb(15 23 42 / 7%);
}

.kanban__eyebrow {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
  letter-spacing: 0.08em;
}

.kanban__header h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.25;
}

.kanban__header p {
  margin: 7px 0 0;
  font-size: 13px;
  color: #64748b;
}

.kanban__header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.kanban__clock {
  font-variant-numeric: tabular-nums;
  font-size: 20px;
  font-weight: 700;
  color: #1d4ed8;
}

.kanban__controls,
.kanban__refresh-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.kanban__select {
  width: 190px;
}

.kanban__refresh-meta {
  font-size: 12px;
  color: #64748b;
}

.kanban__alert {
  margin-top: 12px;
}

.kanban__alert--critical {
  animation: alert-pulse 1.8s ease-in-out infinite;
}

.kanban__body {
  min-height: 320px;
}

.kanban__metrics,
.kanban__section-row {
  margin-top: 14px;
}

.kanban__metrics :deep(.el-col),
.kanban__section-row :deep(.el-col) {
  margin-bottom: 14px;
}

.kanban__metrics :deep(.stat-card) {
  min-height: 94px;
  border: 1px solid rgb(148 163 184 / 18%);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgb(15 23 42 / 6%);
}

.kanban__panel {
  height: 100%;
  border: 1px solid rgb(148 163 184 / 18%);
  border-radius: 10px;
}

.kanban__panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 16px;
  font-weight: 700;
}

.kanban__panel-title small {
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
}

.kanban__panel-heading {
  font-size: 16px;
  font-weight: 700;
}

.kanban__quality-note {
  margin-top: -22px;
  text-align: center;
  font-size: 12px;
  color: #64748b;
}

.kanban__task-progress {
  display: grid;
  grid-template-columns: 80px 1fr;
  align-items: center;
  gap: 14px;
  margin-top: 20px;
  font-size: 14px;
  font-weight: 600;
}

.kanban__status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.kanban__status-grid > div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 18px 8px;
  color: #475569;
  background: #f8fafc;
  border-radius: 8px;
}

.kanban__status-grid strong {
  font-size: 28px;
  color: #334155;
}

.kanban__status-grid span {
  font-size: 12px;
}

.kanban__status-grid .success strong {
  color: #059669;
}

.kanban__status-grid .danger strong {
  color: #dc2626;
}

.kanban__warnings {
  max-height: 110px;
  margin: 14px 0 0;
  padding-left: 20px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.7;
  color: #b45309;
}

.kanban__all-clear {
  margin-top: 14px;
  font-size: 12px;
  color: #059669;
}

@keyframes alert-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.78;
  }
}

@media (max-width: 1100px) {
  .kanban__header {
    flex-direction: column;
  }

  .kanban__header-right {
    align-items: flex-start;
  }

  .kanban__controls,
  .kanban__refresh-meta {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .kanban {
    padding: 10px;
  }

  .kanban__header {
    padding: 16px;
  }

  .kanban__select {
    width: 100%;
  }

  .kanban__status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
