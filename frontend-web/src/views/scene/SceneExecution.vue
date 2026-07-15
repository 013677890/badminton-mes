<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import {
  approveSceneCompletionOrder,
  approveSceneWorkReport,
  createSceneCompletionOrder,
  voidSceneCompletionOrder,
} from '@/api/scene/execution'

defineOptions({ name: 'SceneExecution' })

interface OperationLog {
  time: string
  operation: string
  target: string
  result: string
}

const submitting = ref('')
const logs = ref<OperationLog[]>([])
const reportForm = reactive({ id: undefined as number | undefined, employeeId: undefined as number | undefined })
const completionForm = reactive({
  productionTaskId: undefined as number | undefined,
  workOrderId: undefined as number | undefined,
  batchNo: '', completionQuantity: 0, goodQuantity: 0, defectQuantity: 0,
})
const auditForm = reactive({ id: undefined as number | undefined, remark: '' })
const voidForm = reactive({ id: undefined as number | undefined, remark: '' })

function addLog(operation: string, target: string, result: string) {
  logs.value.unshift({
    time: new Date().toLocaleString('zh-CN', { hour12: false }), operation, target, result,
  })
}

async function approveReport() {
  if (!reportForm.id || !reportForm.employeeId) {
    ElMessage.warning('请填写报工记录 ID 和员工 ID'); return
  }
  submitting.value = 'report'
  try {
    await approveSceneWorkReport(reportForm.id, reportForm.employeeId)
    ElMessage.success('报工审核通过，计件工资快照已同步')
    addLog('报工审核', `报工 #${reportForm.id}`, `员工 #${reportForm.employeeId}`)
  } finally { submitting.value = '' }
}

async function createCompletion() {
  if (!completionForm.productionTaskId || !completionForm.batchNo || completionForm.completionQuantity <= 0) {
    ElMessage.warning('请填写生产任务、产品批次和完工数量'); return
  }
  if (completionForm.goodQuantity + completionForm.defectQuantity !== completionForm.completionQuantity) {
    ElMessage.warning('良品数量与不良数量之和必须等于完工数量'); return
  }
  submitting.value = 'create'
  try {
    const id = await createSceneCompletionOrder({
      productionTaskId: completionForm.productionTaskId,
      workOrderId: completionForm.workOrderId,
      batchNo: completionForm.batchNo,
      completionQuantity: completionForm.completionQuantity,
      goodQuantity: completionForm.goodQuantity,
      defectQuantity: completionForm.defectQuantity,
    })
    ElMessage.success(`完工单 #${id} 已创建，等待审核`)
    addLog('创建完工单', `任务 #${completionForm.productionTaskId}`, `完工单 #${id}`)
    auditForm.id = id
  } finally { submitting.value = '' }
}

async function approveCompletion() {
  if (!auditForm.id) { ElMessage.warning('请填写完工单 ID'); return }
  await ElMessageBox.confirm(`确认审核通过完工单 #${auditForm.id}？`, '审核确认', { type: 'warning' })
  submitting.value = 'approve'
  try {
    await approveSceneCompletionOrder(auditForm.id, auditForm.remark || undefined)
    ElMessage.success('完工单审核通过')
    addLog('审核完工单', `完工单 #${auditForm.id}`, auditForm.remark || '审核通过')
  } finally { submitting.value = '' }
}

async function voidCompletion() {
  if (!voidForm.id) { ElMessage.warning('请填写完工单 ID'); return }
  await ElMessageBox.confirm(`确认作废待审核完工单 #${voidForm.id}？`, '作废确认', { type: 'warning' })
  submitting.value = 'void'
  try {
    await voidSceneCompletionOrder(voidForm.id, voidForm.remark || undefined)
    ElMessage.success('完工单已作废')
    addLog('作废完工单', `完工单 #${voidForm.id}`, voidForm.remark || '已作废')
  } finally { submitting.value = '' }
}
</script>

<template>
  <div class="page-container">
    <PageHeader title="现场执行" description="班组报工确认与生产完工单创建、审核、作废操作台" />
    <el-alert
      title="当前后端 scene 模块仅提供执行动作接口；生产任务、报工记录和完工单请从对应业务列表获取 ID 后在此操作。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-row :gutter="16" class="scene-grid">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header><span class="card-title">1. 待确认报工审核</span></template>
          <el-form :model="reportForm" label-width="110px">
            <el-form-item label="报工记录 ID" required><el-input-number v-model="reportForm.id" :min="1" /></el-form-item>
            <el-form-item label="计件员工 ID" required><el-input-number v-model="reportForm.employeeId" :min="1" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="submitting === 'report'" @click="approveReport">审核报工并同步计件</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header><span class="card-title">2. 创建生产完工单</span></template>
          <el-form :model="completionForm" label-width="110px">
            <el-row :gutter="12">
              <el-col :span="12"><el-form-item label="生产任务 ID" required><el-input-number v-model="completionForm.productionTaskId" :min="1" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="工单 ID"><el-input-number v-model="completionForm.workOrderId" :min="1" /><el-tooltip content="兼容字段，服务端以生产任务所属工单为准"><el-icon class="tip"><QuestionFilled /></el-icon></el-tooltip></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="产品批次" required><el-input v-model="completionForm.batchNo" maxlength="64" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="完工数量" required><el-input-number v-model="completionForm.completionQuantity" :min="1" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="良品数量" required><el-input-number v-model="completionForm.goodQuantity" :min="0" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="不良数量" required><el-input-number v-model="completionForm.defectQuantity" :min="0" /></el-form-item></el-col>
            </el-row>
            <el-form-item><el-button type="primary" :loading="submitting === 'create'" @click="createCompletion">创建待审核完工单</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header><span class="card-title">3. 审核生产完工单</span></template>
          <el-form :model="auditForm" label-width="100px">
            <el-form-item label="完工单 ID" required><el-input-number v-model="auditForm.id" :min="1" /></el-form-item>
            <el-form-item label="审核备注"><el-input v-model="auditForm.remark" type="textarea" maxlength="255" show-word-limit /></el-form-item>
            <el-form-item><el-button type="success" :loading="submitting === 'approve'" @click="approveCompletion">审核通过</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header><span class="card-title">4. 作废待审核完工单</span></template>
          <el-form :model="voidForm" label-width="100px">
            <el-form-item label="完工单 ID" required><el-input-number v-model="voidForm.id" :min="1" /></el-form-item>
            <el-form-item label="作废原因"><el-input v-model="voidForm.remark" type="textarea" maxlength="255" show-word-limit /></el-form-item>
            <el-form-item><el-button type="danger" :loading="submitting === 'void'" @click="voidCompletion">确认作废</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
    <el-card shadow="never">
      <template #header><span class="card-title">本次操作记录</span></template>
      <el-table :data="logs" empty-text="本次会话暂无操作" border>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="operation" label="操作" width="140" />
        <el-table-column prop="target" label="目标" width="180" />
        <el-table-column prop="result" label="结果" min-width="220" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.scene-grid { margin-top: 16px; }
.scene-grid .el-col { margin-bottom: 16px; }
.scene-grid .el-card { height: 100%; }
.card-title { font-weight: 600; }
.tip { margin-left: 6px; color: var(--el-text-color-secondary); }
</style>
