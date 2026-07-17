<script setup lang="ts">
// 车间期间报表按时间、车间和产线查询后端聚合结果，不在浏览器重新计算业务指标。
import { computed } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useProductionStyleReport, buildProductionSummaryMetrics } from '@/composables/useProductionStyleReport'
import { REPORT_EXPORT_ROLES, REPORT_RECORD_TYPE_MAP } from '@/constants/report'
import {
  exportWorkshopPeriod,
  getWorkshopPeriodDetails,
  getWorkshopPeriodSummary,
} from '@/api/report'

defineOptions({ name: 'WorkshopPeriodReport' })

const {
  queryParams,
  workshopOptions,
  lineOptions,
  productOptions,
  onWorkshopChange,
  summary,
  summaryLoading,
  data,
  loading,
  pagination,
  onPageChange,
  exporting,
  handleQuery,
  handleReset,
  handleExport,
} = useProductionStyleReport({
  summary: getWorkshopPeriodSummary,
  details: getWorkshopPeriodDetails,
  exportFile: exportWorkshopPeriod,
})

const metrics = computed(() =>
  summary.value ? buildProductionSummaryMetrics(summary.value) : [],
)

function handlePageChange(pageNo: number) {
  onPageChange({ pageNo, pageSize: pagination.value.pageSize })
}
function handleSizeChange(pageSize: number) {
  onPageChange({ pageNo: 1, pageSize })
}
</script>

<template>
  <div class="page">
    <PageHeader title="车间时段报表" description="按车间和时段汇总报工净额，支持明细下钻与导出" />

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
          <el-form-item label="批次号">
            <el-input v-model="queryParams.batchNo" maxlength="64" clearable placeholder="批次号" />
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
      <el-col v-for="metric in metrics" :key="metric.label" :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">{{ metric.label }}</div>
          <div class="metric-value">
            {{ metric.value }}<span v-if="metric.unit" class="metric-unit">{{ metric.unit }}</span>
          </div>
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
      <el-table-column label="报工单号" prop="reportNo" width="150" />
      <el-table-column label="任务号" prop="taskNo" width="130" />
      <el-table-column label="工单号" prop="workOrderNo" width="130" />
      <el-table-column label="产品名称" prop="productName" min-width="160" show-overflow-tooltip />
      <el-table-column label="批次号" prop="batchNo" width="130" />
      <el-table-column label="车间" prop="workshopName" width="100" />
      <el-table-column label="产线" prop="lineName" width="100" />
      <el-table-column label="工序" prop="processName" width="100" />
      <el-table-column label="记录类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag
            v-if="row.recordType != null"
            :type="REPORT_RECORD_TYPE_MAP[row.recordType]?.type"
            size="small"
          >
            {{ REPORT_RECORD_TYPE_MAP[row.recordType]?.text }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="净投入" prop="netInputQuantity" width="90" align="right" />
      <el-table-column label="净合格" prop="netGoodQuantity" width="90" align="right" />
      <el-table-column label="净不良" prop="netDefectQuantity" width="90" align="right" />
      <el-table-column label="报工时间" prop="reportTime" width="170" />
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
  margin-top: 12px;
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

.metric-unit {
  margin-left: 2px;
  font-size: 14px;
  font-weight: 400;
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
