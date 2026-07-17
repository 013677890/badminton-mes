<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ColumnDef, FilterField, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { loadLineOptions, loadWorkshopOptions } from '@/api/production/options'
import {
  SCENE_TASK_EXEC_ROLES,
  SCENE_TASK_MANAGE_ROLES,
  SCENE_TASK_STATUS,
  SCENE_TASK_STATUS_MAP,
  SCENE_TASK_STATUS_OPTIONS,
} from '@/constants/scene'
import {
  auditSceneTask,
  closeSceneTask,
  getSceneTaskPage,
  getSceneTaskProgress,
  pauseSceneTask,
  releaseSceneTask,
  resumeSceneTask,
  startSceneTask,
} from '@/api/scene/management'
import type { SceneTask, SceneTaskPageParams, SceneTaskProgress } from '@/api/scene/management'

defineOptions({ name: 'SceneTaskList' })

// ---------- 下拉选项 ----------

const filterFields = ref<FilterField[]>([
  { prop: 'taskNo', label: '任务编号', type: 'input' },
  { prop: 'workshopId', label: '车间', type: 'select', options: [] },
  { prop: 'lineId', label: '产线', type: 'select', options: [] },
  { prop: 'taskStatus', label: '任务状态', type: 'select', options: SCENE_TASK_STATUS_OPTIONS },
  { prop: 'planDate', label: '计划日期', type: 'date' },
])

