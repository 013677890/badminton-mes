<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import {
  SCENE_REPAIR_RECHECK_OPTIONS,
  SCENE_REPAIR_STATUS_MAP,
} from '@/constants/scene'
import {
  addRepairRecord,
  assignRepairWorkOrder,
  closeRepairWorkOrder,
  createRepairWorkOrder,
  getRepairWorkOrder,
  recheckRepairWorkOrder,
  startRepairWorkOrder,
} from '@/api/scene/management'
import type { SceneRepairWorkOrder } from '@/api/scene/management'

defineOptions({ name: 'SceneRepairList' })

// ---------- 创建返修工单 ----------

const createFormRef = ref<FormInstance>()
const createLoading = ref(false)
const createForm = reactive({
  sourceReportId: undefined as number | undefined,
  batchNo: '',
  defectQuantity: 0,
  repairQuantity: 0,
  reason: '',
})

const createRules: FormRules = {
  sourceReportId: [{ required: true, message: '请填写来源报工 ID', trigger: 'blur' }],
  batchNo: [{ required: true, message: '请填写批次号', trigger: 'blur' }],
  defectQuantity: [{ required: true, message: '请填写不良数量', trigger: 'blur' }],
  repairQuantity: [{ required: true, message: '请填写返修数量', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写返修原因', trigger: 'blur' }],
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    const id = await createRepairWorkOrder(createForm)
    ElMessage.success(`返修工单 #${id} 已创建`)
    createForm.sourceReportId = undefined
    createForm.batchNo = ''
    createForm.defectQuantity = 0
    createForm.repairQuantity = 0
    createForm.reason = ''
  } catch {
    // 失败提示由拦截器弹出
  } finally {
    createLoading.value = false
  }
}

// ---------- 返修工单操作台 ----------

const queryId = ref<number | undefined>(undefined)
const detail = ref<SceneRepairWorkOrder | null>(null)
const detailLoading = ref(false)

async function loadDetail() {
  if (!queryId.value) {
    ElMessage.warning('请输入返修工单 ID')
    return
  }
  detailLoading.value = true
  try {
    detail.value = await getRepairWorkOrder(queryId.value)
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

// 返修操作表单
const assigneeId = ref<number | undefined>(undefined)
const recordForm = reactive({ quantity: 0, description: '' })
const recheckForm = reactive({ result: 'RELEASED', quantity: 0 })

async function handleAssign() {
  if (!detail.value || !assigneeId.value) {
    ElMessage.warning('请填写分配人员 ID')
    return
  }
  try {
    await assignRepairWorkOrder(detail.value.id, assigneeId.value)
    ElMessage.success('已分配返修人员')
    await loadDetail()
  } catch {
    // 失败提示由拦截器弹出
  }
}

async function handleStart() {
  if (!detail.value) return
  try {
    await startRepairWorkOrder(detail.value.id)
    ElMessage.success('返修已开始')
    await loadDetail()
  } catch {
    // 失败提示由拦截器弹出
  }
}

async function handleAddRecord() {
  if (!detail.value) return
  if (!recordForm.quantity || !recordForm.description) {
    ElMessage.warning('请填写返修数量和描述')
    return
  }
  try {
    await addRepairRecord(detail.value.id, recordForm)
    ElMessage.success('返修记录已添加')
    recordForm.quantity = 0
    recordForm.description = ''
    await loadDetail()
  } catch {
    // 失败提示由拦截器弹出
  }
}

async function handleRecheck() {
  if (!detail.value) return
  try {
    await recheckRepairWorkOrder(detail.value.id, recheckForm)
    ElMessage.success('复检结果已提交')
    await loadDetail()
  } catch {
    // 失败提示由拦截器弹出
  }
}

async function handleClose() {
  if (!detail.value) return
  try {
    await closeRepairWorkOrder(detail.value.id)
    ElMessage.success('返修工单已关闭')
    await loadDetail()
  } catch {
    // 失败提示由拦截器弹出
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="返修工单"
      description="创建返修工单并执行分配、开始、记录、复检、关闭等操作（后端暂未提供分页查询）"
    />

    <el-row :gutter="16" class="console-grid">
      <!-- 创建返修工单 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header><span class="card-title">创建返修工单</span></template>
          <el-form
            ref="createFormRef"
            :model="createForm"
            :rules="createRules"
            label-width="100px"
          >
            <el-form-item label="来源报工 ID" prop="sourceReportId">
              <el-input-number v-model="createForm.sourceReportId" :min="1" />
            </el-form-item>
            <el-form-item label="批次号" prop="batchNo">
              <el-input v-model="createForm.batchNo" maxlength="64" />
            </el-form-item>
            <el-form-item label="不良数量" prop="defectQuantity">
              <el-input-number v-model="createForm.defectQuantity" :min="1" />
            </el-form-item>
            <el-form-item label="返修数量" prop="repairQuantity">
              <el-input-number v-model="createForm.repairQuantity" :min="1" />
            </el-form-item>
            <el-form-item label="返修原因" prop="reason">
              <el-input
                v-model="createForm.reason"
                type="textarea"
                maxlength="255"
                show-word-limit
                :rows="3"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="createLoading" @click="handleCreate">
                创建工单
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 返修工单操作台 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header><span class="card-title">返修工单操作台</span></template>

          <div class="query-bar">
            <el-input-number
              v-model="queryId"
              :min="1"
              placeholder="返修工单 ID"
              style="width: 200px"
            />
            <el-button type="primary" :loading="detailLoading" @click="loadDetail">查询</el-button>
          </div>

          <div v-loading="detailLoading">
            <template v-if="detail">
              <el-descriptions :column="2" border class="detail-desc">
                <el-descriptions-item label="工单号">{{ detail.repairNo }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <StatusTag :status="detail.status" :status-map="SCENE_REPAIR_STATUS_MAP" />
                </el-descriptions-item>
                <el-descriptions-item label="来源报工">{{ detail.sourceReportId }}</el-descriptions-item>
                <el-descriptions-item label="批次号">{{ detail.batchNo }}</el-descriptions-item>
                <el-descriptions-item label="不良数量">{{ detail.defectQuantity }}</el-descriptions-item>
                <el-descriptions-item label="返修数量">{{ detail.repairQuantity }}</el-descriptions-item>
                <el-descriptions-item label="分配人员">
                  {{ detail.assigneeId ?? '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="复检结果">
                  {{ detail.recheckResult ?? '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="返修原因" :span="2">
                  {{ detail.reason || '-' }}
                </el-descriptions-item>
              </el-descriptions>

              <!-- 按状态显示操作区 -->
              <el-divider content-position="left">操作</el-divider>

              <!-- 待分配：分配人员 -->
              <div v-if="detail.status === 'PENDING_ASSIGN'" class="action-row">
                <el-input-number
                  v-model="assigneeId"
                  :min="1"
                  placeholder="返修人员 ID"
                  style="width: 180px"
                />
                <el-button type="primary" @click="handleAssign">分配</el-button>
              </div>

              <!-- 待返修：开始 -->
              <div v-else-if="detail.status === 'PENDING_REPAIR'" class="action-row">
                <el-button type="success" @click="handleStart">开始返修</el-button>
              </div>

              <!-- 返修中：添加记录 -->
              <div v-else-if="detail.status === 'REPAIRING'" class="action-block">
                <div class="action-row">
                  <el-input-number
                    v-model="recordForm.quantity"
                    :min="1"
                    placeholder="返修数量"
                    style="width: 160px"
                  />
                  <el-input
                    v-model="recordForm.description"
                    maxlength="500"
                    placeholder="返修描述"
                    style="width: 300px"
                  />
                  <el-button type="primary" @click="handleAddRecord">添加记录</el-button>
                </div>
                <el-alert
                  title="添加完返修记录后可提交复检"
                  type="info"
                  :closable="false"
                  show-icon
                />
                <div class="action-row">
                  <span class="action-label">复检：</span>
                  <el-select v-model="recheckForm.result" style="width: 140px">
                    <el-option
                      v-for="opt in SCENE_REPAIR_RECHECK_OPTIONS"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                  <el-input-number
                    v-model="recheckForm.quantity"
                    :min="1"
                    placeholder="复检数量"
                    style="width: 160px"
                  />
                  <el-button type="warning" @click="handleRecheck">提交复检</el-button>
                </div>
              </div>

              <!-- 待复检：提交复检 -->
              <div v-else-if="detail.status === 'PENDING_RECHECK'" class="action-row">
                <span class="action-label">复检结果：</span>
                <el-select v-model="recheckForm.result" style="width: 140px">
                  <el-option
                    v-for="opt in SCENE_REPAIR_RECHECK_OPTIONS"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <el-input-number
                  v-model="recheckForm.quantity"
                  :min="1"
                  placeholder="复检数量"
                  style="width: 160px"
                />
                <el-button type="warning" @click="handleRecheck">提交复检</el-button>
              </div>

              <!-- 已放行/已报废：关闭 -->
              <div
                v-else-if="detail.status === 'RELEASED' || detail.status === 'SCRAPPED'"
                class="action-row"
              >
                <el-button type="danger" @click="handleClose">关闭工单</el-button>
              </div>

              <el-alert
                v-else-if="detail.status === 'CLOSED'"
                title="该返修工单已关闭"
                type="success"
                :closable="false"
                show-icon
              />
            </template>

            <el-empty v-else description="输入返修工单 ID 查询详情" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.console-grid .el-col {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
}

.query-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.detail-desc {
  margin-bottom: 16px;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.action-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-label {
  font-size: 14px;
  white-space: nowrap;
}
</style>
