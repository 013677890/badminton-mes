<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ECOption } from '@/utils/echarts'
import { useChart } from '@/composables/useChart'
import EmptyState from '@/components/base/EmptyState.vue'

defineOptions({ name: 'ChartWrapper' })

const props = withDefaults(
  defineProps<{
    option?: ECOption
    loading?: boolean
    height?: string
    title?: string
  }>(),
  { loading: false, height: '320px' },
)

const emit = defineEmits<{
  'chart-click': [params: unknown]
}>()

const chartEl = ref<HTMLElement>()
const { setOption, setLoading, onEvent, resize, getInstance } = useChart(chartEl)

/** series 缺失或数据全空视为空数据，盖 EmptyState */
const isEmpty = computed(() => {
  const option = props.option
  if (!option) return true
  const series = option.series
  const list = Array.isArray(series) ? series : series ? [series] : []
  if (list.length === 0) return true
  return list.every((item) => {
    const data = (item as { data?: unknown[] }).data
    return !data || data.length === 0
  })
})

watch(
  () => props.option,
  (option) => {
    if (option && !isEmpty.value) setOption(option)
  },
  { immediate: true, deep: true },
)

watch(
  () => props.loading,
  (value) => setLoading(value),
)

onEvent('click', (params) => emit('chart-click', params))

defineExpose({ resize, getInstance })
</script>

<template>
  <div class="chart-wrapper">
    <div v-if="title || $slots.extra" class="chart-wrapper__header">
      <span class="chart-wrapper__title">{{ title }}</span>
      <div class="chart-wrapper__extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="chart-wrapper__body" :style="{ height }">
      <div ref="chartEl" class="chart-wrapper__canvas" />
      <div v-if="isEmpty && !loading" class="chart-wrapper__empty">
        <EmptyState />
      </div>
    </div>
  </div>
</template>

<style scoped>
.chart-wrapper__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.chart-wrapper__title {
  font-size: 15px;
  font-weight: 600;
}

.chart-wrapper__body {
  position: relative;
}

.chart-wrapper__canvas {
  width: 100%;
  height: 100%;
}

.chart-wrapper__empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color);
}
</style>
