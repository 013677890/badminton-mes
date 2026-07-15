<script setup lang="ts" generic="Row extends Record<string, any>">
import { useSlots } from 'vue'
import type { ColumnDef, Pagination, RowAction } from '@/types/components'
import { usePermission } from '@/composables/usePermission'
import EmptyState from './EmptyState.vue'
import StatusTag from './StatusTag.vue'

defineOptions({ name: 'ProTable', inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    columns: ColumnDef<Row>[]
    data: Row[]
    loading?: boolean
    /** 传 false 隐藏分页（如弹窗内小表格） */
    pagination?: Pagination | false
    rowKey?: string
    /** 显示多选列 */
    selectable?: boolean
    /** 显示序号列 */
    showIndex?: boolean
    /** 配置驱动的操作列按钮；也可用 #action 插槽完全自定义 */
    rowActions?: RowAction<Row>[]
    actionWidth?: number | string
  }>(),
  {
    loading: false,
    pagination: false,
    rowKey: 'id',
    selectable: false,
    showIndex: false,
    actionWidth: 160,
  },
)

const emit = defineEmits<{
  'page-change': [page: { pageNo: number; pageSize: number }]
  'selection-change': [rows: Row[]]
  'row-action': [key: string, row: Row]
}>()

const slots = useSlots()
const { hasRole } = usePermission()

function hasColSlot(prop: string): boolean {
  return !!slots[`col-${prop}`]
}

/** 支持 'a.b' 嵌套路径取值 */
function cellValue(row: Row, prop: string): unknown {
  return prop.split('.').reduce<any>((acc, key) => (acc == null ? acc : acc[key]), row)
}

function displayValue(row: Row, col: ColumnDef<Row>): string {
  const value = cellValue(row, col.prop)
  if (value === undefined || value === null || value === '') return '-'
  return String(value)
}

function visibleRowActions(row: Row): RowAction<Row>[] {
  return (props.rowActions ?? []).filter(
    (action) => hasRole(action.roles) && (action.show?.(row) ?? true),
  )
}

function handleSelectionChange(rows: Row[]) {
  emit('selection-change', rows)
}

function handleCurrentChange(pageNo: number) {
  if (!props.pagination) return
  emit('page-change', { pageNo, pageSize: props.pagination.pageSize })
}

function handleSizeChange(pageSize: number) {
  emit('page-change', { pageNo: 1, pageSize })
}
</script>

<template>
  <div class="pro-table">
    <el-table
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      border
      stripe
      v-bind="$attrs"
      @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="selectable" type="selection" width="50" fixed="left" />
      <el-table-column v-if="showIndex" type="index" label="序号" width="60" align="center" />
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :fixed="col.fixed"
        :align="col.align ?? 'left'"
        :sortable="col.sortable ?? false"
        :show-overflow-tooltip="col.showOverflowTooltip ?? true"
      >
        <template #default="scope">
          <!-- 单元格优先级：#col-{prop} 插槽 > statusMap > formatter > 原值 -->
          <slot
            v-if="hasColSlot(col.prop)"
            :name="`col-${col.prop}`"
            :row="scope.row as Row"
            :index="scope.$index"
            :value="cellValue(scope.row, col.prop)"
          />
          <StatusTag
            v-else-if="col.statusMap"
            :status="cellValue(scope.row, col.prop) as string | number"
            :status-map="col.statusMap"
          />
          <template v-else-if="col.formatter">{{ col.formatter(scope.row) }}</template>
          <template v-else>{{ displayValue(scope.row, col) }}</template>
        </template>
      </el-table-column>
      <el-table-column
        v-if="slots.action || rowActions?.length"
        label="操作"
        :width="actionWidth"
        fixed="right"
        align="center"
      >
        <template #default="scope">
          <slot name="action" :row="scope.row as Row" :index="scope.$index">
            <template v-for="action in visibleRowActions(scope.row)" :key="action.key">
              <el-popconfirm
                v-if="action.confirm"
                :title="action.confirm"
                width="220"
                @confirm="emit('row-action', action.key, scope.row)"
              >
                <template #reference>
                  <el-button
                    link
                    :type="action.type ?? 'primary'"
                    size="small"
                    :disabled="action.disabled?.(scope.row) ?? false"
                  >
                    {{ action.label }}
                  </el-button>
                </template>
              </el-popconfirm>
              <el-button
                v-else
                link
                :type="action.type ?? 'primary'"
                size="small"
                :disabled="action.disabled?.(scope.row) ?? false"
                @click="emit('row-action', action.key, scope.row)"
              >
                {{ action.label }}
              </el-button>
            </template>
          </slot>
        </template>
      </el-table-column>
      <template #empty>
        <slot name="empty">
          <EmptyState />
        </slot>
      </template>
    </el-table>
    <div v-if="pagination" class="pro-table__pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        :current-page="pagination.pageNo"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.pro-table__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
