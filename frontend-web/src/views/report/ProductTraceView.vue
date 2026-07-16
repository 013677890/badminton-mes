<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import { getProductTrace } from '@/api/report'
import type { ProductTraceRespVO, ProductTraceQueryReq } from '@/api/report'
import { DATA_COMPLETENESS_MAP, TRACE_SOURCE_TYPE_TEXT } from '@/constants/report'

defineOptions({ name: 'ProductTraceView' })

const queryParams = ref<ProductTraceQueryReq>({
  batchCode: '',
  barcodeValue: '',
  workOrderNo: '',
  taskNo: '',
})

const traceData = ref<ProductTraceRespVO | null>(null)
const loading = ref(false)

async function handleQuery() {
  const hasKey =
    queryParams.value.batchCode ||
    queryParams.value.barcodeValue ||
    queryParams.value.workOrderNo ||
    queryParams.value.taskNo
  if (!hasKey) {
    ElMessage.warning('请至少输入一个业务键（批次号、条码值、工单号或任务号）')
    return
  }
  loading.value = true
  try {
    traceData.value = await getProductTrace({
      batchCode: queryParams.value.batchCode?.trim() || undefined,
      barcodeValue: queryParams.value.barcodeValue?.trim() || undefined,
      workOrderNo: queryParams.value.workOrderNo?.trim() || undefined,
      taskNo: queryParams.value.taskNo?.trim() || undefined,
    })
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryParams.value = { batchCode: '', barcodeValue: '', workOrderNo: '', taskNo: '' }
  traceData.value = null
}

const activeTab = ref('task')
</script>

<template>
  <div class="page">
    <PageHeader title="产品追溯" description="按批次号、条码值、工单号或任务号查询产品全链路追溯信息" />

    <el-form :model="queryParams" label-width="80px" class="query-form" inline>
      <el-form-item label="批次号">
        <el-input v-model="queryParams.batchCode" maxlength="64" clearable placeholder="批次号" />
      </el-form-item>
      <el-form-item label="条码值">
        <el-input v-model="queryParams.barcodeValue" maxlength="128" clearable placeholder="条码值" />
      </el-form-item>
      <el-form-item label="工单号">
        <el-input v-model="queryParams.workOrderNo" maxlength="32" clearable placeholder="工单号" />
      </el-form-item>
      <el-form-item label="任务号">
        <el-input v-model="queryParams.taskNo" maxlength="32" clearable placeholder="任务号" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="traceData && traceData.warnings.length"
      type="warning"
      :closable="false"
      show-icon
      class="warnings-alert"
    >
      <div v-for="(warning, index) in traceData.warnings" :key="index">{{ warning }}</div>
    </el-alert>

    <div v-if="traceData" v-loading="loading" class="trace-result">
      <div class="completeness">
        数据完整度：
        <el-tag :type="DATA_COMPLETENESS_MAP[traceData.dataCompleteness ?? 'EMPTY']?.type">
          {{ DATA_COMPLETENESS_MAP[traceData.dataCompleteness ?? 'EMPTY']?.text }}
        </el-tag>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="任务信息" name="task">
          <el-descriptions v-if="traceData.task" :column="3" border>
            <el-descriptions-item label="任务号">{{ traceData.task.taskNo }}</el-descriptions-item>
            <el-descriptions-item label="产品编码">{{ traceData.task.productCode }}</el-descriptions-item>
            <el-descriptions-item label="产品名称">{{ traceData.task.productName }}</el-descriptions-item>
            <el-descriptions-item label="批次号">{{ traceData.task.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="车间">{{ traceData.task.workshopName }}</el-descriptions-item>
            <el-descriptions-item label="产线">{{ traceData.task.lineName }}</el-descriptions-item>
            <el-descriptions-item label="计划数量">{{ traceData.task.planQuantity }}</el-descriptions-item>
            <el-descriptions-item label="投入数量">{{ traceData.task.inputQuantity }}</el-descriptions-item>
            <el-descriptions-item label="合格数量">{{ traceData.task.goodQuantity }}</el-descriptions-item>
            <el-descriptions-item label="不良数量">{{ traceData.task.defectQuantity }}</el-descriptions-item>
            <el-descriptions-item label="完工数量">{{ traceData.task.finishQuantity }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ traceData.task.actualStartTime }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="工单信息" name="workOrder">
          <el-descriptions v-if="traceData.workOrder" :column="3" border>
            <el-descriptions-item label="工单号">{{ traceData.workOrder.workOrderNo }}</el-descriptions-item>
            <el-descriptions-item label="产品名称">{{ traceData.workOrder.productName }}</el-descriptions-item>
            <el-descriptions-item label="规格">{{ traceData.workOrder.spec }}</el-descriptions-item>
            <el-descriptions-item label="批次号">{{ traceData.workOrder.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="计划数量">{{ traceData.workOrder.planQuantity }}</el-descriptions-item>
            <el-descriptions-item label="投入数量">{{ traceData.workOrder.inputQuantity }}</el-descriptions-item>
            <el-descriptions-item label="完工数量">{{ traceData.workOrder.finishQuantity }}</el-descriptions-item>
            <el-descriptions-item label="不良数量">{{ traceData.workOrder.defectQuantity }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="条码信息" name="barcodes">
          <el-table :data="traceData.barcodes" border size="small">
            <el-table-column label="条码值" prop="barcodeValue" min-width="160" />
            <el-table-column label="条码模式" prop="barcodeMode" width="90" />
            <el-table-column label="批次号" prop="batchNo" width="130" />
            <el-table-column label="状态" prop="barcodeStatus" width="90" />
            <el-table-column label="创建时间" prop="createTime" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="工序履历" name="processHistories">
          <el-table :data="traceData.processHistories" border size="small">
            <el-table-column label="工序编码" prop="processCode" width="120" />
            <el-table-column label="工序名称" prop="processName" min-width="120" />
            <el-table-column label="动作类型" prop="actionType" width="90" />
            <el-table-column label="操作人" prop="operatorId" width="90" />
            <el-table-column label="操作原因" prop="actionReason" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作时间" prop="operateTime" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="报工记录" name="workReports">
          <el-table :data="traceData.workReports" border size="small">
            <el-table-column label="报工单号" prop="reportNo" width="150" />
            <el-table-column label="记录类型" prop="recordType" width="90" />
            <el-table-column label="净投入" prop="netInputQuantity" width="80" align="right" />
            <el-table-column label="净合格" prop="netGoodQuantity" width="80" align="right" />
            <el-table-column label="净不良" prop="netDefectQuantity" width="80" align="right" />
            <el-table-column label="冲销原因" prop="reverseReason" min-width="140" show-overflow-tooltip />
            <el-table-column label="报工时间" prop="reportTime" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="质量不良" name="qualityDefects">
          <el-table :data="traceData.qualityDefects" border size="small">
            <el-table-column label="来源类型" width="120">
              <template #default="{ row }">
                {{ TRACE_SOURCE_TYPE_TEXT[row.sourceType] ?? row.sourceType }}
              </template>
            </el-table-column>
            <el-table-column label="来源ID" prop="sourceId" width="120" />
            <el-table-column label="摘要" prop="summary" min-width="200" show-overflow-tooltip />
            <el-table-column label="事件时间" prop="eventTime" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="返修记录" name="repairRecords">
          <el-table :data="traceData.repairRecords" border size="small">
            <el-table-column label="来源类型" width="120">
              <template #default="{ row }">
                {{ TRACE_SOURCE_TYPE_TEXT[row.sourceType] ?? row.sourceType }}
              </template>
            </el-table-column>
            <el-table-column label="来源ID" prop="sourceId" width="120" />
            <el-table-column label="摘要" prop="summary" min-width="200" show-overflow-tooltip />
            <el-table-column label="事件时间" prop="eventTime" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="设备状态" name="equipmentStatuses">
          <el-table :data="traceData.equipmentStatuses" border size="small">
            <el-table-column label="来源类型" width="120">
              <template #default="{ row }">
                {{ TRACE_SOURCE_TYPE_TEXT[row.sourceType] ?? row.sourceType }}
              </template>
            </el-table-column>
            <el-table-column label="来源ID" prop="sourceId" width="120" />
            <el-table-column label="摘要" prop="summary" min-width="200" show-overflow-tooltip />
            <el-table-column label="事件时间" prop="eventTime" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="安灯异常" name="andonExceptions">
          <el-table :data="traceData.andonExceptions" border size="small">
            <el-table-column label="来源类型" width="120">
              <template #default="{ row }">
                {{ TRACE_SOURCE_TYPE_TEXT[row.sourceType] ?? row.sourceType }}
              </template>
            </el-table-column>
            <el-table-column label="来源ID" prop="sourceId" width="120" />
            <el-table-column label="摘要" prop="summary" min-width="200" show-overflow-tooltip />
            <el-table-column label="事件时间" prop="eventTime" width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-empty v-else-if="!loading" description="请输入业务键查询追溯信息" />
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

.warnings-alert {
  margin-top: 12px;
}

.trace-result {
  margin-top: 16px;
}

.completeness {
  margin-bottom: 12px;
}
</style>
