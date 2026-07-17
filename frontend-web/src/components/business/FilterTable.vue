<script setup lang="ts" generic="Row extends Record<string, any>">
import { computed, ref, useSlots } from 'vue'
import type { ColumnDef, FilterField, Pagination, RowAction } from '@/types/components'
import FilterBar from '@/components/base/FilterBar.vue'
import ProTable from '@/components/base/ProTable.vue'

defineOptions({ name: 'FilterTable', inheritAttrs: false })

withDefaults(
  defineProps<{
    columns: ColumnDef<Row>[]
    data: Row[]
    /** 不传或空数组时隐藏筛选区 */
    filterFields?: FilterField[]
    loading?: boolean
    pagination?: Pagination | false
    rowKey?: string
    selectable?: boolean
    showIndex?: boolean
    rowActions?: RowAction<Row>[]
    actionWidth?: number | string
    /** 表格区标题 */
    title?: string
  }>(),
  { loading: false, pagination: false, rowKey: 'id', selectable: false, showIndex: false },
)

const emit = defineEmits<{
  /** 透传 FilterBar 的原始参数，避免限制各业务页的专用查询参数类型。 */
  query: [params: any]
  reset: [params: any]
  'page-change': [page: { pageNo: number; pageSize: number }]
  'selection-change': [rows: Row[]]
  'row-action': [key: string, row: Row]
}>()

const slots = useSlots()

/** 需要透传给 ProTable 的插槽：#col-{prop}、#action、#empty */
const tableSlotNames = computed(() =>
  Object.keys(slots).filter(
    (name) => name.startsWith('col-') || name === 'action' || name === 'empty',
  ),
)

const filterBarRef = ref<InstanceType<typeof FilterBar>>()

defineExpose({
  /** 读取当前筛选参数（配合 useTable 的 refresh 场景） */
  getFilterParams: () => filterBarRef.value?.getParams() ?? {},
  setFilterParams: (params: Record<string, any>) => filterBarRef.value?.setParams(params),
})
</script>

<template>
  <div class="filter-table">
    <el-card v-if="filterFields && filterFields.length > 0" shadow="never">
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
      <div v-if="title || slots.toolbar" class="filter-table__toolbar">
        <span class="filter-table__title">{{ title }}</span>
        <div class="filter-table__toolbar-actions">
          <slot name="toolbar" />
        </div>
      </div>
      <ProTable
        v-bind="$attrs"
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        :selectable="selectable"
        :show-index="showIndex"
        :row-actions="rowActions"
        :action-width="actionWidth"
        @page-change="emit('page-change', $event)"
        @selection-change="emit('selection-change', $event as Row[])"
        @row-action="(key: string, row: Row) => emit('row-action', key, row)"
      >
        <template v-for="name in tableSlotNames" :key="name" #[name]="scope">
          <slot :name="name" v-bind="scope" />
        </template>
      </ProTable>
    </el-card>
  </div>
</template>

<style scoped>
.filter-table > .el-card + .el-card {
  margin-top: 16px;
}

.filter-table__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.filter-table__title {
  font-size: 15px;
  font-weight: 600;
}

.filter-table__toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
