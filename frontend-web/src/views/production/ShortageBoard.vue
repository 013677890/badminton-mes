<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import type { ColumnDef, OptionItem, RowAction } from '@/types/components'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import ProTable from '@/components/base/ProTable.vue'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  ROLE_SEED_IDS,
  SHORTAGE_HANDLE_ROLES,
  SHORTAGE_HANDLE_TYPE_OPTIONS,
} from '@/constants/production'
import { loadRoleUserOptions } from '@/api/production/options'
import {
  createShortageHandle,
  getShortageBoard,
  getShortageOrdersByMaterial,
} from '@/api/production/kit'
import type { ShortageBoardRow, ShortageOrderRow } from '@/api/production/kit'

defineOptions({ name: 'ShortageBoard' })

const router = useRouter()

// ---------- 汇总表 ----------

const boardRows = ref<ShortageBoardRow[]>([])
const boardLoading = ref(false)

const boardColumns: ColumnDef<ShortageBoardRow>[] = [
  { prop: 'materialCode', label: '物料编码', width: 150 },
  { prop: 'materialName', label: '物料名称', minWidth: 160 },
  { prop: 'totalShortage', label: '欠料总量', width: 120, align: 'right' },
  { prop: 'affectedOrderCount', label: '影响工单数', width: 110, align: 'right' },
  { prop: 'transitQuantity', label: '在途数量', width: 110, align: 'right' },
  {
    prop: 'expectedArrivalDate',
    label: '最近预计到料',
    width: 130,
    formatter: (row) => row.expectedArrivalDate ?? '未登记',
  },
]

const boardActions: RowAction<ShortageBoardRow>[] = [{ key: 'drill', label: '影响工单' }]

async function loadBoard() {
  boardLoading.value = true
  try {
    boardRows.value = await getShortageBoard()
  } finally {
    boardLoading.value = false
  }
}

onMounted(loadBoard)

// ---------- 下钻抽屉 ----------

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const drillMaterial = ref<ShortageBoardRow>()
const orderRows = ref<ShortageOrderRow[]>([])

const orderColumns: ColumnDef<ShortageOrderRow>[] = [
  { prop: 'workOrderNo', label: '工单号', width: 170 },
  { prop: 'productName', label: '产品', minWidth: 130 },
  { prop: 'requireQuantity', label: '需求', width: 100, align: 'right' },
  { prop: 'availableQuantity', label: '可用', width: 100, align: 'right' },
  { prop: 'shortageQuantity', label: '欠料', width: 100, align: 'right' },
]

const orderActions: RowAction<ShortageOrderRow>[] = [
  { key: 'detail', label: '工单详情' },
  { key: 'handle', label: '登记处理', roles: SHORTAGE_HANDLE_ROLES },
]

async function handleBoardAction(key: string, row: ShortageBoardRow) {
  if (key !== 'drill') return
  drillMaterial.value = row
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    orderRows.value = await getShortageOrdersByMaterial(row.materialId)
  } finally {
    drawerLoading.value = false
  }
}

// ---------- 处理登记 ----------

const handlerOptions = ref<OptionItem[]>([])

interface HandleForm {
  workOrderId?: number
  workOrderNo?: string
  handleType: number
  handlerId: number | null
  expectedArrivalDate: string
  handleRemark: string
}

const handleDialog = useFormDialog<HandleForm>(
  () => ({ handleType: 1, handlerId: null, expectedArrivalDate: '', handleRemark: '' }),
  {
    titles: { create: '登记欠料处理' },
    submit: async (model) => {
      await createShortageHandle({
        workOrderId: model.workOrderId!,
        materialId: drillMaterial.value!.materialId,
        handleType: model.handleType,
        handlerId: model.handlerId!,
        expectedArrivalDate: model.expectedArrivalDate || undefined,
        handleRemark: model.handleRemark || undefined,
      })
      ElMessage.success('处理记录已登记')
    },
    onSuccess: () => void loadBoard(),
  },
)

const handleRules = {
  handleType: [{ required: true, message: '请选择处理方式', trigger: 'change' }],
  handlerId: [{ required: true, message: '请选择责任人', trigger: 'change' }],
}

async function handleOrderAction(key: string, row: ShortageOrderRow) {
  if (key === 'detail') {
    router.push(`/production/work-orders/${row.workOrderId}`)
    return
  }
  if (key === 'handle') {
    if (handlerOptions.value.length === 0) {
      try {
        handlerOptions.value = await loadRoleUserOptions(ROLE_SEED_IDS.PMC)
      } catch {
        // 加载失败仍可打开弹窗
      }
    }
    handleDialog.open('create', { workOrderId: row.workOrderId, workOrderNo: row.workOrderNo })
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="欠料看板"
      description="按物料聚合的欠料汇总（欠料量降序），下钻查看影响工单并登记处理"
    >
      <template #extra>
        <el-button type="primary" :icon="Refresh" :loading="boardLoading" @click="loadBoard">
          刷新
        </el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <ProTable
        :columns="boardColumns"
        :data="boardRows"
        :loading="boardLoading"
        :row-actions="boardActions"
        :action-width="110"
        show-index
        @row-action="handleBoardAction"
      >
        <template #empty>
          <el-empty description="当前无欠料，所有已分析工单均齐套" />
        </template>
      </ProTable>
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      :title="drillMaterial ? `${drillMaterial.materialCode} ${drillMaterial.materialName} · 影响工单` : '影响工单'"
      size="720px"
    >
      <ProTable
        :columns="orderColumns"
        :data="orderRows"
        :loading="drawerLoading"
        :row-actions="orderActions"
        :action-width="170"
        @row-action="handleOrderAction"
      />
    </el-drawer>

    <FormDialog
      v-model:visible="handleDialog.visible.value"
      :title="handleDialog.title.value"
      :model="handleDialog.model.value"
      :rules="handleRules"
      :submit-loading="handleDialog.submitLoading.value"
      width="520px"
      @submit="handleDialog.handleSubmit"
    >
      <el-form-item label="工单">
        <el-input :model-value="handleDialog.model.value.workOrderNo" disabled />
      </el-form-item>
      <el-form-item label="欠料物料">
        <el-input
          :model-value="drillMaterial ? `${drillMaterial.materialCode} ${drillMaterial.materialName}` : ''"
          disabled
        />
      </el-form-item>
      <el-form-item label="处理方式" prop="handleType">
        <el-radio-group v-model="handleDialog.model.value.handleType">
          <el-radio v-for="opt in SHORTAGE_HANDLE_TYPE_OPTIONS" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="责任人" prop="handlerId">
        <el-select v-model="handleDialog.model.value.handlerId" filterable placeholder="PMC 计划员">
          <el-option
            v-for="opt in handlerOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="预计到料" prop="expectedArrivalDate">
        <el-date-picker
          v-model="handleDialog.model.value.expectedArrivalDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选填"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="处理说明" prop="handleRemark">
        <el-input
          v-model="handleDialog.model.value.handleRemark"
          type="textarea"
          :rows="2"
          maxlength="255"
          show-word-limit
          placeholder="选填"
        />
      </el-form-item>
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
</style>
