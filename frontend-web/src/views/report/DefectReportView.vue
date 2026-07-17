<script setup lang="ts">
// 不良报表只读后端聚合结果，筛选条件变化时重新请求并保持图表与表格使用同一份数据。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { loadLineOptions, loadProductOptions, loadWorkshopOptions } from '@/api/production/options'
import type { OptionItem } from '@/types/components'
import {
  DEFECT_VIEW_OPTIONS,
  DEFECT_VIEW_TYPE,
  REPORT_EXPORT_ROLES,
} from '@/constants/report'
import {
  exportDefect,
  getDefectPage,
  getDefectSummary,
} from '@/api/report'
import type { DefectReportSummary, DefectReportDetail, DefectPageParams } from '@/api/report'

defineOptions({ name: 'DefectReportView' })

const queryParams = reactive({
  startTime: '',
  endTime: '',
  workshopId: undefined as number | undefined,
  lineId: undefined as number | undefined,
  productId: undefined as number | undefined,
  view: DEFECT_VIEW_TYPE.SOURCE as string,
})

const workshopOptions = ref<OptionItem[]>([])
const lineOptions = ref<OptionItem[]>([])
const productOptions = ref<OptionItem[]>([])

onMounted(async () => {
  const [workshops, products] = await Promise.all([loadWorkshopOptions(), loadProductOptions()])
  workshopOptions.value = workshops
  productOptions.value = products
})

async function onWorkshopChange() {
  queryParams.lineId = undefined
  lineOptions.value = await loadLineOptions(queryParams.workshopId)
}

const summary = ref<DefectReportSummary | null>(null)
const summaryLoading = ref(false)
const exporting = ref(false)

const { data, loading, pagination, query, onPageChange } = useTable<
  DefectReportDetail,
  DefectPageParams
>({
  fetcher: (params) => getDefectPage(params),
  immediate: false,
})

function validateTimeRange(): boolean {
  if (!queryParams.startTime || !queryParams.endTime) {
    ElMessage.warning('请选择开始时间和结束时间')
    return false
  }
  if (queryParams.startTime > queryParams.endTime) {
    ElMessage.warning('开始时间不能晚于结束时间')
    return false
  }
  return true
}

function buildQuery(): DefectPageParams {
  return {
    startTime: queryParams.startTime,
    endTime: queryParams.endTime,
    workshopId: queryParams.workshopId || undefined,
    lineId: queryParams.lineId || undefined,
    productId: queryParams.productId || undefined,
    view: queryParams.view,
  }
}

async function handleQuery() {
  if (!validateTimeRange()) return
  const req = buildQuery()
  summaryLoading.value = true
  try {
    summary.value = await getDefectSummary(req)
  } finally {
    summaryLoading.value = false
  }
  await query(req)
}

function handleReset() {
  queryParams.startTime = ''
  queryParams.endTime = ''
  queryParams.workshopId = undefined
  queryParams.lineId = undefined
  queryParams.productId = undefined
  queryParams.view = DEFECT_VIEW_TYPE.SOURCE
  summary.value = null
  data.value = []
  pagination.value = { pageNo: 1, pageSize: pagination.value.pageSize, total: 0 }
}

async function handleExport() {
  if (!validateTimeRange()) return
  exporting.value = true
  try {
    await exportDefect(buildQuery())
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function handlePageChange(pageNo: number) {
  onPageChange({ pageNo, pageSize: pagination.value.pageSize })
}

function handleSizeChange(pageSize: number) {
  onPageChange({ pageNo: 1, pageSize })
}

function formatRate(rate?: number): string {
  if (rate === null || rate === undefined || Number.isNaN(rate)) return '--'
  return (rate * 100).toFixed(2)
}
</script>

<template>
  <div class="page">
    <PageHeader title="不良报表" description="聚合报工不良、质检不良和返修复检不良，支持来源明细与综合归并视图" />

    <el-form :model="queryParams" label-width="80px" class="query-form">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-form-item label="开始时间" required>
            <el-date-picker
              v-model="queryParams.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择开始时间"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="结束时间" required>
            <el-date-picker
              v-model="queryParams.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择结束时间"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="车间">
            <el-select
              v-model="queryParams.workshopId"
              clearable
              filterable
              placeholder="全部车间"
              style="width: 100%"
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
        </el-col>
        <el-col :span="6">
          <el-form-item label="产线">
            <el-select
              v-model="queryParams.lineId"
              clearable
              filterable
              placeholder="全部产线"
              style="width: 100%"
            >
              <el-option
                v-for="opt in lineOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="产品">
            <el-select
              v-model="queryParams.productId"
              clearable
              filterable
              placeholder="全部产品"
              style="width: 100%"
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
        <el-col :span="6">
          <el-form-item label="视图类型">
            <el-select v-model="queryParams.view" style="width: 100%">
              <el-option
                v-for="opt in DEFECT_VIEW_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12" class="query-actions">
          <el-button type="primary" :loading="loading || summaryLoading" @click="handleQuery">
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <PermissionButton
            :roles="REPORT_EXPORT_ROLES"
            type="success"
            :loading="exporting"
            @click="handleExport"
          >
            导出
          </PermissionButton>
        </el-col>
      </el-row>
    </el-form>

    <el-row v-loading="summaryLoading" :gutter="12" class="summary-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">现场不良</div>
          <div class="metric-value danger">{{ summary?.sceneDefectQuantity ?? '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">质检不良</div>
          <div class="metric-value danger">{{ summary?.qualityDefectQuantity ?? '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">返修复检不良</div>
          <div class="metric-value danger">{{ summary?.repairRecheckDefectQuantity ?? '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">综合不良</div>
          <div class="metric-value danger">{{ summary?.comprehensiveDefectQuantity ?? '--' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-loading="summaryLoading" :gutter="12" class="summary-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">现场不良率</div>
          <div class="metric-value">{{ formatRate(summary?.sceneDefectRate) }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">综合不良率</div>
          <div class="metric-value">{{ formatRate(summary?.comprehensiveDefectRate) }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">来源记录数</div>
          <div class="metric-value">{{ summary?.sourceRecordCount ?? '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">去重合并数</div>
          <div class="metric-value">{{ summary?.mergedDuplicateCount ?? '--' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      v-if="summary && summary.warnings.length"
      type="warning"
      :closable="false"
      show-icon
      class="warnings-alert"
    >
      <div v-for="(warning, index) in summary.warnings" :key="index">{{ warning }}</div>
    </el-alert>

    <el-table v-loading="loading" :data="data" border stripe class="detail-table">
      <el-table-column label="来源类型" prop="sourceType" width="120" />
      <el-table-column label="归并号" prop="defectGroupNo" width="120" />
      <el-table-column label="任务号" prop="taskNo" width="130" />
      <el-table-column label="工单号" prop="workOrderNo" width="130" />
      <el-table-column label="产品" prop="productName" min-width="140" show-overflow-tooltip />
      <el-table-column label="批次号" prop="batchNo" width="130" />
      <el-table-column label="工序" prop="processName" width="100" />
      <el-table-column label="不良编码" prop="defectCode" width="100" />
      <el-table-column label="不良名称" prop="defectName" min-width="120" show-overflow-tooltip />
      <el-table-column label="净不良数" prop="netQuantity" width="90" align="right" />
      <el-table-column label="发现时间" prop="detectedTime" width="170" />
    </el-table>

    <el-pagination
      :current-page="pagination.pageNo"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
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

.query-actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding-left: 8px;
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

.warnings-alert {
  margin-top: 12px;
}

.detail-table {
  margin-top: 12px;
}

.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
