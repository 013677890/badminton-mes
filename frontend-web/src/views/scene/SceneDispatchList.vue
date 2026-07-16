<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import {
  SCENE_DISPATCH_ROLES,
  SCENE_DISPATCH_STATUS,
  SCENE_DISPATCH_STATUS_MAP,
  SCENE_DISPATCH_STATUS_OPTIONS,
  SCENE_OPERATION_STATUS_MAP,
} from '@/constants/scene'
import {
  cancelDispatchOrder,
  confirmDispatchOrder,
  generateDispatchOrder,
  getDispatchOperations,
  getDispatchOrderPage,
} from '@/api/scene/management'
import type {
  SceneDispatchDetail,
  SceneDispatchOrder,
  SceneDispatchPageParams,
} from '@/api/scene/management'

defineOptions({ name: 'SceneDispatchList' })

const { PENDING_CONFIRM, CONFIRMED } = SCENE_DISPATCH_STATUS

// ---------- 列表 ----------

const filterFields: FilterField[] = [
  { prop: 'dispatchNo', label: '派工单号', type: 'input' },
  { prop: 'taskId', label: '任务 ID', type: 'input' },
  { prop: 'dispatchStatus', label: '派工状态', type: 'select', options: SCENE_DISPATCH_STATUS_OPTIONS },
]

const columns: ColumnDef<SceneDispatchOrder>[] = [
  { prop: 'dispatchNo', label: '派工单号', width: 170, fixed: 'left' },
  { prop: 'taskId', label: '任务 ID', width: 100 },
  { prop: 'routingCode', label: '工艺路线编码', width: 140 },
  { prop: 'routingVersion', label: '路线版本', width: 100 },
  { prop: 'dispatchStatus', label: '状态', width: 100, statusMap: SCENE_DISPATCH_STATUS_MAP },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const rowActions: RowAction<SceneDispatchOrder>[] = [
  { key: 'detail', label: '查看明细', type: 'info' },
  {
    key: 'confirm',
    label: '确认',
    type: 'success',
    roles: SCENE_DISPATCH_ROLES,
    confirm: '确认该派工单？确认后工序明细可执行',
    show: (row) => row.dispatchStatus === PENDING_CONFIRM,
  },
  {
    key: 'cancel',
    label: '取消',
    type: 'danger',
    roles: SCENE_DISPATCH_ROLES,
    confirm: '取消后不可恢复，确认取消该派工单？',
    show: (row) => row.dispatchStatus === PENDING_CONFIRM || row.dispatchStatus === CONFIRMED,
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  SceneDispatchOrder,
  SceneDispatchPageParams
>({ fetcher: getDispatchOrderPage })

// ---------- 工序明细弹窗 ----------

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<SceneDispatchDetail[]>([])
const detailDispatchNo = ref('')

const detailColumns: ColumnDef<SceneDispatchDetail>[] = [
  { prop: 'seq', label: '序号', width: 60, align: 'center' },
  { prop: 'processCode', label: '工序编码', width: 120 },
  { prop: 'processName', label: '工序名称', minWidth: 120 },
  {
    prop: 'keyProcess',
    label: '关键工序',
    width: 90,
    align: 'center',
    formatter: (row) => (row.keyProcess ? '是' : '否'),
  },
  {
    prop: 'inspect',
    label: '需检验',
    width: 80,
    align: 'center',
    formatter: (row) => (row.inspect ? '是' : '否'),
  },
  { prop: 'planQuantity', label: '计划数', width: 80, align: 'right' },
  { prop: 'detailStatus', label: '作业状态', width: 100, statusMap: SCENE_OPERATION_STATUS_MAP },
  {
    prop: 'paused',
    label: '暂停',
    width: 70,
    align: 'center',
    formatter: (row) => (row.paused ? '是' : '否'),
  },
  { prop: 'actualStartTime', label: '实际开始', width: 160 },
  { prop: 'actualEndTime', label: '实际完成', width: 160 },
]

async function openDetail(row: SceneDispatchOrder) {
  detailDispatchNo.value = row.dispatchNo
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await getDispatchOperations(row.id)
  } catch {
    detailData.value = []
  } finally {
    detailLoading.value = false
  }
}

// ---------- 生成派工 ----------

async function handleGenerate() {
  let taskIdStr: string
  try {
    const result = await ElMessageBox.prompt('请输入生产任务 ID', '生成派工', {
      inputPattern: /^[1-9]\d*$/,
      inputErrorMessage: '请输入正整数任务 ID',
      type: 'info',
    })
    taskIdStr = result.value.trim()
  } catch {
    return
  }
  try {
    const id = await generateDispatchOrder(Number(taskIdStr))
    ElMessage.success(`派工单 #${id} 已生成`)
    await refresh()
  } catch {
    // 失败提示由拦截器弹出
  }
}

// ---------- 行操作 ----------

async function handleRowAction(key: string, row: SceneDispatchOrder) {
  if (key === 'detail') {
    return openDetail(row)
  }
  if (key === 'confirm') {
    try {
      await confirmDispatchOrder(row.id)
      ElMessage.success('派工单已确认')
    } catch {
      return
    }
    await refresh()
    return
  }
  if (key === 'cancel') {
    try {
      await cancelDispatchOrder(row.id)
      ElMessage.success('派工单已取消')
    } catch {
      return
    }
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="派工管理" description="按生产任务生成派工单，确认后工序明细可执行现场作业" />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="200"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="SCENE_DISPATCH_ROLES" type="primary" @click="handleGenerate">
          生成派工
        </PermissionButton>
      </template>
    </FilterTable>

    <el-dialog v-model="detailVisible" :title="`工序明细 - ${detailDispatchNo}`" width="900px">
      <el-table v-loading="detailLoading" :data="detailData" border stripe>
        <el-table-column
          v-for="col in detailColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align ?? 'left'"
        >
          <template #default="scope">
            <StatusTag
              v-if="col.statusMap"
              :status="scope.row[col.prop]"
              :status-map="col.statusMap"
            />
            <template v-else-if="col.formatter">{{ col.formatter(scope.row) }}</template>
            <template v-else>{{ scope.row[col.prop] ?? '-' }}</template>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
