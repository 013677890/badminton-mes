<script setup lang="ts">
import { computed } from 'vue'
import type { CustomSeriesOption } from 'echarts/charts'
import { graphic } from 'echarts/core'
import type { ECOption } from '@/utils/echarts'
import type { GanttRow, GanttTask, StatusMap, TagType } from '@/types/components'
import ChartWrapper from '@/components/base/charts/ChartWrapper.vue'

defineOptions({ name: 'GanttSchedule' })

/** TagType 语义色 → 甘特条填充色 */
const TYPE_COLORS: Record<TagType, string> = {
  primary: '#409eff',
  success: '#67c23a',
  info: '#909399',
  warning: '#e6a23c',
  danger: '#f56c6c',
}

const props = withDefaults(
  defineProps<{
    /** 甘特行（产线/设备/班组） */
    rows: GanttRow[]
    tasks: GanttTask[]
    /** 状态 → 颜色/文案（复用 StatusMap，type 决定条形色，text 进图例与提示） */
    statusMap?: StatusMap
    loading?: boolean
    /** 不传则按行数自适应 */
    height?: string
    /** 视窗时间范围，缺省取任务最早/最晚时间外扩 1 小时 */
    timeRange?: [string, string]
  }>(),
  { loading: false },
)

const emit = defineEmits<{
  'task-click': [task: GanttTask]
}>()

const chartHeight = computed(
  () => props.height ?? `${Math.max(props.rows.length, 3) * 48 + 88}px`,
)

function taskColor(task: GanttTask): string {
  const type = task.status ? props.statusMap?.[task.status]?.type : undefined
  return TYPE_COLORS[type ?? 'primary']
}

/** 图例：仅展示任务中实际出现的状态 */
const legendItems = computed(() => {
  if (!props.statusMap) return []
  const used = new Set(props.tasks.map((task) => task.status).filter(Boolean))
  return Object.entries(props.statusMap)
    .filter(([status]) => used.has(status))
    .map(([status, meta]) => ({ status, text: meta.text, color: TYPE_COLORS[meta.type] }))
})

interface GanttDataItem {
  value: [number, number, number]
  name: string
  itemStyle: { color: string }
  task: GanttTask
}

const chartData = computed<GanttDataItem[]>(() => {
  const rowIndexMap = new Map(props.rows.map((row, index) => [row.key, index]))
  return props.tasks
    .filter((task) => rowIndexMap.has(task.rowKey))
    .map((task) => ({
      value: [
        rowIndexMap.get(task.rowKey)!,
        new Date(task.start).getTime(),
        new Date(task.end).getTime(),
      ],
      name: task.name,
      itemStyle: { color: taskColor(task) },
      task,
    }))
})

const renderGanttBar: NonNullable<CustomSeriesOption['renderItem']> = (params, api) => {
  const rowIndex = Number(api.value(0))
  const start = api.coord([api.value(1), rowIndex])
  const end = api.coord([api.value(2), rowIndex])
  const barHeight = Math.min((api.size!([0, 1]) as number[])[1]! * 0.5, 26)
  const coordSys = params.coordSys as unknown as {
    x: number
    y: number
    width: number
    height: number
  }
  const clipped = graphic.clipRectByRect(
    {
      x: start[0]!,
      y: start[1]! - barHeight / 2,
      width: Math.max(end[0]! - start[0]!, 2),
      height: barHeight,
    },
    coordSys,
  )
  if (!clipped) return
  const label = (params as unknown as { data?: GanttDataItem }).data?.name ?? ''
  return {
    type: 'rect',
    transition: ['shape'],
    shape: { ...clipped, r: 3 },
    style: api.style(),
    textConfig: { position: 'inside' },
    textContent:
      clipped.width > 48
        ? {
            style: {
              text: label,
              fill: '#fff',
              fontSize: 12,
              width: clipped.width - 8,
              overflow: 'truncate',
            },
          }
        : undefined,
    emphasis: { style: { opacity: 0.85 } },
  } as ReturnType<NonNullable<CustomSeriesOption['renderItem']>>
}

const option = computed<ECOption>(() => {
  const [rangeStart, rangeEnd] = props.timeRange ?? []
  const times = chartData.value.flatMap((item) => [item.value[1], item.value[2]])
  const oneHour = 3600 * 1000
  return {
    tooltip: {
      formatter: (params: unknown) => {
        const task = (params as { data?: GanttDataItem }).data?.task
        if (!task) return ''
        const statusText = task.status ? (props.statusMap?.[task.status]?.text ?? task.status) : ''
        return [
          `<b>${task.name}</b>`,
          statusText,
          `${task.start} ~ ${task.end}`,
        ]
          .filter(Boolean)
          .join('<br/>')
      },
    },
    grid: { left: 8, right: 16, top: 12, bottom: 40, containLabel: true },
    xAxis: {
      type: 'time',
      min: rangeStart ?? (times.length ? Math.min(...times) - oneHour : undefined),
      max: rangeEnd ?? (times.length ? Math.max(...times) + oneHour : undefined),
      axisLabel: { formatter: '{MM}-{dd} {HH}:{mm}' },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: props.rows.map((row) => row.label),
      axisTick: { show: false },
      splitLine: { show: true, lineStyle: { color: 'var(--el-border-color-lighter)' } },
    },
    dataZoom: [
      { type: 'slider', xAxisIndex: 0, height: 16, bottom: 8 },
      { type: 'inside', xAxisIndex: 0 },
    ],
    series: [
      {
        type: 'custom',
        renderItem: renderGanttBar,
        encode: { x: [1, 2], y: 0 },
        data: chartData.value,
        clip: true,
      },
    ],
  }
})

function handleChartClick(params: unknown) {
  const task = (params as { data?: GanttDataItem }).data?.task
  if (task) emit('task-click', task)
}
</script>

<template>
  <div class="gantt-schedule">
    <div v-if="legendItems.length" class="gantt-schedule__legend">
      <span v-for="item in legendItems" :key="item.status" class="gantt-schedule__legend-item">
        <span class="gantt-schedule__legend-dot" :style="{ backgroundColor: item.color }" />
        {{ item.text }}
      </span>
    </div>
    <ChartWrapper
      :option="option"
      :loading="loading"
      :height="chartHeight"
      @chart-click="handleChartClick"
    />
  </div>
</template>

<style scoped>
.gantt-schedule__legend {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.gantt-schedule__legend-item {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.gantt-schedule__legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}
</style>
