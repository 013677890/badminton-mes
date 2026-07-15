<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import PagePager from './PagePager.vue'
import { useTable } from '@/composables/useTable'
import type { OptionItem } from '@/types/components'
import { ENABLE_STATUS_OPTIONS } from '@/constants/production'
import {
  REPAIR_RECHECK_OPTIONS,
  SCENE_BATCH_STATUS_MAP,
  SCENE_DISPATCH_STATUS_MAP,
  SCENE_MANAGE_ROLES,
  SCENE_OPERATION_STATUS_MAP,
  SCENE_OPERATOR_ROLES,
  SCENE_PARAMETER_TYPE_OPTIONS,
  SCENE_TASK_EXEC_ROLES,
  SCENE_TASK_PLAN_ROLES,
  SCENE_TASK_STATUS_MAP,
  SCENE_TASK_STATUS_OPTIONS,
} from '@/constants/scene'
import { loadLineOptions, loadProductOptions, loadWorkshopOptions } from '@/api/production/options'
import {
  actionSceneDispatch,
  actionSceneOperation,
  actionSceneTask,
  addSceneRepairRecord,
  assignSceneRepair,
  auditSceneCompletion,
  changeSceneParameterStatus,
  closeSceneRepair,
  createSceneCompletion,
  createSceneRepair,
  generateSceneDispatch,
  getMySceneOperations,
  getSceneDispatchOperations,
  getSceneDispatchPage,
  getSceneOperationHistories,
  getSceneOperationPage,
  getSceneParameterLogs,
  getSceneParameterPage,
  getSceneProductStatusPage,
  getSceneRepair,
  getSceneStatusHistories,
  getSceneTaskPage,
  getSceneTaskProgress,
  pauseSceneOperation,
  reasonActionSceneTask,
  recheckSceneRepair,
  reverseSceneWorkReport,
  saveSceneParameter,
  saveSceneTask,
  scanSceneOperation,
  startSceneRepair,
  submitSceneCompletion,
  submitSceneWorkReport,
  syncSceneCompletion,
  updateSceneCompletion,
} from '@/api/scene/management'
import type { SceneDispatchOrder, SceneProductionParameter, SceneProductionTask, SceneRepair } from '@/api/scene/management'

defineOptions({ name: 'SceneExecution' })

const activeTab = ref('tasks')
const workshopOptions = ref<OptionItem[]>([]); const lineOptions = ref<OptionItem[]>([]); const productOptions = ref<OptionItem[]>([])
onMounted(async () => { [workshopOptions.value, lineOptions.value, productOptions.value] = await Promise.all([loadWorkshopOptions(), loadLineOptions(), loadProductOptions()]) })
function json(value: unknown) { return JSON.stringify(value, null, 2) }

