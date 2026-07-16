<script lang="ts">
/** 分段阈值：到该占比（0-1）为止使用该颜色 */
export interface GaugeThreshold {
  /** 0-1 之间的分段终点占比 */
  ratio: number
  color: string
}

/** 缺省阈值：低红 / 中黄 / 高绿（稼动率、合格率语义）。模块作用域，供 defineProps 默认值引用 */
const DEFAULT_THRESHOLDS: GaugeThreshold[] = [
  { ratio: 0.6, color: '#f56c6c' },
  { ratio: 0.85, color: '#e6a23c' },
  { ratio: 1, color: '#67c23a' },
]
</script>

<script setup lang="ts">
import { computed } from 'vue'
import type { ECOption } from '@/utils/echarts'
import ChartWrapper from './ChartWrapper.vue'

defineOptions({ name: 'GaugeCard' })

const props = withDefaults(
  defineProps<{
    value?: number | null
    title?: string
    loading?: boolean
    height?: string
    /** 仪表盘内指标名，如 稼动率 */
    name?: string
    unit?: string
    min?: number
    max?: number
    /** 轴分段配色，按 ratio 升序 */
    thresholds?: GaugeThreshold[]
  }>(),
  {
    loading: false,
    height: '260px',
    unit: '%',
    min: 0,
    max: 100,
    thresholds: () => DEFAULT_THRESHOLDS,
  },
)

const option = computed<ECOption | undefined>(() => {
  if (props.value === undefined || props.value === null) return undefined
  const ratio = (props.value - props.min) / (props.max - props.min || 1)
  const current =
    props.thresholds.find((item) => ratio <= item.ratio) ??
    props.thresholds[props.thresholds.length - 1]
  return {
    series: [
      {
        type: 'gauge',
        min: props.min,
        max: props.max,
        startAngle: 210,
        endAngle: -30,
        radius: '95%',
        center: ['50%', '58%'],
        axisLine: {
          lineStyle: {
            width: 14,
            color: props.thresholds.map((item) => [item.ratio, item.color]),
          },
        },
        pointer: { length: '58%', width: 5, itemStyle: { color: 'auto' } },
        axisTick: { distance: -14, length: 4, lineStyle: { color: '#fff', width: 1 } },
        splitLine: { distance: -14, length: 14, lineStyle: { color: '#fff', width: 2 } },
        axisLabel: { distance: 22, fontSize: 10, color: 'inherit' },
        title: { offsetCenter: [0, '68%'], fontSize: 13 },
        detail: {
          valueAnimation: true,
          offsetCenter: [0, '38%'],
          fontSize: 24,
          fontWeight: 600,
          formatter: `{value}${props.unit}`,
          color: current?.color,
        },
        data: [{ value: props.value, name: props.name ?? '' }],
      },
    ],
  }
})
</script>

<template>
  <ChartWrapper :option="option" :title="title" :loading="loading" :height="height">
    <template v-if="$slots.extra" #extra>
      <slot name="extra" />
    </template>
  </ChartWrapper>
</template>
