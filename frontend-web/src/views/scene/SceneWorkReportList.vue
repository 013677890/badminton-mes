<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import {
  SCENE_REPORT_AUDIT_ROLES,
  SCENE_REPORT_ROLES,
} from '@/constants/scene'
import {
  reverseSceneWorkReport,
  submitDeviceCountReport,
  submitSceneWorkReport,
} from '@/api/scene/execution'
import type { SceneWorkReportSubmitReq } from '@/api/scene/execution'

defineOptions({ name: 'SceneWorkReportList' })

/** 生成客户端幂等请求号（UUID v4 片段） */
function genRequestNo(): string {
  return `RPT-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

// ---------- 人工报工提交 ----------

const submitFormRef = ref<FormInstance>()
const submitLoading = ref(false)
const submitForm = reactive<SceneWorkReportSubmitReq>({
  requestNo: genRequestNo(),
  dispatchDetailId: undefined as unknown as number,
  inputQuantity: 0,
  goodQuantity: 0,
  defectQuantity: 0,
  reworkQuantity: 0,
  barcodeValue: '',
  reportTime: '',
})

const submitRules: FormRules = {
  requestNo: [{ required: true, message: '请填写请求号', trigger: 'blur' }],
  dispatchDetailId: [{ required: true, message: '请填写派工明细 ID', trigger: 'blur' }],
  reportTime: [{ required: true, message: '请选择报工时间', trigger: 'change' }],
}

async function handleSubmitReport() {
  const valid = await submitFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const id = await submitSceneWorkReport(submitForm)
    ElMessage.success(`人工报工 #${id} 已提交`)
    submitForm.requestNo = genRequestNo()
  } catch {
    // 失败提示由拦截器弹出
  } finally {
    submitLoading.value = false
  }
}

// ---------- 设备计数报工 ----------

const deviceFormRef = ref<FormInstance>()
const deviceLoading = ref(false)
const deviceForm = reactive<SceneWorkReportSubmitReq>({
  requestNo: genRequestNo(),
  dispatchDetailId: undefined as unknown as number,
  inputQuantity: 0,
  goodQuantity: 0,
  defectQuantity: 0,
  reworkQuantity: 0,
  barcodeValue: '',
  reportTime: '',
})

async function handleSubmitDeviceReport() {
  const valid = await deviceFormRef.value?.validate().catch(() => false)
  if (!valid) return
  deviceLoading.value = true
  try {
    const id = await submitDeviceCountReport(deviceForm)
    ElMessage.success(`设备计数报工 #${id} 已提交`)
    deviceForm.requestNo = genRequestNo()
  } catch {
    // 失败提示由拦截器弹出
  } finally {
    deviceLoading.value = false
  }
}

// ---------- 报工冲销 ----------

const reverseFormRef = ref<FormInstance>()
const reverseLoading = ref(false)
const reverseForm = reactive({
  id: undefined as number | undefined,
  requestNo: genRequestNo(),
  reason: '',
})

