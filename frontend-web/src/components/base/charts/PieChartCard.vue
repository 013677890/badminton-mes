<script setup lang="ts">
import { computed } from 'vue'
import type { ECOption } from '@/utils/echarts'
import type { PieDataItem } from '@/types/components'
import ChartWrapper from './ChartWrapper.vue'

defineOptions({ name: 'PieChartCard' })

const props = withDefaults(
  defineProps<{
    data?: PieDataItem[]
    title?: string
    loading?: boolean
    height?: string
    /** 环形图；中心默认展示合计 */
    donut?: boolean
    /** 环形图中心文案，缺省为合计值 */
    centerText?: string
  }>(),
  { loading: false, height: '320px', donut: false },
)

const emit = defineEmits<{
  'chart-click': [params: unknown]
}>()

const option = computed<ECOption | undefined>(() => {
  const data = props.data
  if (!data) return undefined
  const total = data.reduce((sum, item) => sum + item.value, 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}（{d}%）' },
    legend: { orient: 'vertical', right: 8, top: 'center' },
    title: props.donut
      ? {
          text: props.centerText ?? String(total),
          subtext: props.centerText ? undefined : '合计',
          left: '38%',
          top: '42%',
          textAlign: 'center',
          textStyle: { fontSize: 22, fontWeight: 600 },
          subtextStyle: { fontSize: 12 },
        }
      : undefined,
    series: [
      {
        type: 'pie',
        radius: props.donut ? ['48%', '70%'] : '70%',
        center: ['38%', '50%'],
        data,
        label: { formatter: '{b} {d}%' },
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.3)' },
        },
      },
    ],
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
