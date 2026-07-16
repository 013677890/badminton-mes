<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import {
  getRealtimeOverview,
  getRealtimeTasks,
} from '@/api/report'
import type {
  RealtimeProductionOverview,
  RealtimeProductionTask,
} from '@/api/report'
import { loadLineOptions, loadProductOptions, loadWorkshopOptions } from '@/api/production/options'
import type { OptionItem } from '@/types/components'
import { useAutoRefresh } from '@/composables/useAutoRefresh'

defineOptions({ name: 'RealtimeProductionView' })

const queryParams = reactive({
  workshopId: undefined as number | undefined,
  lineId: undefined as number | undefined,
  productId: undefined as number | undefined,
})

const workshopOptions = ref<OptionItem[]>([])
const lineOptions = ref<OptionItem[]>([])
const productOptions = ref<OptionItem[]>([])

const overview = ref<RealtimeProductionOverview | null>(null)
const tasks = ref<RealtimeProductionTask[]>([])
const loading = ref(false)

onMounted(async () => {
  const [workshops, products] = await Promise.all([loadWorkshopOptions(), loadProductOptions()])
  workshopOptions.value = workshops
  productOptions.value = products
})

async function onWorkshopChange() {
  queryParams.lineId = undefined
  lineOptions.value = await loadLineOptions(queryParams.workshopId)
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      workshopId: queryParams.workshopId || undefined,
      lineId: queryParams.lineId || undefined,
      productId: queryParams.productId || undefined,
    }
    const [overviewData, tasksData] = await Promise.all([
      getRealtimeOverview(params),
      getRealtimeTasks(params),
    ])
    overview.value = overviewData
    tasks.value = tasksData
  } finally {
    loading.value = false
  }
}

const { lastUpdated, paused, pause, resume } = useAutoRefresh(fetchData, 30000)

function handleQuery() {
  fetchData()
}

function handleReset() {
  queryParams.workshopId = undefined
  queryParams.lineId = undefined
  queryParams.productId = undefined
  overview.value = null
  tasks.value = []
}

function toggleAutoRefresh(value: boolean) {
  if (value) {
    resume()
  } else {
    pause()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="实时生产" description="车间在制任务、设备运行和安灯异常的实时总览" />

    <el-form :model="queryParams" label-width="80px" class="query-form" inline>
      <el-form-item label="车间">
        <el-select
          v-model="queryParams.workshopId"
          clearable
          filterable
          placeholder="全部车间"
          @change="onWorkshopChange"
        >
          <el-option
            v-for="opt in workshopOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产线">
        <el-select
          v-model="queryParams.lineId"
          clearable
          filterable
          placeholder="全部产线"
        >
          <el-option
            v-for="opt in lineOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产品">
        <el-select
          v-model="queryParams.productId"
          clearable
          filterable
          placeholder="全部产品"
        >
          <el-option
            v-for="opt in productOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-switch
          :model-value="!paused"
          inline-prompt
          active-text="自动刷新"
          @change="toggleAutoRefresh"
        />
        <span v-if="lastUpdated" class="refresh-time">
          最后刷新: {{ lastUpdated.toLocaleTimeString('zh-CN', { hour12: false }) }}
        </span>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="overview && overview.warnings.length"
      type="warning"
      :closable="false"
      show-icon
      class="warnings-alert"
    >
      <div v-for="(warning, index) in overview.warnings" :key="index">{{ warning }}</div>
    </el-alert>

    <div v-loading="loading">
      <el-row v-if="overview" :gutter="12" class="summary-row">
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">在制任务</div>
            <div class="metric-value">{{ overview.activeTaskCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">暂停任务</div>
            <div class="metric-value">{{ overview.pausedTaskCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">异常批次</div>
            <div class="metric-value danger">{{ overview.abnormalBatchCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">计划数量</div>
            <div class="metric-value">{{ overview.planQuantity }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">投入数量</div>
            <div class="metric-value">{{ overview.inputQuantity }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">合格数量</div>
            <div class="metric-value">{{ overview.goodQuantity }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row v-if="overview" :gutter="12" class="summary-row">
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">不良数量</div>
            <div class="metric-value danger">{{ overview.defectQuantity }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">设备总数</div>
            <div class="metric-value">{{ overview.equipmentTotalCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">运行设备</div>
            <div class="metric-value">{{ overview.runningEquipmentCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">不可用设备</div>
            <div class="metric-value danger">{{ overview.unavailableEquipmentCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">未关闭安灯</div>
            <div class="metric-value danger">{{ overview.openAndonCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-label">严重安灯</div>
            <div class="metric-value danger">{{ overview.criticalAndonCount }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-divider content-position="left">在制任务列表</el-divider>

      <el-table :data="tasks" border stripe>
        <el-table-column label="任务号" prop="taskNo" width="130" />
        <el-table-column label="工单号" prop="workOrderNo" width="130" />
        <el-table-column label="产品名称" prop="productName" min-width="160" show-overflow-tooltip />
        <el-table-column label="批次号" prop="batchNo" width="130" />
        <el-table-column label="车间" prop="workshopName" width="100" />
        <el-table-column label="产线" prop="lineName" width="100" />
        <el-table-column label="计划" prop="planQuantity" width="70" align="right" />
        <el-table-column label="投入" prop="inputQuantity" width="70" align="right" />
        <el-table-column label="合格" prop="goodQuantity" width="70" align="right" />
        <el-table-column label="不良" prop="defectQuantity" width="70" align="right" />
        <el-table-column label="完工" prop="finishQuantity" width="70" align="right" />
        <el-table-column label="异常" width="60" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.abnormal" type="danger" size="small">是</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" prop="actualStartTime" width="170" />
        <el-table-column label="更新时间" prop="updateTime" width="170" />
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.page {
  padding: 0 0 16px;
}

.query-form {
  padding: 16px;
  background: var(--el-bg-color);
  border-radius: 4px;
}

.refresh-time {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.warnings-alert {
  margin-top: 12px;
}

.summary-row {
  margin-bottom: 12px;
}

.metric-card {
  text-align: center;
}

.metric-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.metric-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.metric-value.danger {
  color: var(--el-color-danger);
}
</style>