// 生产任务
const taskFilters = reactive({ taskNo: '', workshopId: undefined as number | undefined, lineId: undefined as number | undefined, taskStatus: undefined as number | undefined, planDate: '' })
const taskTable = useTable({ fetcher: getSceneTaskPage })
const taskVisible = ref(false); const taskEditingId = ref<number>()
const taskForm = reactive({ workOrderId: undefined as number | undefined, lineId: undefined as number | undefined, shiftId: undefined as number | undefined, planDate: '', planQuantity: 1, planStartTime: '', planEndTime: '' })
function openTask(row?: SceneProductionTask | Record<string, any>) { taskEditingId.value = row?.id; Object.assign(taskForm, row ? { workOrderId: row.workOrderId, lineId: row.lineId, shiftId: row.shiftId || undefined, planDate: row.planDate, planQuantity: row.planQuantity, planStartTime: row.planStartTime, planEndTime: row.planEndTime } : { workOrderId: undefined, lineId: undefined, shiftId: undefined, planDate: '', planQuantity: 1, planStartTime: '', planEndTime: '' }); taskVisible.value = true }
async function submitTask() { if (!taskForm.workOrderId || !taskForm.lineId || !taskForm.planDate || !taskForm.planStartTime || !taskForm.planEndTime) { ElMessage.warning('请完整填写任务计划'); return } await saveSceneTask(taskEditingId.value, taskForm as any); ElMessage.success('生产任务已保存'); taskVisible.value = false; await taskTable.refresh() }
async function taskAction(row: SceneProductionTask | Record<string, any>, action: 'audit' | 'release' | 'start' | 'resume') { await actionSceneTask(row.id, action); ElMessage.success('任务状态已更新'); await taskTable.refresh() }
async function taskReasonAction(row: SceneProductionTask | Record<string, any>, action: 'pause' | 'close') { const { value } = await ElMessageBox.prompt(`请输入${action === 'pause' ? '暂停' : '关闭'}原因`, '操作确认', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' }); await reasonActionSceneTask(row.id, action, value); ElMessage.success('任务状态已更新'); await taskTable.refresh() }
const detailVisible = ref(false); const detailTitle = ref('详情'); const detailData = ref<unknown>()
async function showProgress(row: SceneProductionTask | Record<string, any>) { detailData.value = await getSceneTaskProgress(row.id); detailTitle.value = '任务进度'; detailVisible.value = true }

// 派工单
const dispatchFilters = reactive({ dispatchNo: '', taskId: undefined as number | undefined, dispatchStatus: undefined as number | undefined })
const dispatchTable = useTable({ fetcher: getSceneDispatchPage })
async function createDispatch() { const { value } = await ElMessageBox.prompt('请输入已下发生产任务 ID', '生成派工单', { inputPattern: /^[1-9]\d*$/, inputErrorMessage: '请输入正整数' }); const id = await generateSceneDispatch(Number(value)); ElMessage.success(`派工单 #${id} 已生成`); await dispatchTable.refresh() }
async function dispatchAction(row: SceneDispatchOrder | Record<string, any>, action: 'confirm' | 'cancel') { if (action === 'cancel') await ElMessageBox.confirm('确认取消该派工单？', '取消确认', { type: 'warning' }); await actionSceneDispatch(row.id, action); ElMessage.success('派工单状态已更新'); await dispatchTable.refresh() }
async function showOperations(row: SceneDispatchOrder | Record<string, any>) { detailData.value = await getSceneDispatchOperations(row.id); detailTitle.value = '派工工序明细'; detailVisible.value = true }

// 工位作业
const operationFilters = reactive({ taskId: undefined as number | undefined, userId: undefined as number | undefined, stationId: undefined as number | undefined, equipmentId: undefined as number | undefined, detailStatus: undefined as number | undefined })
const operationTable = useTable({ fetcher: getSceneOperationPage })
async function loadMyOperations() { const rows = await getMySceneOperations(); operationTable.data.value = rows; operationTable.pagination.value.total = rows.length }
async function scanOperation(row: Record<string, any>) { const { value } = await ElMessageBox.prompt('扫描或输入产品条码', '工位扫码', { inputPattern: /\S+/, inputErrorMessage: '条码不能为空' }); await scanSceneOperation(row.id, value, row.equipmentId); ElMessage.success('扫码成功') }
async function operationAction(row: Record<string, any>, action: 'start' | 'finish') { await actionSceneOperation(row.id, action); ElMessage.success('作业状态已更新'); await operationTable.refresh() }
async function pauseOperation(row: Record<string, any>) { const { value } = await ElMessageBox.prompt('请输入暂停原因', '暂停作业', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' }); await pauseSceneOperation(row.id, value); ElMessage.success('作业已暂停'); await operationTable.refresh() }

// 在制批次
const statusFilters = reactive({ batchNo: '', taskId: undefined as number | undefined, batchStatus: undefined as number | undefined, abnormal: undefined as boolean | undefined })
const statusTable = useTable({ fetcher: getSceneProductStatusPage })
async function showStatusHistory(row: Record<string, any>) { const [statuses, operations] = await Promise.all([getSceneStatusHistories(row.id), getSceneOperationHistories(row.id)]); detailData.value = { statusHistories: statuses, operationHistories: operations }; detailTitle.value = '批次流转履历'; detailVisible.value = true }

// 生产参数
const parameterFilters = reactive({ paramCode: '', workshopId: undefined as number | undefined, lineId: undefined as number | undefined, productId: undefined as number | undefined, status: undefined as number | undefined })
const parameterTable = useTable({ fetcher: getSceneParameterPage })
const parameterVisible = ref(false); const parameterEditingId = ref<number>()
const parameterForm = reactive({ paramCode: '', paramName: '', paramValue: '', valueType: 4, workshopId: undefined as number | undefined, lineId: undefined as number | undefined, productId: undefined as number | undefined, remark: '', changeReason: '' })
function openParameter(row?: SceneProductionParameter | Record<string, any>) { parameterEditingId.value = row?.id; Object.assign(parameterForm, row ? { paramCode: row.paramCode, paramName: row.paramName, paramValue: row.paramValue, valueType: row.valueType, workshopId: row.workshopId || undefined, lineId: row.lineId || undefined, productId: row.productId || undefined, remark: row.remark || '', changeReason: '' } : { paramCode: '', paramName: '', paramValue: '', valueType: 4, workshopId: undefined, lineId: undefined, productId: undefined, remark: '', changeReason: '' }); parameterVisible.value = true }
async function submitParameter() { if (!parameterForm.paramCode || !parameterForm.paramName || !parameterForm.paramValue || !parameterForm.changeReason) { ElMessage.warning('请填写参数信息和变更原因'); return } await saveSceneParameter(parameterEditingId.value, parameterForm); ElMessage.success('生产参数已保存'); parameterVisible.value = false; await parameterTable.refresh() }
async function toggleParameter(row: SceneProductionParameter | Record<string, any>) { const { value } = await ElMessageBox.prompt('请输入状态变更原因', row.status === 1 ? '停用参数' : '启用参数', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' }); await changeSceneParameterStatus(row.id, row.status !== 1, value); ElMessage.success('参数状态已更新'); await parameterTable.refresh() }
async function showParameterLogs(row: Record<string, any>) { detailData.value = await getSceneParameterLogs(row.id); detailTitle.value = '参数变更记录'; detailVisible.value = true }

// 执行单据（后端未提供完工单/返修单分页，按业务 ID 操作）
const reportForm = reactive({ requestNo: '', dispatchDetailId: undefined as number | undefined, inputQuantity: 0, goodQuantity: 0, defectQuantity: 0, reworkQuantity: 0, barcodeValue: '', reportTime: '', device: false })
const reverseForm = reactive({ id: undefined as number | undefined, requestNo: '', reason: '' })
async function submitReport() { if (!reportForm.requestNo || !reportForm.dispatchDetailId || !reportForm.reportTime) { ElMessage.warning('请完整填写报工信息'); return } const id = await submitSceneWorkReport({ requestNo: reportForm.requestNo, dispatchDetailId: reportForm.dispatchDetailId, inputQuantity: reportForm.inputQuantity, goodQuantity: reportForm.goodQuantity, defectQuantity: reportForm.defectQuantity, reworkQuantity: reportForm.reworkQuantity, barcodeValue: reportForm.barcodeValue || undefined, reportTime: reportForm.reportTime }, reportForm.device); ElMessage.success(`报工记录 #${id} 已创建`) }
async function reverseReport() { if (!reverseForm.id || !reverseForm.requestNo || !reverseForm.reason) { ElMessage.warning('请完整填写冲销信息'); return } const id = await reverseSceneWorkReport(reverseForm.id, reverseForm.requestNo, reverseForm.reason); ElMessage.success(`冲销记录 #${id} 已创建`) }

const completionForm = reactive({ taskId: undefined as number | undefined, id: undefined as number | undefined, finishQuantity: 1, approved: true, remark: '' })
async function createCompletion() { if (!completionForm.taskId) { ElMessage.warning('请输入任务 ID'); return } const id = await createSceneCompletion(completionForm.taskId, completionForm.finishQuantity); completionForm.id = id; ElMessage.success(`完工单 #${id} 已创建`) }
async function updateCompletion() { if (!completionForm.id) { ElMessage.warning('请输入完工单 ID'); return } await updateSceneCompletion(completionForm.id, completionForm.finishQuantity); ElMessage.success('完工数量已更新') }
async function completionAction(action: 'submit' | 'audit' | 'sync') { if (!completionForm.id) { ElMessage.warning('请输入完工单 ID'); return } if (action === 'submit') await submitSceneCompletion(completionForm.id); else if (action === 'audit') await auditSceneCompletion(completionForm.id, completionForm.approved, completionForm.remark || undefined); else await syncSceneCompletion(completionForm.id); ElMessage.success('完工单操作成功') }

const repairForm = reactive({ id: undefined as number | undefined, sourceReportId: undefined as number | undefined, batchNo: '', defectQuantity: 1, repairQuantity: 1, reason: '', assigneeId: undefined as number | undefined, recordQuantity: 1, description: '', recheckResult: 'RELEASED', recheckQuantity: 1 })
const repairData = ref<SceneRepair>()
async function createRepair() { if (!repairForm.sourceReportId || !repairForm.batchNo || !repairForm.reason) { ElMessage.warning('请完整填写返修来源、批次和原因'); return } const id = await createSceneRepair({ sourceReportId: repairForm.sourceReportId, batchNo: repairForm.batchNo, defectQuantity: repairForm.defectQuantity, repairQuantity: repairForm.repairQuantity, reason: repairForm.reason }); repairForm.id = id; ElMessage.success(`返修单 #${id} 已创建`); await loadRepair() }
async function loadRepair() { if (!repairForm.id) { ElMessage.warning('请输入返修单 ID'); return } repairData.value = await getSceneRepair(repairForm.id) }
async function repairAction(action: 'assign' | 'start' | 'record' | 'recheck' | 'close') { if (!repairForm.id) { ElMessage.warning('请输入返修单 ID'); return } if (action === 'assign') { if (!repairForm.assigneeId) return ElMessage.warning('请输入维修人 ID'); await assignSceneRepair(repairForm.id, repairForm.assigneeId) } else if (action === 'start') await startSceneRepair(repairForm.id); else if (action === 'record') await addSceneRepairRecord(repairForm.id, repairForm.recordQuantity, repairForm.description); else if (action === 'recheck') await recheckSceneRepair(repairForm.id, repairForm.recheckResult, repairForm.recheckQuantity); else await closeSceneRepair(repairForm.id); ElMessage.success('返修单操作成功'); await loadRepair() }
</script>

<template>
  <div class="page-container">
    <PageHeader title="现场执行" description="生产任务下发、派工执行、批次流转、参数控制与报工完工返修闭环" />
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="生产任务" name="tasks">
          <div class="toolbar"><el-form :inline="true" :model="taskFilters"><el-form-item label="任务号"><el-input v-model="taskFilters.taskNo" clearable /></el-form-item><el-form-item label="车间"><el-select v-model="taskFilters.workshopId" clearable><el-option v-for="item in workshopOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="状态"><el-select v-model="taskFilters.taskStatus" clearable><el-option v-for="item in SCENE_TASK_STATUS_OPTIONS" :key="item.value" v-bind="item" /></el-select></el-form-item><el-button type="primary" @click="taskTable.query(taskFilters)">查询</el-button></el-form><PermissionButton :roles="SCENE_TASK_PLAN_ROLES" type="primary" @click="openTask()">新建任务</PermissionButton></div>
          <el-table v-loading="taskTable.loading.value" :data="taskTable.data.value" border><el-table-column prop="taskNo" label="任务号" width="150" /><el-table-column prop="workOrderNo" label="工单号" width="150" /><el-table-column prop="productName" label="产品" min-width="140" /><el-table-column prop="batchNo" label="批次" width="120" /><el-table-column prop="lineName" label="产线" width="120" /><el-table-column prop="planQuantity" label="计划" /><el-table-column prop="finishQuantity" label="完工" /><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.taskStatus" :map="SCENE_TASK_STATUS_MAP" /></template></el-table-column><el-table-column label="操作" width="360" fixed="right"><template #default="{ row }"><el-button link @click="showProgress(row)">进度</el-button><PermissionButton v-if="row.taskStatus === 0" :roles="SCENE_TASK_PLAN_ROLES" link @click="openTask(row)">编辑</PermissionButton><PermissionButton v-if="row.taskStatus === 0" :roles="SCENE_TASK_PLAN_ROLES" link type="primary" @click="taskAction(row, 'audit')">审核</PermissionButton><PermissionButton v-if="row.taskStatus === 1" :roles="SCENE_TASK_PLAN_ROLES" link type="primary" @click="taskAction(row, 'release')">下发</PermissionButton><PermissionButton v-if="row.taskStatus === 2" :roles="SCENE_TASK_EXEC_ROLES" link type="success" @click="taskAction(row, 'start')">开工</PermissionButton><PermissionButton v-if="row.taskStatus === 3" :roles="SCENE_TASK_EXEC_ROLES" link type="warning" @click="taskReasonAction(row, 'pause')">暂停</PermissionButton><PermissionButton v-if="row.taskStatus === 4" :roles="SCENE_TASK_EXEC_ROLES" link type="success" @click="taskAction(row, 'resume')">恢复</PermissionButton><PermissionButton v-if="[3,4,5].includes(row.taskStatus)" :roles="SCENE_MANAGE_ROLES" link @click="taskReasonAction(row, 'close')">关闭</PermissionButton></template></el-table-column></el-table><PagePager :state="taskTable" />
        </el-tab-pane>

        <el-tab-pane label="派工单" name="dispatch">
          <div class="toolbar"><el-form :inline="true"><el-form-item label="派工号"><el-input v-model="dispatchFilters.dispatchNo" /></el-form-item><el-form-item label="任务 ID"><el-input-number v-model="dispatchFilters.taskId" :min="1" /></el-form-item><el-button type="primary" @click="dispatchTable.query(dispatchFilters)">查询</el-button></el-form><PermissionButton :roles="SCENE_TASK_EXEC_ROLES" type="primary" @click="createDispatch">生成派工单</PermissionButton></div>
          <el-table :data="dispatchTable.data.value" border><el-table-column prop="dispatchNo" label="派工号" min-width="170" /><el-table-column prop="taskId" label="任务 ID" /><el-table-column prop="routingCode" label="工艺路线" width="130" /><el-table-column prop="routingVersion" label="版本" /><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.dispatchStatus" :map="SCENE_DISPATCH_STATUS_MAP" /></template></el-table-column><el-table-column label="操作" width="240"><template #default="{ row }"><el-button link @click="showOperations(row)">工序明细</el-button><PermissionButton v-if="row.dispatchStatus === 0" :roles="SCENE_TASK_EXEC_ROLES" link type="primary" @click="dispatchAction(row, 'confirm')">确认</PermissionButton><PermissionButton v-if="[0,1].includes(row.dispatchStatus)" :roles="SCENE_TASK_EXEC_ROLES" link type="danger" @click="dispatchAction(row, 'cancel')">取消</PermissionButton></template></el-table-column></el-table><PagePager :state="dispatchTable" />
        </el-tab-pane>

        <el-tab-pane label="工位作业" name="operations">
          <div class="toolbar"><el-form :inline="true"><el-form-item label="任务 ID"><el-input-number v-model="operationFilters.taskId" :min="1" /></el-form-item><el-form-item label="人员 ID"><el-input-number v-model="operationFilters.userId" :min="1" /></el-form-item><el-form-item label="状态"><el-select v-model="operationFilters.detailStatus" clearable><el-option label="待作业" :value="0" /><el-option label="作业中" :value="1" /><el-option label="已完成" :value="2" /><el-option label="异常" :value="3" /></el-select></el-form-item><el-button type="primary" @click="operationTable.query(operationFilters)">查询</el-button></el-form><el-button @click="loadMyOperations">我的作业</el-button></div>
          <el-table :data="operationTable.data.value" border><el-table-column prop="id" label="作业 ID" /><el-table-column prop="processCode" label="工序编码" width="120" /><el-table-column prop="processName" label="工序" min-width="140" /><el-table-column prop="stationId" label="工位 ID" /><el-table-column prop="equipmentId" label="设备 ID" /><el-table-column prop="planQuantity" label="计划" /><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.detailStatus" :map="SCENE_OPERATION_STATUS_MAP" /></template></el-table-column><el-table-column label="操作" width="260"><template #default="{ row }"><PermissionButton :roles="SCENE_OPERATOR_ROLES" link @click="scanOperation(row)">扫码</PermissionButton><PermissionButton v-if="row.detailStatus === 0" :roles="SCENE_OPERATOR_ROLES" link type="success" @click="operationAction(row, 'start')">开始</PermissionButton><PermissionButton v-if="row.detailStatus === 1" :roles="SCENE_OPERATOR_ROLES" link type="warning" @click="pauseOperation(row)">暂停</PermissionButton><PermissionButton v-if="row.detailStatus === 1" :roles="SCENE_OPERATOR_ROLES" link type="primary" @click="operationAction(row, 'finish')">完工</PermissionButton></template></el-table-column></el-table><PagePager :state="operationTable" />
        </el-tab-pane>

        <el-tab-pane label="在制批次" name="statuses">
          <el-form :inline="true"><el-form-item label="批次"><el-input v-model="statusFilters.batchNo" /></el-form-item><el-form-item label="任务 ID"><el-input-number v-model="statusFilters.taskId" :min="1" /></el-form-item><el-form-item label="异常"><el-select v-model="statusFilters.abnormal" clearable><el-option label="是" :value="true" /><el-option label="否" :value="false" /></el-select></el-form-item><el-button type="primary" @click="statusTable.query(statusFilters)">查询</el-button></el-form>
          <el-table :data="statusTable.data.value" border><el-table-column prop="batchNo" label="批次" min-width="150" /><el-table-column prop="taskId" label="任务 ID" /><el-table-column prop="productId" label="产品 ID" /><el-table-column prop="currentProcessName" label="当前工序" min-width="130" /><el-table-column label="批次状态" width="100"><template #default="{ row }"><StatusTag :status="row.batchStatus" :map="SCENE_BATCH_STATUS_MAP" /></template></el-table-column><el-table-column label="异常" width="80"><template #default="{ row }"><el-tag :type="row.abnormal ? 'danger' : 'success'">{{ row.abnormal ? '是' : '否' }}</el-tag></template></el-table-column><el-table-column prop="updateTime" label="更新时间" width="170" /><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link @click="showStatusHistory(row)">流转履历</el-button></template></el-table-column></el-table><PagePager :state="statusTable" />
        </el-tab-pane>

        <el-tab-pane label="生产参数" name="parameters">
          <div class="toolbar"><el-form :inline="true"><el-form-item label="参数编码"><el-input v-model="parameterFilters.paramCode" /></el-form-item><el-form-item label="状态"><el-select v-model="parameterFilters.status" clearable><el-option v-for="item in ENABLE_STATUS_OPTIONS" :key="item.value" v-bind="item" /></el-select></el-form-item><el-button type="primary" @click="parameterTable.query(parameterFilters)">查询</el-button></el-form><PermissionButton :roles="SCENE_MANAGE_ROLES" type="primary" @click="openParameter()">新增参数</PermissionButton></div>
          <el-table :data="parameterTable.data.value" border><el-table-column prop="paramCode" label="编码" width="140" /><el-table-column prop="paramName" label="名称" min-width="140" /><el-table-column prop="paramValue" label="参数值" /><el-table-column prop="valueType" label="值类型" /><el-table-column prop="workshopId" label="车间 ID" /><el-table-column prop="lineId" label="产线 ID" /><el-table-column label="状态" width="80"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="220"><template #default="{ row }"><el-button link @click="showParameterLogs(row)">变更记录</el-button><PermissionButton :roles="SCENE_MANAGE_ROLES" link @click="openParameter(row)">编辑</PermissionButton><PermissionButton :roles="SCENE_MANAGE_ROLES" link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleParameter(row)">{{ row.status === 1 ? '停用' : '启用' }}</PermissionButton></template></el-table-column></el-table><PagePager :state="parameterTable" />
        </el-tab-pane>

        <el-tab-pane label="执行单据" name="documents">
          <el-alert title="完工单与返修单后端未提供分页接口，本页按生产任务或单据 ID 执行业务动作。" type="info" :closable="false" />
          <el-row :gutter="16" class="document-grid">
            <el-col :xs="24" :lg="12"><el-card shadow="never"><template #header>生产报工</template><el-form :model="reportForm" label-width="100px"><el-form-item label="请求流水" required><el-input v-model="reportForm.requestNo" /></el-form-item><el-form-item label="作业明细 ID" required><el-input-number v-model="reportForm.dispatchDetailId" :min="1" /></el-form-item><el-row><el-col :span="12"><el-form-item label="投入"><el-input-number v-model="reportForm.inputQuantity" :min="0" /></el-form-item></el-col><el-col :span="12"><el-form-item label="良品"><el-input-number v-model="reportForm.goodQuantity" :min="0" /></el-form-item></el-col><el-col :span="12"><el-form-item label="不良"><el-input-number v-model="reportForm.defectQuantity" :min="0" /></el-form-item></el-col><el-col :span="12"><el-form-item label="返工"><el-input-number v-model="reportForm.reworkQuantity" :min="0" /></el-form-item></el-col></el-row><el-form-item label="报工时间"><el-date-picker v-model="reportForm.reportTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item><el-form-item label="设备计数"><el-switch v-model="reportForm.device" /></el-form-item><PermissionButton :roles="reportForm.device ? SCENE_MANAGE_ROLES : SCENE_OPERATOR_ROLES" type="primary" @click="submitReport">提交报工</PermissionButton></el-form><el-divider>冲销报工</el-divider><el-form :model="reverseForm" label-width="100px"><el-form-item label="原报工 ID"><el-input-number v-model="reverseForm.id" :min="1" /></el-form-item><el-form-item label="请求流水"><el-input v-model="reverseForm.requestNo" /></el-form-item><el-form-item label="冲销原因"><el-input v-model="reverseForm.reason" /></el-form-item><PermissionButton :roles="SCENE_MANAGE_ROLES" type="warning" @click="reverseReport">冲销</PermissionButton></el-form></el-card></el-col>
            <el-col :xs="24" :lg="12"><el-card shadow="never"><template #header>生产完工单</template><el-form :model="completionForm" label-width="100px"><el-form-item label="任务 ID"><el-input-number v-model="completionForm.taskId" :min="1" /><PermissionButton :roles="SCENE_TASK_EXEC_ROLES" type="primary" @click="createCompletion">从任务创建</PermissionButton></el-form-item><el-form-item label="完工单 ID"><el-input-number v-model="completionForm.id" :min="1" /></el-form-item><el-form-item label="完工数量"><el-input-number v-model="completionForm.finishQuantity" :min="1" /><PermissionButton :roles="SCENE_TASK_EXEC_ROLES" @click="updateCompletion">更新数量</PermissionButton></el-form-item><el-form-item><PermissionButton :roles="SCENE_TASK_EXEC_ROLES" type="primary" @click="completionAction('submit')">提交审核</PermissionButton></el-form-item><el-divider>主管审核 / ERP 同步</el-divider><el-form-item label="审核结论"><el-switch v-model="completionForm.approved" active-text="通过" inactive-text="驳回" /></el-form-item><el-form-item label="审核备注"><el-input v-model="completionForm.remark" /></el-form-item><el-form-item><PermissionButton :roles="SCENE_MANAGE_ROLES" type="success" @click="completionAction('audit')">审核</PermissionButton><PermissionButton :roles="SCENE_MANAGE_ROLES" @click="completionAction('sync')">同步 ERP</PermissionButton></el-form-item></el-form></el-card></el-col>
            <el-col :span="24"><el-card shadow="never"><template #header>返修工单</template><el-form :inline="true" :model="repairForm"><el-form-item label="返修单 ID"><el-input-number v-model="repairForm.id" :min="1" /></el-form-item><el-button @click="loadRepair">查询</el-button><el-form-item label="来源报工 ID"><el-input-number v-model="repairForm.sourceReportId" :min="1" /></el-form-item><el-form-item label="批次"><el-input v-model="repairForm.batchNo" /></el-form-item><el-form-item label="不良数"><el-input-number v-model="repairForm.defectQuantity" :min="1" /></el-form-item><el-form-item label="返修数"><el-input-number v-model="repairForm.repairQuantity" :min="1" /></el-form-item><el-form-item label="原因"><el-input v-model="repairForm.reason" /></el-form-item><el-button type="primary" @click="createRepair">创建返修单</el-button></el-form><el-descriptions v-if="repairData" :column="4" border><el-descriptions-item label="单号">{{ repairData.repairNo }}</el-descriptions-item><el-descriptions-item label="批次">{{ repairData.batchNo }}</el-descriptions-item><el-descriptions-item label="状态">{{ repairData.status }}</el-descriptions-item><el-descriptions-item label="返修数量">{{ repairData.repairQuantity }}</el-descriptions-item></el-descriptions><el-divider>生命周期操作</el-divider><el-form :inline="true"><el-form-item label="维修人 ID"><el-input-number v-model="repairForm.assigneeId" :min="1" /></el-form-item><el-button @click="repairAction('assign')">指派</el-button><el-button @click="repairAction('start')">开始返修</el-button><el-form-item label="记录数量"><el-input-number v-model="repairForm.recordQuantity" :min="1" /></el-form-item><el-form-item label="记录说明"><el-input v-model="repairForm.description" /></el-form-item><el-button @click="repairAction('record')">添加记录</el-button><el-form-item label="复检结论"><el-select v-model="repairForm.recheckResult"><el-option v-for="item in REPAIR_RECHECK_OPTIONS" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="复检数量"><el-input-number v-model="repairForm.recheckQuantity" :min="0" /></el-form-item><el-button type="primary" @click="repairAction('recheck')">复检</el-button><el-button type="danger" @click="repairAction('close')">关闭</el-button></el-form></el-card></el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="taskVisible" :title="taskEditingId ? '编辑生产任务' : '新建生产任务'" width="660px"><el-form :model="taskForm" label-width="100px"><el-form-item label="工单 ID" required><el-input-number v-model="taskForm.workOrderId" :min="1" /></el-form-item><el-form-item label="产线" required><el-select v-model="taskForm.lineId" filterable><el-option v-for="item in lineOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="班次 ID"><el-input-number v-model="taskForm.shiftId" :min="1" /></el-form-item><el-form-item label="计划日期" required><el-date-picker v-model="taskForm.planDate" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="计划数量" required><el-input-number v-model="taskForm.planQuantity" :min="1" /></el-form-item><el-form-item label="开始时间" required><el-date-picker v-model="taskForm.planStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item><el-form-item label="结束时间" required><el-date-picker v-model="taskForm.planEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item></el-form><template #footer><el-button @click="taskVisible = false">取消</el-button><el-button type="primary" @click="submitTask">保存</el-button></template></el-dialog>
    <el-dialog v-model="parameterVisible" :title="parameterEditingId ? '编辑生产参数' : '新增生产参数'" width="660px"><el-form :model="parameterForm" label-width="100px"><el-form-item label="参数编码" required><el-input v-model="parameterForm.paramCode" /></el-form-item><el-form-item label="参数名称" required><el-input v-model="parameterForm.paramName" /></el-form-item><el-form-item label="参数值" required><el-input v-model="parameterForm.paramValue" /></el-form-item><el-form-item label="值类型"><el-select v-model="parameterForm.valueType"><el-option v-for="item in SCENE_PARAMETER_TYPE_OPTIONS" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="车间"><el-select v-model="parameterForm.workshopId" clearable><el-option v-for="item in workshopOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="产线"><el-select v-model="parameterForm.lineId" clearable><el-option v-for="item in lineOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="产品"><el-select v-model="parameterForm.productId" clearable><el-option v-for="item in productOptions" :key="item.value" v-bind="item" /></el-select></el-form-item><el-form-item label="备注"><el-input v-model="parameterForm.remark" /></el-form-item><el-form-item label="变更原因" required><el-input v-model="parameterForm.changeReason" /></el-form-item></el-form><template #footer><el-button @click="parameterVisible = false">取消</el-button><el-button type="primary" @click="submitParameter">保存</el-button></template></el-dialog>
    <el-drawer v-model="detailVisible" :title="detailTitle" size="760px"><pre class="json-result">{{ json(detailData) }}</pre></el-drawer>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }.pagination { margin-top: 16px; justify-content: flex-end; }.document-grid { margin-top: 16px; }.document-grid .el-col { margin-bottom: 16px; }.json-result { max-height: calc(100vh - 120px); overflow: auto; padding: 12px; background: var(--el-fill-color-light); white-space: pre-wrap; }
</style>
