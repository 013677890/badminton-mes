<script setup lang="ts">
import { computed } from 'vue'
import type { ECOption } from '@/utils/echarts'
import type { AxisChartData } from '@/types/components'
import ChartWrapper from './ChartWrapper.vue'

defineOptions({ name: 'LineChartCard' })

const props = withDefaults(
  defineProps<{
    data?: AxisChartData
    title?: string
    loading?: boolean
    height?: string
    /** 平滑曲线，默认 true */
    smooth?: boolean
    /** 面积填充 */
    area?: boolean
    /** y 轴单位，展示在轴名位置 */
    unit?: string
  }>(),
  { loading: false, height: '320px', smooth: true, area: false },
)

const emit = defineEmits<{
  'chart-click': [params: unknown]
}>()

const option = computed<ECOption | undefined>(() => {
  const data = props.data
  if (!data) return undefined
  return {
    tooltip: { trigger: 'axis' },
    legend: data.series.length > 1 ? { bottom: 0 } : undefined,
    grid: { left: 8, right: 16, top: 32, bottom: data.series.length > 1 ? 32 : 8, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.categories },
    yAxis: { type: 'value', name: props.unit },
    series: data.series.map((item) => ({
      type: 'line',
      name: item.name,
      data: item.data,
      smooth: props.smooth,
      showSymbol: item.data.length <= 31,
      areaStyle: props.area ? { opacity: 0.15 } : undefined,
    })),
  }
})
</script>

<template>
  <ChartWrapper
    :option="option"
    :title="title"
    :loading="loading"
    :height="height"
    @chart-click="emit('chart-click', $event)"
  >
    <template v-if="$slots.extra" #extra>
      <slot name="extra" />
    </template>
  </ChartWrapper>
</template>