onMounted(async () => {
  try {
    const [workshops, lines] = await Promise.all([loadWorkshopOptions(), loadLineOptions()])
    const wsField = filterFields.value.find((f) => f.prop === 'workshopId')
    const lineField = filterFields.value.find((f) => f.prop === 'lineId')
    if (wsField) wsField.options = workshops
    if (lineField) lineField.options = lines
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

// ---------- 列表 ----------

const columns: ColumnDef<SceneTask>[] = [
  { prop: 'taskNo', label: '任务编号', width: 150, fixed: 'left' },
  { prop: 'workOrderNo', label: '工单号', width: 150 },
  { prop: 'productName', label: '产品', minWidth: 140 },
  { prop: 'batchNo', label: '批次号', width: 130 },
  { prop: 'workshopName', label: '车间', width: 110 },
  { prop: 'lineName', label: '产线', width: 110 },
  { prop: 'planQuantity', label: '计划数', width: 80, align: 'right' },
  { prop: 'inputQuantity', label: '投入数', width: 80, align: 'right' },
  { prop: 'goodQuantity', label: '良品数', width: 80, align: 'right' },
  { prop: 'finishQuantity', label: '完工数', width: 80, align: 'right' },
  { prop: 'taskStatus', label: '状态', width: 90, statusMap: SCENE_TASK_STATUS_MAP },
  { prop: 'planStartTime', label: '计划开始', width: 160 },
  { prop: 'planEndTime', label: '计划完成', width: 160 },
]

const { PENDING_AUDIT, AUDITED, RELEASED, IN_PRODUCTION, PAUSED } = SCENE_TASK_STATUS

const rowActions: RowAction<SceneTask>[] = [
  {
    key: 'audit',
    label: '审核',
    type: 'success',
    roles: SCENE_TASK_MANAGE_ROLES,
    confirm: '确认审核通过该生产任务？',
    show: (row) => row.taskStatus === PENDING_AUDIT,
  },
  {
    key: 'release',
    label: '下达',
    type: 'success',
    roles: SCENE_TASK_MANAGE_ROLES,
    confirm: '下达后可开始派工与生产，确认？',
    show: (row) => row.taskStatus === AUDITED,
  },
  {
    key: 'start',
    label: '开始',
    type: 'primary',
    roles: SCENE_TASK_EXEC_ROLES,
    confirm: '确认开始该生产任务？',
    show: (row) => row.taskStatus === RELEASED,
  },
  {
    key: 'pause',
    label: '暂停',
    type: 'warning',
    roles: SCENE_TASK_EXEC_ROLES,
    show: (row) => row.taskStatus === IN_PRODUCTION,
  },
  {
    key: 'resume',
    label: '恢复',
    type: 'success',
    roles: SCENE_TASK_EXEC_ROLES,
    confirm: '确认恢复该生产任务？',
    show: (row) => row.taskStatus === PAUSED,
  },
  {
    key: 'close',
    label: '关闭',
    type: 'danger',
    roles: SCENE_TASK_MANAGE_ROLES,
    show: (row) => row.taskStatus === IN_PRODUCTION || row.taskStatus === PAUSED,
  },
  { key: 'progress', label: '进度', type: 'info' },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  SceneTask,
  SceneTaskPageParams
>({ fetcher: getSceneTaskPage })

// ---------- 进度弹窗 ----------

const progressVisible = ref(false)
const progressLoading = ref(false)
const progressData = ref<SceneTaskProgress | null>(null)

async function openProgress(row: SceneTask) {
  progressVisible.value = true
  progressLoading.value = true
  try {
    progressData.value = await getSceneTaskProgress(row.id)
  } catch {
    progressData.value = null
  } finally {
    progressLoading.value = false
  }
}

// ---------- 行操作 ----------

async function confirmThen(message: string, action: () => Promise<unknown>, tip: string) {
  try {
    await ElMessageBox.confirm(message, '操作确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await action()
    ElMessage.success(tip)
  } catch {
    // 失败提示由拦截器弹出
  } finally {
    await refresh()
  }
}

async function promptReasonThen(
  title: string,
  action: (reason: string) => Promise<unknown>,
  tip: string,
) {
  let reason: string
  try {
    const result = await ElMessageBox.prompt('请填写操作原因（必填）', title, {
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空',
      inputPlaceholder: '不超过 255 字',
      type: 'warning',
    })
    reason = result.value.trim()
  } catch {
    return
  }
  try {
    await action(reason)
    ElMessage.success(tip)
  } catch {
    // 失败提示由拦截器弹出
  } finally {
    await refresh()
  }
}

function handleRowAction(key: string, row: SceneTask) {
  switch (key) {
    case 'audit':
      return confirmThen('确认审核通过该生产任务？', () => auditSceneTask(row.id), '任务已审核')
    case 'release':
      return confirmThen('下达后可开始派工与生产，确认？', () => releaseSceneTask(row.id), '任务已下达')
    case 'start':
      return confirmThen('确认开始该生产任务？', () => startSceneTask(row.id), '任务已开始')
    case 'pause':
      return promptReasonThen('暂停任务', (reason) => pauseSceneTask(row.id, reason), '任务已暂停')
    case 'resume':
      return confirmThen('确认恢复该生产任务？', () => resumeSceneTask(row.id), '任务已恢复')
    case 'close':
      return promptReasonThen('关闭任务', (reason) => closeSceneTask(row.id, reason), '任务已关闭')
    case 'progress':
      return openProgress(row)
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="生产任务"
      description="状态机：待审核 -> 已审核 -> 已下达 -> 进行中 ⇄ 已暂停 -> 已完工 -> 已关闭"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="230"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    />

    <el-dialog v-model="progressVisible" title="任务进度" width="520px">
      <div v-loading="progressLoading">
        <el-descriptions v-if="progressData" :column="2" border>
          <el-descriptions-item label="任务状态">
            <StatusTag :status="progressData.taskStatus" :status-map="SCENE_TASK_STATUS_MAP" />
          </el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ progressData.planQuantity }}</el-descriptions-item>
          <el-descriptions-item label="工序总数">{{ progressData.operationTotal }}</el-descriptions-item>
          <el-descriptions-item label="已完成工序">{{ progressData.operationCompleted }}</el-descriptions-item>
          <el-descriptions-item label="当前工序" :span="2">
            {{ progressData.currentProcessName || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无进度数据" />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
