<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormRules } from 'element-plus'
import { Download, Plus } from '@element-plus/icons-vue'
import type { ColumnDef, FilterField, RowAction, StatusMap } from '@/types/components'
import type { PageParam, PageResult } from '@/utils/request'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import FilterTable from '@/components/business/FilterTable.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { formatNumber } from '@/utils/format'

defineOptions({ name: 'TableDemo' })

// ---------- 领域模型与 mock 数据（真实场景替换为后端接口） ----------

interface WorkOrder {
  id: number
  orderNo: string
  productName: string
  quantity: number
  workshop: string
  status: string
  planDate: string
  createdAt: string
}

const ORDER_STATUS: StatusMap = {
  DRAFT: { type: 'info', text: '草稿' },
  RELEASED: { type: 'primary', text: '已下达' },
  IN_PROGRESS: { type: 'warning', text: '生产中' },
  FINISHED: { type: 'success', text: '已完工' },
  CLOSED: { type: 'danger', text: '已关闭' },
}

const WORKSHOPS = ['一车间', '二车间', '三车间']
const PRODUCTS = ['AS-05 比赛级羽毛球', 'AS-9 训练球', 'AS-40 国际赛事球', 'E-30 娱乐球']

let nextId = 1

function buildMockOrders(): WorkOrder[] {
  const statuses = Object.keys(ORDER_STATUS)
  return Array.from({ length: 57 }, (_, index) => {
    const day = (index % 28) + 1
    return {
      id: nextId++,
      orderNo: `WO2026${String(7).padStart(2, '0')}${String(day).padStart(2, '0')}${String(index + 1).padStart(3, '0')}`,
      productName: PRODUCTS[index % PRODUCTS.length]!,
      quantity: (index % 9 + 1) * 500,
      workshop: WORKSHOPS[index % WORKSHOPS.length]!,
      status: statuses[index % statuses.length]!,
      planDate: `2026-07-${String(day).padStart(2, '0')}`,
      createdAt: `2026-06-${String(day).padStart(2, '0')} 09:3${index % 10}:00`,
    }
  })
}

const orderDb = buildMockOrders()

interface OrderQuery {
  orderNo?: string
  status?: string
  workshop?: string
  planDate?: [string, string]
}

/** 模拟后端分页接口：筛选 + 分页 + 300ms 延迟 */
async function fetchOrders(params: OrderQuery & PageParam): Promise<PageResult<WorkOrder>> {
  await new Promise((resolve) => setTimeout(resolve, 300))
  let list = orderDb.filter((order) => {
    if (params.orderNo && !order.orderNo.includes(params.orderNo)) return false
    if (params.status && order.status !== params.status) return false
    if (params.workshop && order.workshop !== params.workshop) return false
    if (params.planDate && (order.planDate < params.planDate[0] || order.planDate > params.planDate[1])) {
      return false
    }
    return true
  })
  list = [...list].sort((a, b) => b.id - a.id)
  const start = (params.pageNo - 1) * params.pageSize
  return {
    list: list.slice(start, start + params.pageSize),
    total: list.length,
    pageNo: params.pageNo,
    pageSize: params.pageSize,
  }
}

// ---------- 列表：schema 配置 + useTable ----------

const filterFields: FilterField[] = [
  { prop: 'orderNo', label: '工单号', type: 'input' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: Object.entries(ORDER_STATUS).map(([value, meta]) => ({ label: meta.text, value })),
  },
  {
    prop: 'workshop',
    label: '车间',
    type: 'select',
    options: WORKSHOPS.map((name) => ({ label: name, value: name })),
  },
  { prop: 'planDate', label: '计划日期', type: 'dateRange', span: 8 },
]

const columns: ColumnDef<WorkOrder>[] = [
  { prop: 'orderNo', label: '工单号', width: 170, fixed: 'left' },
  { prop: 'productName', label: '产品', minWidth: 170 },
  {
    prop: 'quantity',
    label: '计划数量（打）',
    width: 130,
    align: 'right',
    formatter: (row) => formatNumber(row.quantity),
  },
  { prop: 'workshop', label: '车间', width: 100 },
  { prop: 'status', label: '状态', width: 100, align: 'center', statusMap: ORDER_STATUS },
  { prop: 'planDate', label: '计划完成', width: 110 },
  { prop: 'createdAt', label: '创建时间', width: 160 },
]

