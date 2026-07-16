<script setup lang="ts">
import { computed } from 'vue'
import type { ECOption } from '@/utils/echarts'
import type { AxisChartData } from '@/types/components'
import ChartWrapper from './ChartWrapper.vue'

defineOptions({ name: 'BarChartCard' })

const props = withDefaults(
  defineProps<{
    data?: AxisChartData
    title?: string
    loading?: boolean
    height?: string
    /** 横向条形图（类目在 y 轴），适合车间/产线对比 */
    horizontal?: boolean
    /** y 轴单位 */
    unit?: string
  }>(),
  { loading: false, height: '320px', horizontal: false },
)

const emit = defineEmits<{
  'chart-click': [params: unknown]
}>()

const option = computed<ECOption | undefined>(() => {
  const data = props.data
  if (!data) return undefined
  const categoryAxis = { type: 'category' as const, data: data.categories }
  const valueAxis = { type: 'value' as const, name: props.unit }
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: data.series.length > 1 ? { bottom: 0 } : undefined,
    grid: { left: 8, right: 16, top: 32, bottom: data.series.length > 1 ? 32 : 8, containLabel: true },
    xAxis: props.horizontal ? valueAxis : categoryAxis,
    yAxis: props.horizontal ? categoryAxis : valueAxis,
    series: data.series.map((item) => ({
      type: 'bar',
      name: item.name,
      data: item.data,
      stack: item.stack,
      barMaxWidth: 32,
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
