<script setup lang="ts" generic="Row extends Record<string, any>">
import { computed, ref, useSlots } from 'vue'
import type { ColumnDef, FilterField, Pagination } from '@/types/components'
import type { ECOption } from '@/utils/echarts'
import FilterBar from '@/components/base/FilterBar.vue'
import ProTable from '@/components/base/ProTable.vue'
import ChartWrapper from '@/components/base/charts/ChartWrapper.vue'

defineOptions({ name: 'QueryChartPanel', inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    /** 查询区字段配置 */
    filterFields: FilterField[]
    /** 图表配置；也可用 #chart 插槽完全自定义图表区 */
    chartOption?: ECOption
    chartTitle?: string
    chartHeight?: string
    chartLoading?: boolean
    /** 明细表列配置；不传则隐藏明细区 */
    columns?: ColumnDef<Row>[]
    data?: Row[]
    loading?: boolean
    pagination?: Pagination | false
    tableTitle?: string
  }>(),
  {
    chartHeight: '340px',
    chartLoading: false,
    loading: false,
    pagination: false,
    tableTitle: '明细数据',
  },
)

const emit = defineEmits<{
  query: [params: Record<string, any>]
  reset: [params: Record<string, any>]
  'page-change': [page: { pageNo: number; pageSize: number }]
  'chart-click': [params: unknown]
}>()

const slots = useSlots()

const tableSlotNames = computed(() =>
  Object.keys(slots).filter(
    (name) => name.startsWith('col-') || name === 'action' || name === 'empty',
  ),
)

const showTable = computed(() => (props.columns?.length ?? 0) > 0)

const filterBarRef = ref<InstanceType<typeof FilterBar>>()

defineExpose({
  getFilterParams: () => filterBarRef.value?.getParams() ?? {},
  setFilterParams: (params: Record<string, any>) => filterBarRef.value?.setParams(params),
})
</script>

<template>
  <div class="query-chart-panel">
    <el-card shadow="never">
      <FilterBar
        ref="filterBarRef"
        :fields="filterFields"
        @query="emit('query', $event)"
        @reset="emit('reset', $event)"
      >
        <template #extra>
          <slot name="filter-extra" />
        </template>
      </FilterBar>
    </el-card>

    <el-card shadow="never">
      <slot name="chart">
        <ChartWrapper
          :option="chartOption"
          :title="chartTitle"
          :loading="chartLoading"
          :height="chartHeight"
          @chart-click="emit('chart-click', $event)"
        >
          <template v-if="slots['chart-extra']" #extra>
            <slot name="chart-extra" />
          </template>
        </ChartWrapper>
      </slot>
    </el-card>

    <el-card v-if="showTable" shadow="never">
      <div class="query-chart-panel__table-header">
        <span class="query-chart-panel__table-title">{{ tableTitle }}</span>
        <div class="query-chart-panel__table-actions">
          <slot name="toolbar" />
        </div>
      </div>
      <ProTable
        v-bind="$attrs"
        :columns="columns!"
        :data="data ?? []"
        :loading="loading"
        :pagination="pagination"
        @page-change="emit('page-change', $event)"
      >
        <template v-for="name in tableSlotNames" :key="name" #[name]="scope">
          <slot :name="name" v-bind="scope" />
        </template>
      </ProTable>
    </el-card>
  </div>
</template>

<style scoped>
.query-chart-panel > .el-card + .el-card {
  margin-top: 16px;
}

.query-chart-panel__table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.query-chart-panel__table-title {
  font-size: 15px;
  font-weight: 600;
}

.query-chart-panel__table-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
