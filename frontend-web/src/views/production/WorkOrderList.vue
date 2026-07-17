<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { useWorkOrderActions } from '@/composables/useWorkOrderActions'
import {
  KIT_STATUS_MAP,
  PRIORITY_OPTIONS,
  WORK_ORDER_STATUS_MAP,
  WO_EXEC_ROLES,
  WO_PLAN_ROLES,
  statusMapToOptions,
} from '@/constants/production'
import {
  loadEffectiveBomOptions,
  loadEffectiveRouteOptions,
  loadWorkshopOptions,
} from '@/api/production/options'
import { getDefaultRoute } from '@/api/production/craftRoute'
import { createWorkOrder, getWorkOrderPage, updateWorkOrder } from '@/api/production/workOrder'
import type { WorkOrder, WorkOrderPageParams } from '@/api/production/workOrder'
import { loadProductOptions } from '@/api/production/options'

defineOptions({ name: 'WorkOrderList' })

const router = useRouter()

// ---------- 下拉选项 ----------

const workshopOptions = ref<OptionItem[]>([])
const productOptions = ref<OptionItem[]>([])
const routeOptions = ref<OptionItem[]>([])
const bomOptions = ref<OptionItem[]>([])

const filterFields = ref<FilterField[]>([
  { prop: 'workOrderNo', label: '工单号', type: 'input' },
  { prop: 'workshopId', label: '车间', type: 'select', options: [] },
  {
    prop: 'orderStatus',
    label: '工单状态',
    type: 'select',
    options: statusMapToOptions(WORK_ORDER_STATUS_MAP),
  },
  { prop: 'planEndRange', label: '计划完成', type: 'dateRange' },
])

