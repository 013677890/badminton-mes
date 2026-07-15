<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  SopMediaItem,
  StatusMap,
  TouchActionItem,
  TouchCardItem,
  TouchFormField,
} from '@/types/components'
import TouchCardList from '@/components/business/touch/TouchCardList.vue'
import TouchActionButtons from '@/components/business/touch/TouchActionButtons.vue'
import TouchSimpleForm from '@/components/business/touch/TouchSimpleForm.vue'
import TouchSopViewer from '@/components/business/touch/TouchSopViewer.vue'
import TouchScanPage from '@/components/business/touch/TouchScanPage.vue'
import { formatDateTime } from '@/utils/format'

defineOptions({ name: 'TabletDemo' })

type Section = 'tasks' | 'scan' | 'sop'
const section = ref<Section>('tasks')

// ---------- 任务列表：TouchCardList + TouchActionButtons + TouchSimpleForm ----------

const TASK_STATUS: StatusMap = {
  WAITING: { type: 'info', text: '待开工' },
  RUNNING: { type: 'warning', text: '生产中' },
  PAUSED: { type: 'danger', text: '已暂停' },
  DONE: { type: 'success', text: '已完工' },
}

interface TabletTask extends TouchCardItem {
  key: string
  status: string
}

const tasks = ref<TabletTask[]>([
  {
    key: 'WO20260714001',
    title: 'WO20260714001',
    subtitle: 'AS-05 比赛级羽毛球 · 插毛工序',
    status: 'RUNNING',
    fields: [
      { label: '计划数量', value: '500 打' },
      { label: '已报工', value: '320 打' },
      { label: '产线', value: '插毛一线' },
      { label: '计划完成', value: '07-14 17:00' },
    ],
  },
  {
    key: 'WO20260714002',
    title: 'WO20260714002',
    subtitle: 'AS-9 训练球 · 扎线工序',
    status: 'WAITING',
    fields: [
      { label: '计划数量', value: '800 打' },
      { label: '已报工', value: '0 打' },
      { label: '产线', value: '扎线线' },
      { label: '计划完成', value: '07-14 19:00' },
    ],
  },
  {
    key: 'WO20260713005',
    title: 'WO20260713005',
    subtitle: 'AS-40 国际赛事球 · 点胶工序',
    status: 'PAUSED',
    fields: [
      { label: '计划数量', value: '300 打' },
      { label: '已报工', value: '150 打' },
      { label: '产线', value: '点胶线' },
      { label: '暂停原因', value: '待料' },
    ],
  },
  {
    key: 'WO20260713002',
    title: 'WO20260713002',
    subtitle: 'E-30 娱乐球 · 包装工序',
    status: 'DONE',
    disabled: true,
    fields: [
      { label: '计划数量', value: '600 打' },
      { label: '已报工', value: '600 打' },
    ],
  },
])

const selectedKey = ref<string>()
const selectedTask = computed(() => tasks.value.find((task) => task.key === selectedKey.value))

function handleTaskClick(item: TouchCardItem) {
  selectedKey.value = String(item.key)
  showReportForm.value = false
}

/** 按任务状态控制可用动作 */
const taskActions = computed<TouchActionItem[]>(() => {
  const status = selectedTask.value?.status
  return [
    { key: 'start', label: '开工', type: 'primary', disabled: status !== 'WAITING' && status !== 'PAUSED' },
    { key: 'pause', label: '暂停', type: 'warning', disabled: status !== 'RUNNING' },
    { key: 'report', label: '报工', type: 'success', disabled: status !== 'RUNNING' },
    { key: 'finish', label: '完工', type: 'danger', disabled: status !== 'RUNNING' },
  ]
})

const actionLoading = ref<string>()
const showReportForm = ref(false)

async function handleAction(key: string) {
  const task = selectedTask.value
  if (!task) return
  if (key === 'report') {
    showReportForm.value = !showReportForm.value
    return
  }
  actionLoading.value = key
  try {
    await new Promise((resolve) => setTimeout(resolve, 400))
    if (key === 'start') task.status = 'RUNNING'
    if (key === 'pause') task.status = 'PAUSED'
    if (key === 'finish') {
      task.status = 'DONE'
      task.disabled = true
    }
    ElMessage.success(`${task.key} ${TASK_STATUS[task.status]?.text ?? task.status}`)
  } finally {
    actionLoading.value = undefined
  }
}

// ---------- 报工表单：TouchSimpleForm ----------

const reportFields: TouchFormField[] = [
  { prop: 'qualifiedQty', label: '合格数量（打）', type: 'number', required: true },
  { prop: 'defectQty', label: '不良数量（打）', type: 'number' },
  {
    prop: 'defectReason',
    label: '不良原因',
    type: 'select',
    options: [
      { label: '毛片歪斜', value: 'FEATHER_SKEW' },
      { label: '胶水溢出', value: 'GLUE_OVERFLOW' },
      { label: '球头开裂', value: 'CORK_CRACK' },
      { label: '其他', value: 'OTHER' },
    ],
  },
]