const reverseRules: FormRules = {
  id: [{ required: true, message: '请填写报工记录 ID', trigger: 'blur' }],
  requestNo: [{ required: true, message: '请填写请求号', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写冲销原因', trigger: 'blur' }],
}

async function handleReverse() {
  const valid = await reverseFormRef.value?.validate().catch(() => false)
  if (!valid) return
  reverseLoading.value = true
  try {
    const id = await reverseSceneWorkReport(reverseForm.id!, {
      requestNo: reverseForm.requestNo,
      reason: reverseForm.reason,
    })
    ElMessage.success(`报工 #${reverseForm.id} 已冲销，冲销单 #${id}`)
    reverseForm.requestNo = genRequestNo()
    reverseForm.reason = ''
  } catch {
    // 失败提示由拦截器弹出
  } finally {
    reverseLoading.value = false
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="报工操作台"
      description="人工报工与设备计数报工提交、报工全额冲销（后端暂未提供报工分页查询）"
    />

    <el-row :gutter="16" class="console-grid">
      <!-- 人工报工提交 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">人工报工提交</span>
              <PermissionButton
                :roles="SCENE_REPORT_ROLES"
                type="primary"
                size="small"
                :loading="submitLoading"
                @click="handleSubmitReport"
              >
                提交报工
              </PermissionButton>
            </div>
          </template>
          <el-form
            ref="submitFormRef"
            :model="submitForm"
            :rules="submitRules"
            label-width="100px"
            :disabled="false"
          >
            <el-form-item label="请求号" prop="requestNo">
              <el-input v-model="submitForm.requestNo" maxlength="64">
                <template #append>
                  <el-button @click="submitForm.requestNo = genRequestNo()">刷新</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="派工明细 ID" prop="dispatchDetailId">
              <el-input-number v-model="submitForm.dispatchDetailId" :min="1" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="投入数" prop="inputQuantity">
                  <el-input-number v-model="submitForm.inputQuantity" :min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="良品数" prop="goodQuantity">
                  <el-input-number v-model="submitForm.goodQuantity" :min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="不良数" prop="defectQuantity">
                  <el-input-number v-model="submitForm.defectQuantity" :min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="返修数" prop="reworkQuantity">
                  <el-input-number v-model="submitForm.reworkQuantity" :min="0" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="扫码值" prop="barcodeValue">
              <el-input v-model="submitForm.barcodeValue" maxlength="64" placeholder="选填" />
            </el-form-item>
            <el-form-item label="报工时间" prop="reportTime">
              <el-date-picker
                v-model="submitForm.reportTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择报工时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 设备计数报工 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">设备计数报工</span>
              <PermissionButton
                :roles="SCENE_REPORT_AUDIT_ROLES"
                type="primary"
                size="small"
                :loading="deviceLoading"
                @click="handleSubmitDeviceReport"
              >
                提交报工
              </PermissionButton>
            </div>
          </template>
          <el-form
            ref="deviceFormRef"
            :model="deviceForm"
            :rules="submitRules"
            label-width="100px"
          >
            <el-form-item label="请求号" prop="requestNo">
              <el-input v-model="deviceForm.requestNo" maxlength="64">
                <template #append>
                  <el-button @click="deviceForm.requestNo = genRequestNo()">刷新</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="派工明细 ID" prop="dispatchDetailId">
              <el-input-number v-model="deviceForm.dispatchDetailId" :min="1" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="投入数" prop="inputQuantity">
                  <el-input-number v-model="deviceForm.inputQuantity" :min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="良品数" prop="goodQuantity">
                  <el-input-number v-model="deviceForm.goodQuantity" :min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="不良数" prop="defectQuantity">
                  <el-input-number v-model="deviceForm.defectQuantity" :min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="返修数" prop="reworkQuantity">
                  <el-input-number v-model="deviceForm.reworkQuantity" :min="0" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="扫码值" prop="barcodeValue">
              <el-input v-model="deviceForm.barcodeValue" maxlength="64" placeholder="选填" />
            </el-form-item>
            <el-form-item label="报工时间" prop="reportTime">
              <el-date-picker
                v-model="deviceForm.reportTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择报工时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 报工冲销 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">报工全额冲销</span>
              <PermissionButton
                :roles="SCENE_REPORT_AUDIT_ROLES"
                type="danger"
                size="small"
                :loading="reverseLoading"
                @click="handleReverse"
              >
                确认冲销
              </PermissionButton>
            </div>
          </template>
          <el-form
            ref="reverseFormRef"
            :model="reverseForm"
            :rules="reverseRules"
            label-width="100px"
          >
            <el-form-item label="报工记录 ID" prop="id">
              <el-input-number v-model="reverseForm.id" :min="1" />
            </el-form-item>
            <el-form-item label="请求号" prop="requestNo">
              <el-input v-model="reverseForm.requestNo" maxlength="64">
                <template #append>
                  <el-button @click="reverseForm.requestNo = genRequestNo()">刷新</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="冲销原因" prop="reason">
              <el-input
                v-model="reverseForm.reason"
                type="textarea"
                maxlength="255"
                show-word-limit
                :rows="3"
              />
            </el-form-item>
          </el-form>
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

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-weight: 600;
}
</style>