const rowActions: RowAction<WorkOrder>[] = [
  { key: 'view', label: '查看' },
  { key: 'edit', label: '编辑', roles: ['ADMIN', 'PLANNER'] },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: ['ADMIN'],
    confirm: '删除后不可恢复，确认删除该工单？',
    show: (row) => row.status === 'DRAFT',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  WorkOrder,
  OrderQuery
>({ fetcher: fetchOrders })

const selectedRows = ref<WorkOrder[]>([])

// ---------- 表单弹窗：useFormDialog + FormDialog ----------

interface OrderForm {
  id?: number
  orderNo: string
  productName: string
  quantity: number
  workshop: string
  planDate: string
}

const dialog = useFormDialog<OrderForm>(
  () => ({ orderNo: '', productName: '', quantity: 500, workshop: '', planDate: '' }),
  {
    async submit(model, mode) {
      await new Promise((resolve) => setTimeout(resolve, 300))
      if (mode === 'edit') {
        const target = orderDb.find((order) => order.id === model.id)
        if (target) Object.assign(target, model)
      } else {
        orderDb.unshift({
          ...model,
          id: nextId++,
          status: 'DRAFT',
          createdAt: '2026-07-14 10:00:00',
        })
      }
      ElMessage.success(mode === 'edit' ? '保存成功' : '新增成功')
    },
    onSuccess: refresh,
  },
)

const orderRules: FormRules = {
  orderNo: [{ required: true, message: '请输入工单号', trigger: 'blur' }],
  productName: [{ required: true, message: '请选择产品', trigger: 'change' }],
  workshop: [{ required: true, message: '请选择车间', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择计划完成日期', trigger: 'change' }],
}

function handleRowAction(key: string, row: WorkOrder) {
  if (key === 'view') {
    dialog.open('view', row)
  } else if (key === 'edit') {
    dialog.open('edit', row)
  } else if (key === 'delete') {
    const index = orderDb.findIndex((order) => order.id === row.id)
    if (index !== -1) orderDb.splice(index, 1)
    ElMessage.success('删除成功')
    void refresh()
  }
}

function handleExport() {
  ElMessage.info(`模拟导出：已选 ${selectedRows.value.length} 行（未选则导出全部筛选结果）`)
}
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="筛选列表页示例"
      description="FilterTable（FilterBar + ProTable 双 schema 驱动）+ useTable + FormDialog + useFormDialog + 行操作权限"
    />

    <FilterTable
      :columns="columns"
      :filter-fields="filterFields"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="170"
      selectable
      title="生产工单"
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @selection-change="selectedRows = $event"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton
          :roles="['ADMIN', 'PLANNER']"
          type="primary"
          :icon="Plus"
          @click="dialog.open('create')"
        >
          新增工单
        </PermissionButton>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
      </template>
      <!-- #col-{prop} 插槽自定义列 -->
      <template #col-orderNo="{ row, value }">
        <el-link type="primary" :underline="false" @click="dialog.open('view', row as WorkOrder)">
          {{ value }}
        </el-link>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="`${dialog.title.value}工单`"
      :model="dialog.model.value"
      :rules="orderRules"
      :submit-loading="dialog.submitLoading.value"
      :readonly="dialog.readonly.value"
      @submit="dialog.handleSubmit"
    >
      <el-form-item label="工单号" prop="orderNo">
        <el-input v-model="dialog.model.value.orderNo" placeholder="如 WO20260714001" />
      </el-form-item>
      <el-form-item label="产品" prop="productName">
        <el-select v-model="dialog.model.value.productName" placeholder="请选择产品">
          <el-option v-for="name in PRODUCTS" :key="name" :label="name" :value="name" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划数量" prop="quantity">
        <el-input-number
          v-model="dialog.model.value.quantity"
          :min="1"
          :step="100"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item label="车间" prop="workshop">
        <el-select v-model="dialog.model.value.workshop" placeholder="请选择车间">
          <el-option v-for="name in WORKSHOPS" :key="name" :label="name" :value="name" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划完成" prop="planDate">
        <el-date-picker
          v-model="dialog.model.value.planDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
        />
      </el-form-item>
    </FormDialog>
  </div>
</template>