const reportModel = reactive<Record<string, any>>({
  qualifiedQty: undefined,
  defectQty: 0,
  defectReason: '',
})

const reportSubmitting = ref(false)

async function handleReportSubmit(model: Record<string, any>) {
  reportSubmitting.value = true
  try {
    await new Promise((resolve) => setTimeout(resolve, 400))
    ElMessage.success(
      `报工成功：合格 ${model.qualifiedQty} 打，不良 ${model.defectQty || 0} 打`,
    )
    showReportForm.value = false
    reportModel.qualifiedQty = undefined
    reportModel.defectQty = 0
    reportModel.defectReason = ''
  } finally {
    reportSubmitting.value = false
  }
}

// ---------- 扫码报工：TouchScanPage ----------

interface ScanRecord {
  code: string
  time: string
  result: string
}

const scanLoading = ref(false)
const scanRecords = ref<ScanRecord[]>([])

async function handleScan(code: string) {
  scanLoading.value = true
  try {
    await new Promise((resolve) => setTimeout(resolve, 500))
    scanRecords.value.unshift({ code, time: formatDateTime(new Date()), result: '报工成功' })
    if (scanRecords.value.length > 6) scanRecords.value.pop()
    ElMessage.success(`条码 ${code} 报工成功`)
  } finally {
    scanLoading.value = false
  }
}

// ---------- SOP：TouchSopViewer ----------

/** 离线可用的占位图（真实场景为工艺文件服务地址） */
function sopImage(step: string, text: string, color: string): string {
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='800' height='450'><rect width='100%' height='100%' fill='${color}'/><text x='50%' y='44%' font-size='56' fill='#fff' text-anchor='middle' font-family='sans-serif'>${step}</text><text x='50%' y='60%' font-size='30' fill='#fff' text-anchor='middle' font-family='sans-serif'>${text}</text></svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

const sopItems: SopMediaItem[] = [
  { type: 'image', title: '毛片分拣', url: sopImage('步骤 1', '按毛片弯度分级，同球同级', '#409eff') },
  { type: 'image', title: '插毛定位', url: sopImage('步骤 2', '16 孔位顺时针插毛，角度 28°', '#67c23a') },
  { type: 'image', title: '注胶固定', url: sopImage('步骤 3', '环形注胶一圈，胶量 0.8g', '#e6a23c') },
  { type: 'doc', title: '《插毛工序作业指导书 V2.3》', url: 'about:blank' },
]
</script>

<template>
  <div class="tablet-demo">
    <el-radio-group v-model="section" size="large" class="tablet-demo__switch">
      <el-radio-button value="tasks">我的任务</el-radio-button>
      <el-radio-button value="scan">扫码报工</el-radio-button>
      <el-radio-button value="sop">SOP 指导</el-radio-button>
    </el-radio-group>

    <!-- 任务列表 -->
    <template v-if="section === 'tasks'">
      <TouchCardList
        :items="tasks"
        :status-map="TASK_STATUS"
        :active-key="selectedKey"
        @item-click="handleTaskClick"
      />

      <el-card v-if="selectedTask" shadow="never" class="tablet-demo__panel">
        <template #header>当前任务：{{ selectedTask.title }}</template>
        <TouchActionButtons
          :actions="taskActions"
          :loading-key="actionLoading"
          :columns="4"
          @action="handleAction"
        />
        <div v-if="showReportForm" class="tablet-demo__report">
          <TouchSimpleForm
            v-model="reportModel"
            :fields="reportFields"
            submit-text="提交报工"
            :submit-loading="reportSubmitting"
            @submit="handleReportSubmit"
          />
        </div>
      </el-card>
      <el-alert
        v-else
        type="info"
        :closable="false"
        title="点击上方任务卡片，进行开工 / 暂停 / 报工 / 完工操作"
        class="tablet-demo__panel"
      />
    </template>

    <!-- 扫码报工 -->
    <el-card v-else-if="section === 'scan'" shadow="never">
      <TouchScanPage
        title="产品序列号扫码报工"
        :min-length="6"
        :loading="scanLoading"
        @scan="handleScan"
      >
        <el-table :data="scanRecords" size="large">
          <el-table-column prop="code" label="条码" min-width="180" />
          <el-table-column prop="result" label="结果" width="140">
            <template #default="{ row }">
              <el-tag type="success" size="large">{{ row.result }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="时间" width="200" />
        </el-table>
      </TouchScanPage>
    </el-card>

    <!-- SOP 指导 -->
    <el-card v-else shadow="never">
      <TouchSopViewer :items="sopItems" title="插毛工序 SOP" />
    </el-card>
  </div>
</template>

<style scoped>
.tablet-demo__switch {
  margin-bottom: 16px;
}

.tablet-demo__panel {
  margin-top: 16px;
}

.tablet-demo__report {
  max-width: 480px;
  margin-top: 16px;
}
</style>
