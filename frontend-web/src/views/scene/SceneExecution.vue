<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import {
  auditSceneCompletionOrder,
  createCompletionFromTask,
  submitSceneCompletionOrder,
  syncSceneCompletionOrder,
} from '@/api/scene/execution'
import { SCENE_COMPLETION_AUDIT_ROLES, SCENE_COMPLETION_ROLES } from '@/constants/scene'

defineOptions({ name: 'SceneExecution' })

interface OperationLog {
  time: string
  operation: string
  target: string
  result: string
}

const submitting = ref('')
const logs = ref<OperationLog[]>([])

const createForm = reactive({
  taskId: undefined as number | undefined,
  finishQuantity: undefined as number | undefined,
})

const auditForm = reactive({
  id: undefined as number | undefined,
  approved: true,
  remark: '',
})

const syncForm = reactive({ id: undefined as number | undefined })

function addLog(operation: string, target: string, result: string) {
  logs.value.unshift({
    time: new Date().toLocaleString('zh-CN', { hour12: false }),
    operation,
    target,
    result,
  })
}

async function createCompletion() {
  if (!createForm.taskId || !createForm.finishQuantity || createForm.finishQuantity <= 0) {
    ElMessage.warning('请填写生产任务 ID 和完工数量')
    return
  }
  submitting.value = 'create'
  try {
    const id = await createCompletionFromTask(createForm.taskId, createForm.finishQuantity)
    ElMessage.success(`完工单 #${id} 已创建`)
    addLog('创建完工单', `任务 #${createForm.taskId}`, `完工单 #${id}`)
    auditForm.id = id
  } finally {
    submitting.value = ''
  }
}

async function submitCompletion() {
  if (!auditForm.id) {
    ElMessage.warning('请填写完工单 ID')
    return
  }
  submitting.value = 'submit'
  try {
    await submitSceneCompletionOrder(auditForm.id)
    ElMessage.success('完工单已提交审核')
    addLog('提交完工单', `完工单 #${auditForm.id}`, '已提交')
  } finally {
    submitting.value = ''
  }
}

async function auditCompletion() {
  if (!auditForm.id) {
    ElMessage.warning('请填写完工单 ID')
    return
  }
  await ElMessageBox.confirm(
    `确认${auditForm.approved ? '审核通过' : '驳回'}完工单 #${auditForm.id}？`,
    '审核确认',
    { type: 'warning' },
  )
  submitting.value = 'audit'
  try {
    await auditSceneCompletionOrder(auditForm.id, auditForm.approved, auditForm.remark || undefined)
    ElMessage.success(auditForm.approved ? '完工单审核通过' : '完工单已驳回')
    addLog(
      '审核完工单',
      `完工单 #${auditForm.id}`,
      auditForm.approved ? '审核通过' : `驳回：${auditForm.remark || '无'}`,
    )
  } finally {
    submitting.value = ''
  }
}

async function syncCompletion() {
  if (!syncForm.id) {
    ElMessage.warning('请填写完工单 ID')
    return
  }
  await ElMessageBox.confirm(`确认同步完工单 #${syncForm.id} 至外部系统？`, '同步确认', {
    type: 'warning',
  })
  submitting.value = 'sync'
  try {
    await syncSceneCompletionOrder(syncForm.id)
    ElMessage.success('完工单同步请求已发送')
    addLog('同步完工单', `完工单 #${syncForm.id}`, '同步请求已发送')
  } finally {
    submitting.value = ''
  }
}
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="现场执行操作台"
      description="完工单从任务创建、提交审核、审核通过/驳回、人工同步外部系统的操作入口"
    />
    <el-row :gutter="16" class="scene-grid">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">1. 从任务创建完工单</span>
              <el-tag :type="SCENE_COMPLETION_ROLES.length > 2 ? 'warning' : 'info'" size="small">
                {{ SCENE_COMPLETION_ROLES.join(' / ') }}
              </el-tag>
            </div>
          </template>
          <el-form :model="createForm" label-width="100px">
            <el-form-item label="生产任务 ID" required>
              <el-input-number v-model="createForm.taskId" :min="1" />
            </el-form-item>
            <el-form-item label="完工数量" required>
              <el-input-number v-model="createForm.finishQuantity" :min="1" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting === 'create'" @click="createCompletion">
                创建完工单
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">2. 提交完工单审核</span>
              <el-tag size="small">{{ SCENE_COMPLETION_ROLES.join(' / ') }}</el-tag>
            </div>
          </template>
          <el-form :model="auditForm" label-width="100px">
            <el-form-item label="完工单 ID" required>
              <el-input-number v-model="auditForm.id" :min="1" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting === 'submit'" @click="submitCompletion">
                提交审核
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">3. 审核完工单</span>
              <el-tag type="warning" size="small">{{ SCENE_COMPLETION_AUDIT_ROLES.join(' / ') }}</el-tag>
            </div>
          </template>
          <el-form :model="auditForm" label-width="100px">
            <el-form-item label="完工单 ID" required>
              <el-input-number v-model="auditForm.id" :min="1" />
            </el-form-item>
            <el-form-item label="审核结果" required>
              <el-radio-group v-model="auditForm.approved">
                <el-radio :value="true">通过</el-radio>
                <el-radio :value="false">驳回</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="审核备注">
              <el-input v-model="auditForm.remark" type="textarea" maxlength="255" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button
                :type="auditForm.approved ? 'success' : 'warning'"
                :loading="submitting === 'audit'"
                @click="auditCompletion"
              >
                确认审核
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">4. 同步完工单至外部系统</span>
              <el-tag type="warning" size="small">{{ SCENE_COMPLETION_AUDIT_ROLES.join(' / ') }}</el-tag>
            </div>
          </template>
          <el-form :model="syncForm" label-width="100px">
            <el-form-item label="完工单 ID" required>
              <el-input-number v-model="syncForm.id" :min="1" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting === 'sync'" @click="syncCompletion">
                人工同步
              </el-button>
            </el-form-item>
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
.scene-grid {
  margin-top: 16px;
}
.scene-grid .el-col {
  margin-bottom: 16px;
}
.scene-grid .el-card {
  height: 100%;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-weight: 600;
}
</style>