onMounted(async () => {
  try {
    const [workshops, products, routes] = await Promise.all([
      loadWorkshopOptions(),
      loadProductOptions(),
      loadEffectiveRouteOptions(),
    ])
    workshopOptions.value = workshops
    productOptions.value = products
    routeOptions.value = routes
    const field = filterFields.value.find((item) => item.prop === 'workshopId')
    if (field) field.options = workshops
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

/** 根据车间主键转换为页面展示名称，找不到档案时回退显示主键。 */
function workshopLabel(workshopId: number): string {
  return String(workshopOptions.value.find((opt) => opt.value === workshopId)?.label ?? workshopId)
}

// ---------- 列表 ----------

const columns: ColumnDef<WorkOrder>[] = [
  { prop: 'workOrderNo', label: '工单号', width: 170, fixed: 'left' },
  { prop: 'productName', label: '产品', minWidth: 140 },
  { prop: 'workshopId', label: '车间', width: 130, formatter: (row) => workshopLabel(row.workshopId) },
  { prop: 'planQuantity', label: '计划数', width: 90, align: 'right' },
  { prop: 'progress', label: '进度（完工/计划）', width: 150 },
  { prop: 'priority', label: '优先级', width: 80, align: 'center' },
  { prop: 'planStartTime', label: '计划开始', width: 160 },
  { prop: 'planEndTime', label: '计划完成', width: 160 },
  { prop: 'orderStatus', label: '状态', width: 90, statusMap: WORK_ORDER_STATUS_MAP },
  { prop: 'kitStatus', label: '齐套', width: 96, statusMap: KIT_STATUS_MAP },
]

const rowActions: RowAction<WorkOrder>[] = [
  { key: 'detail', label: '详情' },
  { key: 'edit', label: '编辑', roles: WO_PLAN_ROLES, show: (row) => row.orderStatus === 0 },
  { key: 'release', label: '下达', type: 'success', roles: WO_PLAN_ROLES, show: (row) => row.orderStatus === 0 },
  {
    key: 'pause',
    label: '暂停',
    type: 'warning',
    roles: WO_EXEC_ROLES,
    show: (row) => row.orderStatus === 1 || row.orderStatus === 2,
  },
  { key: 'resume', label: '恢复', type: 'success', roles: WO_EXEC_ROLES, show: (row) => row.orderStatus === 3 },
  {
    key: 'finish',
    label: '完工',
    type: 'success',
    roles: WO_EXEC_ROLES,
    show: (row) => row.orderStatus === 1 || row.orderStatus === 2,
  },
  { key: 'close', label: '关闭', roles: WO_PLAN_ROLES, show: (row) => row.orderStatus === 4 },
  {
    key: 'cancel',
    label: '作废',
    type: 'danger',
    roles: WO_PLAN_ROLES,
    show: (row) => row.orderStatus === 0 || row.orderStatus === 1,
  },
  { key: 'remove', label: '删除', type: 'danger', roles: WO_PLAN_ROLES, show: (row) => row.orderStatus === 0 },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  WorkOrder,
  WorkOrderPageParams
>({ fetcher: getWorkOrderPage })

/** dateRange 拆为后端的 begin/end 字段（补齐时分秒覆盖整天） */
/**
 * 将页面日期范围拆成后端要求的起止时间，并触发工单分页查询。
 */
function handleQuery(params: Record<string, any>) {
  const { planEndRange, ...rest } = params
  if (Array.isArray(planEndRange) && planEndRange.length === 2) {
    rest.planEndTimeBegin = `${planEndRange[0]} 00:00:00`
    rest.planEndTimeEnd = `${planEndRange[1]} 23:59:59`
  }
  query(rest as WorkOrderPageParams)
}

// ---------- 新建 / 编辑弹窗 ----------

interface WorkOrderForm {
  id?: number
  productId: number | null
  workshopId: number | null
  batchNo: string
  bomId: number | null
  routingId: number | null
  planQuantity: number
  overRatio: number | null
  priority: number
  planStartTime: string
  planEndTime: string
  changeReason: string
}

const dialog = useFormDialog<WorkOrderForm>(
  () => ({
    productId: null,
    workshopId: null,
    batchNo: '',
    bomId: null,
    routingId: null,
    planQuantity: 1,
    overRatio: null,
    priority: 5,
    planStartTime: '',
    planEndTime: '',
    changeReason: '',
  }),
  {
    titles: { create: '新建工单', edit: '编辑工单（仅已创建可改）' },
    submit: async (model, mode) => {
      // 前端先阻止明显的时间倒置，后端仍会再次校验，避免绕过页面直接调用接口。
      if (model.planEndTime <= model.planStartTime) {
        ElMessage.warning('计划完成时间必须晚于计划开始时间')
        throw new Error('invalid plan time')
      }
      const payload = {
        // 表单模型允许 null 便于控件清空；接口 payload 将可选字段转换为 undefined，避免提交无意义的 null。
        productId: model.productId!,
        workshopId: model.workshopId!,
        batchNo: model.batchNo || undefined,
        bomId: model.bomId ?? undefined,
        routingId: model.routingId ?? undefined,
        planQuantity: model.planQuantity,
        overRatio: model.overRatio ?? undefined,
        priority: model.priority,
        planStartTime: model.planStartTime,
        planEndTime: model.planEndTime,
        changeReason: model.changeReason || undefined,
      }
      if (mode === 'create') {
        // 新建只提交一次创建请求，成功后由 useFormDialog 的 onSuccess 触发列表刷新。
        await createWorkOrder(payload)
        ElMessage.success('工单已创建')
      } else {
        // 编辑复用同一 payload；工单当前状态能否修改由后端状态机最终判断。
        await updateWorkOrder(model.id!, payload)
        ElMessage.success('工单已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  workshopId: [{ required: true, message: '请选择车间', trigger: 'change' }],
  planQuantity: [{ required: true, message: '请输入计划数量', trigger: 'blur' }],
  planStartTime: [{ required: true, message: '请选择计划开始时间', trigger: 'change' }],
  planEndTime: [{ required: true, message: '请选择计划完成时间', trigger: 'change' }],
}

/** 产品切换：联动生效 BOM 列表，并尝试预选默认工艺路线 */
async function handleProductChange(productId: number | null) {
  // 先清空旧产品的 BOM，避免异步请求期间用户误提交上一个产品的物料结构。
  dialog.model.value.bomId = null
  bomOptions.value = []
  if (!productId) return
  try {
    // 生效 BOM 列表是产品维度数据；只有唯一候选时才自动回填，多个候选交给用户选择。
    bomOptions.value = await loadEffectiveBomOptions(productId)
    if (bomOptions.value.length === 1) {
      dialog.model.value.bomId = bomOptions.value[0].value as number
    }
  } catch {
    // BOM 未配置时留空
  }
  try {
    // 默认工艺路线是便利性回填，查询失败不阻塞工单创建，用户仍可手工选择路线。
    const route = await getDefaultRoute(productId)
    dialog.model.value.routingId = route.id
  } catch {
    // 未配置默认路线则由用户手选
  }
}

function openEdit(row: WorkOrder) {
  // 将列表行转换为弹窗模型，空值转为空字符串以适配输入控件。
  dialog.open('edit', {
    id: row.id,
    productId: row.productId,
    workshopId: row.workshopId,
    batchNo: row.batchNo ?? '',
    bomId: row.bomId,
    routingId: row.routingId,
    planQuantity: row.planQuantity,
    overRatio: row.overRatio,
    priority: row.priority ?? 5,
    planStartTime: row.planStartTime,
    planEndTime: row.planEndTime,
    changeReason: '',
  })
  // 编辑时按当前产品恢复 BOM 选项
  void loadEffectiveBomOptions(row.productId)
    .then((options) => {
      bomOptions.value = options
    })
    .catch(() => {
      bomOptions.value = []
    })
}

// ---------- 行操作 ----------

const actions = useWorkOrderActions(refresh)

function handleRowAction(key: string, row: WorkOrder) {
  // 行操作由 key 分发到统一 action；每个分支只负责导航或触发对应业务动作。
  switch (key) {
    case 'detail':
      // 详情页使用路由传递主键，详情页再按主键读取最新数据，避免依赖列表快照。
      router.push(`/production/work-orders/${row.id}`)
      break
    case 'edit':
      openEdit(row)
      break
    case 'release':
      void actions.release(row.id)
      break
    case 'pause':
      void actions.pause(row.id)
      break
    case 'resume':
      void actions.resume(row.id)
      break
    case 'finish':
      void actions.finish(row.id)
      break
    case 'close':
      void actions.close(row.id)
      break
    case 'cancel':
      void actions.cancel(row.id)
      break
    case 'remove':
      void actions.remove(row.id)
      break
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="生产工单"
      description="状态机：已创建 → 已下达 → 生产中 ⇄ 暂停 → 已完工 → 已关闭；仅已创建可编辑/删除"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="230"
      @query="handleQuery"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="WO_PLAN_ROLES" type="primary" @click="dialog.open()">
          新建工单
        </PermissionButton>
      </template>

      <template #col-progress="{ row }">
        <div class="progress-cell">
          <el-progress
            :percentage="row.planQuantity > 0 ? Math.min(100, Math.round((row.finishQuantity / row.planQuantity) * 100)) : 0"
            :stroke-width="8"
            class="progress-cell__bar"
          />
          <span class="progress-cell__text">{{ row.finishQuantity }}/{{ row.planQuantity }}</span>
        </div>
      </template>
    </FilterTable>

    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      width="720px"
      @submit="dialog.handleSubmit"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="产品" prop="productId">
            <el-select
              v-model="dialog.model.value.productId"
              filterable
              placeholder="请选择产品"
              @change="handleProductChange"
            >
              <el-option
                v-for="opt in productOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="车间" prop="workshopId">
            <el-select v-model="dialog.model.value.workshopId" filterable placeholder="请选择车间">
              <el-option
                v-for="opt in workshopOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="BOM" prop="bomId">
            <el-select
              v-model="dialog.model.value.bomId"
              clearable
              placeholder="产品的生效 BOM（选填）"
              :disabled="!dialog.model.value.productId"
            >
              <el-option
                v-for="opt in bomOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="工艺路线" prop="routingId">
            <el-select
              v-model="dialog.model.value.routingId"
              clearable
              filterable
              placeholder="生效路线（选产品后自动带默认）"
            >
              <el-option
                v-for="opt in routeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计划数量" prop="planQuantity">
            <el-input-number
              v-model="dialog.model.value.planQuantity"
              :min="1"
              :step="100"
              controls-position="right"
              class="full-width"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="超产比例" prop="overRatio">
            <el-input-number
              v-model="dialog.model.value.overRatio"
              :min="0"
              :max="999.99"
              :precision="2"
              :step="1"
              controls-position="right"
              placeholder="%（选填）"
              class="full-width"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="dialog.model.value.priority">
              <el-option
                v-for="opt in PRIORITY_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="批次号" prop="batchNo">
            <el-input v-model="dialog.model.value.batchNo" maxlength="64" placeholder="选填" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计划开始" prop="planStartTime">
            <el-date-picker
              v-model="dialog.model.value.planStartTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择日期时间"
              class="full-width"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计划完成" prop="planEndTime">
            <el-date-picker
              v-model="dialog.model.value.planEndTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择日期时间"
              class="full-width"
            />
          </el-form-item>
        </el-col>
        <el-col v-if="dialog.mode.value === 'edit'" :span="24">
          <el-form-item label="变更原因" prop="changeReason">
            <el-input
              v-model="dialog.model.value.changeReason"
              type="textarea"
              :rows="2"
              maxlength="255"
              show-word-limit
              placeholder="记录本次计划变更原因（选填），将写入状态日志"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}

.progress-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.progress-cell__bar {
  flex: 1;
}

.progress-cell__bar :deep(.el-progress__text) {
  display: none;
}

.progress-cell__text {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
