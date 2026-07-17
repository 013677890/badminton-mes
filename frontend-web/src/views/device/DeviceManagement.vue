<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import type { OptionItem } from '@/types/components'
import { ENABLE_STATUS_MAP, ENABLE_STATUS_OPTIONS } from '@/constants/production'
import {
  CHECK_RESULT_OPTIONS,
  COMMISSIONING_RESULT_OPTIONS,
  COUNT_EXCEPTION_STATUS_MAP,
  COUNT_EXCEPTION_STATUS_OPTIONS,
  COUNT_EXCEPTION_TYPE_TEXT,
  COUNT_MODE_OPTIONS,
  DEVICE_COMMISSIONING_STATUS_MAP,
  DEVICE_COMMISSIONING_STATUS_OPTIONS,
  DEVICE_CONFIG_ROLES,
  DEVICE_EXCEPTION_ROLES,
  DEVICE_REPORT_ROLES,
  MATCH_STATUS_MAP,
  MATCH_STATUS_OPTIONS,
  REPORT_MODE_OPTIONS,
  REPORT_STATUS_MAP,
  RUNTIME_STATUS_OPTIONS,
} from '@/constants/device'
import {
  createDeviceAccessConfig,
  deleteDeviceAccessConfig,
  getDeviceAccessConfigPage,
  loadDeviceAccessConfigOptions,
  updateDeviceAccessConfig,
} from '@/api/device/accessConfig'
import type { DeviceAccessConfig, DeviceAccessConfigSaveReq } from '@/api/device/accessConfig'
import {
  createDeviceCommissioningRecord,
  getDeviceCommissioningPage,
} from '@/api/device/commissioning'
import type { DeviceCommissioningSaveReq } from '@/api/device/commissioning'
import {
  getDeviceCountExceptionPage,
  getDeviceCountRecordPage,
  processDeviceCountException,
  reportDeviceCount,
} from '@/api/device/count'
import type { DeviceCountReportReq, DeviceCountReportResult } from '@/api/device/count'
import { loadEquipmentLedgerOptions } from '@/api/equipment/options'
import { loadLineOptions, loadProcessOptions } from '@/api/production/options'

defineOptions({ name: 'DeviceManagement' })

const activeTab = ref('access')
const equipmentOptions = ref<OptionItem[]>([])
const accessConfigOptions = ref<OptionItem[]>([])
const processOptions = ref<OptionItem[]>([])
const lineOptions = ref<OptionItem[]>([])

function optionLabel(options: OptionItem[], value: number | null | undefined, fallback = '-') {
  return options.find((item) => item.value === value)?.label ?? (value ? `#${value}` : fallback)
}

async function reloadAccessOptions() {
  accessConfigOptions.value = await loadDeviceAccessConfigOptions()
}

onMounted(async () => {
  const results = await Promise.allSettled([
    loadEquipmentLedgerOptions(),
    loadProcessOptions(),
    loadLineOptions(),
    loadDeviceAccessConfigOptions(),
  ])
  if (results[0].status === 'fulfilled') equipmentOptions.value = results[0].value
  if (results[1].status === 'fulfilled') processOptions.value = results[1].value
  if (results[2].status === 'fulfilled') lineOptions.value = results[2].value
  if (results[3].status === 'fulfilled') accessConfigOptions.value = results[3].value
})

// ---------- 接入配置 ----------

const accessFilters = reactive({
  keyword: '', equipmentId: undefined as number | undefined,
  commissioningStatus: '', enabledStatus: undefined as number | undefined,
})
const accessTable = useTable({ fetcher: getDeviceAccessConfigPage })
const accessDialogVisible = ref(false)
const accessEditingId = ref<number>()
const accessForm = reactive({
  configCode: '', configName: '', equipmentId: undefined as number | undefined,
  collectionPointCode: '', processId: undefined as number | undefined,
  productionLineId: undefined as number | undefined, countMode: 'CUMULATIVE',
  spikeThreshold: undefined as number | undefined, reportMode: 'PENDING_CONFIRMATION',
  enabledStatus: 0, remark: '',
})

function resetAccessForm() {
  Object.assign(accessForm, {
    configCode: '', configName: '', equipmentId: undefined, collectionPointCode: '',
    processId: undefined, productionLineId: undefined, countMode: 'CUMULATIVE',
    spikeThreshold: undefined, reportMode: 'PENDING_CONFIRMATION', enabledStatus: 0, remark: '',
  })
}

function openAccessDialog(row?: DeviceAccessConfig | Record<string, any>) {
  resetAccessForm()
  accessEditingId.value = row?.id
  if (row) {
    Object.assign(accessForm, {
      ...row,
      processId: row.processId ?? undefined,
      productionLineId: row.productionLineId ?? undefined,
      spikeThreshold: row.spikeThreshold ?? undefined,
      remark: row.remark ?? '',
    })
  }
  accessDialogVisible.value = true
}

async function submitAccess() {
  if (!accessForm.configCode || !accessForm.configName || !accessForm.equipmentId || !accessForm.collectionPointCode) {
    ElMessage.warning('请完整填写配置编码、名称、设备与采集点')
    return
  }
  const payload = { ...accessForm } as DeviceAccessConfigSaveReq
  if (payload.enabledStatus === 1 && accessEditingId.value) {
    const current = accessTable.data.value.find((item) => item.id === accessEditingId.value)
    if (current?.commissioningStatus !== 'PASSED') {
      ElMessage.warning('联调通过后才能启用正式采集')
      return
    }
  }
  if (accessEditingId.value) await updateDeviceAccessConfig(accessEditingId.value, payload)
  else await createDeviceAccessConfig(payload)
  ElMessage.success(accessEditingId.value ? '接入配置已更新' : '接入配置已创建')
  accessDialogVisible.value = false
  await Promise.all([accessTable.refresh(), reloadAccessOptions()])
}

async function removeAccess(row: DeviceAccessConfig | Record<string, any>) {
  await ElMessageBox.confirm(`确认删除接入配置“${row.configName}”？`, '删除确认', { type: 'warning' })
  await deleteDeviceAccessConfig(row.id)
  ElMessage.success('接入配置已删除')
  await Promise.all([accessTable.refresh(), reloadAccessOptions()])
}

// ---------- 联调记录 ----------

const commissioningFilters = reactive({
  accessConfigId: undefined as number | undefined, testResult: '',
})
const commissioningTable = useTable({ fetcher: getDeviceCommissioningPage })
const commissioningDialogVisible = ref(false)
const commissioningForm = reactive<DeviceCommissioningSaveReq>({
  accessConfigId: 0, testTime: '', communicationResult: 'SUCCESS',
  dataFormatResult: 'SUCCESS', testResult: 'PASSED', issueDescription: '', samplePayload: '',
})
const commissioningFailed = computed(() =>
  commissioningForm.communicationResult === 'FAILED'
  || commissioningForm.dataFormatResult === 'FAILED'
  || commissioningForm.testResult === 'FAILED',
)

function openCommissioningDialog(configId?: number) {
  Object.assign(commissioningForm, {
    accessConfigId: configId ?? 0,
    testTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
    communicationResult: 'SUCCESS', dataFormatResult: 'SUCCESS', testResult: 'PASSED',
    issueDescription: '', samplePayload: '',
  })
  commissioningDialogVisible.value = true
}

async function submitCommissioning() {
  if (!commissioningForm.accessConfigId || !commissioningForm.testTime) {
    ElMessage.warning('请选择接入配置并填写联调时间')
    return
  }
  if (commissioningFailed.value && !commissioningForm.issueDescription?.trim()) {
    ElMessage.warning('联调失败时必须填写问题说明')
    return
  }
  if (commissioningForm.testResult === 'PASSED'
    && (commissioningForm.communicationResult !== 'SUCCESS' || commissioningForm.dataFormatResult !== 'SUCCESS')) {
    ElMessage.warning('通信和数据格式检查成功后才能通过联调')
    return
  }
  await createDeviceCommissioningRecord(commissioningForm)
  ElMessage.success('联调记录已保存')
  commissioningDialogVisible.value = false
  await Promise.all([commissioningTable.refresh(), accessTable.refresh(), reloadAccessOptions()])
}

// ---------- 计数记录 ----------

const countFilters = reactive({
  accessConfigId: undefined as number | undefined,
  equipmentId: undefined as number | undefined,
  matchStatus: '',
})
const countTable = useTable({ fetcher: getDeviceCountRecordPage })
const reportDialogVisible = ref(false)
const reportResultVisible = ref(false)
const latestReportResult = ref<DeviceCountReportResult>()
const reportForm = reactive<DeviceCountReportReq>({
  configCode: '', equipmentCode: '', collectedAt: '', serialNumber: '', countValue: 0,
  runtimeStatus: 'RUNNING', faultStatus: '', rawPayload: '',
})

function openReportDialog() {
  Object.assign(reportForm, {
    configCode: '', equipmentCode: '',
    collectedAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
    serialNumber: `MES-${Date.now()}`, countValue: 0, runtimeStatus: 'RUNNING',
    faultStatus: '', rawPayload: '',
  })
  reportDialogVisible.value = true
}

async function submitReport() {
  if (!reportForm.configCode || !reportForm.equipmentCode || !reportForm.collectedAt || !reportForm.serialNumber) {
    ElMessage.warning('请完整填写配置编码、设备编码、采集时间和流水号')
    return
  }
  latestReportResult.value = await reportDeviceCount(reportForm)
  reportDialogVisible.value = false
  reportResultVisible.value = true
  await Promise.all([countTable.refresh(), exceptionTable.refresh(), accessTable.refresh()])
}

// ---------- 计数异常 ----------

const exceptionFilters = reactive({
  accessConfigId: undefined as number | undefined,
  equipmentId: undefined as number | undefined,
  processingStatus: '',
})
const exceptionTable = useTable({ fetcher: getDeviceCountExceptionPage })
const exceptionDialogVisible = ref(false)
const exceptionForm = reactive({ id: 0, processingStatus: 'RESOLVED' as 'RESOLVED' | 'IGNORED', processingResult: '' })

function openExceptionDialog(id: number) {
  Object.assign(exceptionForm, { id, processingStatus: 'RESOLVED', processingResult: '' })
  exceptionDialogVisible.value = true
}

async function submitException() {
  if (!exceptionForm.processingResult.trim()) {
    ElMessage.warning('请填写处理结果')
    return
  }
  await processDeviceCountException(exceptionForm.id, exceptionForm)
  ElMessage.success('异常已处理')
  exceptionDialogVisible.value = false
  await exceptionTable.refresh()
}
</script>

<template>
  <div class="page-container">
    <PageHeader title="设备数据接入" description="接入配置、设备联调、计数采集与异常处理闭环" />
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="接入配置" name="access">
          <el-form inline :model="accessFilters" class="filters">
            <el-form-item label="关键字"><el-input v-model="accessFilters.keyword" clearable /></el-form-item>
            <el-form-item label="设备"><el-select v-model="accessFilters.equipmentId" clearable filterable><el-option v-for="o in equipmentOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
            <el-form-item label="联调状态"><el-select v-model="accessFilters.commissioningStatus" clearable><el-option v-for="o in DEVICE_COMMISSIONING_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
            <el-form-item label="采集状态"><el-select v-model="accessFilters.enabledStatus" clearable><el-option v-for="o in ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
            <el-form-item><el-button type="primary" @click="accessTable.query(accessFilters)">查询</el-button><el-button @click="accessTable.reset()">重置</el-button></el-form-item>
          </el-form>
          <div class="toolbar"><PermissionButton :roles="DEVICE_CONFIG_ROLES" type="primary" @click="openAccessDialog()">新增接入配置</PermissionButton></div>
          <el-table v-loading="accessTable.loading.value" :data="accessTable.data.value" border>
            <el-table-column prop="configCode" label="配置编码" width="140" /><el-table-column prop="configName" label="配置名称" min-width="150" />
            <el-table-column label="设备" min-width="180"><template #default="{ row }">{{ optionLabel(equipmentOptions, row.equipmentId) }}</template></el-table-column>
            <el-table-column prop="collectionPointCode" label="采集点" width="140" /><el-table-column prop="countMode" label="计数模式" width="110" />
            <el-table-column label="联调" width="90"><template #default="{ row }"><StatusTag :status="row.commissioningStatus" :status-map="DEVICE_COMMISSIONING_STATUS_MAP" /></template></el-table-column>
            <el-table-column label="正式采集" width="90"><template #default="{ row }"><StatusTag :status="row.enabledStatus" :status-map="ENABLE_STATUS_MAP" /></template></el-table-column>
            <el-table-column prop="lastCommunicationTime" label="最后通信" width="170" />
            <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><PermissionButton link type="primary" :roles="DEVICE_CONFIG_ROLES" @click="openAccessDialog(row)">编辑</PermissionButton><PermissionButton link type="success" :roles="DEVICE_CONFIG_ROLES" @click="openCommissioningDialog(row.id)">联调</PermissionButton><PermissionButton link type="danger" :roles="DEVICE_CONFIG_ROLES" @click="removeAccess(row)">删除</PermissionButton></template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="accessTable.pagination.value.pageNo" v-model:page-size="accessTable.pagination.value.pageSize" :total="accessTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="accessTable.refresh" />
        </el-tab-pane>

        <el-tab-pane label="联调记录" name="commissioning">
          <el-form inline :model="commissioningFilters" class="filters"><el-form-item label="接入配置"><el-select v-model="commissioningFilters.accessConfigId" clearable filterable><el-option v-for="o in accessConfigOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="联调结果"><el-select v-model="commissioningFilters.testResult" clearable><el-option v-for="o in COMMISSIONING_RESULT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="commissioningTable.query(commissioningFilters)">查询</el-button><el-button @click="commissioningTable.reset()">重置</el-button></el-form-item></el-form>
          <div class="toolbar"><PermissionButton :roles="DEVICE_CONFIG_ROLES" type="primary" @click="openCommissioningDialog()">新增联调记录</PermissionButton></div>
          <el-table v-loading="commissioningTable.loading.value" :data="commissioningTable.data.value" border><el-table-column label="接入配置" min-width="200"><template #default="{ row }">{{ optionLabel(accessConfigOptions, row.accessConfigId) }}</template></el-table-column><el-table-column prop="testTime" label="联调时间" width="170" /><el-table-column prop="testerUserId" label="测试人" width="90" /><el-table-column prop="communicationResult" label="通信" width="90" /><el-table-column prop="dataFormatResult" label="格式" width="90" /><el-table-column label="结果" width="90"><template #default="{ row }"><StatusTag :status="row.testResult" :status-map="DEVICE_COMMISSIONING_STATUS_MAP" /></template></el-table-column><el-table-column prop="issueDescription" label="问题说明" min-width="220" show-overflow-tooltip /></el-table>
          <el-pagination v-model:current-page="commissioningTable.pagination.value.pageNo" v-model:page-size="commissioningTable.pagination.value.pageSize" :total="commissioningTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="commissioningTable.refresh" />
        </el-tab-pane>

        <el-tab-pane label="计数记录" name="counts">
          <el-form inline :model="countFilters" class="filters"><el-form-item label="接入配置"><el-select v-model="countFilters.accessConfigId" clearable filterable><el-option v-for="o in accessConfigOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="设备"><el-select v-model="countFilters.equipmentId" clearable filterable><el-option v-for="o in equipmentOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="匹配状态"><el-select v-model="countFilters.matchStatus" clearable><el-option v-for="o in MATCH_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="countTable.query(countFilters)">查询</el-button><el-button @click="countTable.reset()">重置</el-button></el-form-item></el-form>
          <div class="toolbar"><PermissionButton :roles="DEVICE_REPORT_ROLES" type="primary" @click="openReportDialog">模拟计数上报</PermissionButton></div>
          <el-table v-loading="countTable.loading.value" :data="countTable.data.value" border><el-table-column prop="equipmentCode" label="设备编码" width="130" /><el-table-column prop="collectionPointCode" label="采集点" width="130" /><el-table-column prop="collectedAt" label="采集时间" width="170" /><el-table-column prop="serialNumber" label="流水号" min-width="150" /><el-table-column prop="rawCount" label="原始计数" width="100" /><el-table-column prop="incrementCount" label="有效增量" width="100" /><el-table-column label="匹配状态" width="100"><template #default="{ row }"><StatusTag :status="row.matchStatus" :status-map="MATCH_STATUS_MAP" /></template></el-table-column><el-table-column label="报工状态" width="110"><template #default="{ row }"><StatusTag :status="row.reportStatus" :status-map="REPORT_STATUS_MAP" /></template></el-table-column></el-table>
          <el-pagination v-model:current-page="countTable.pagination.value.pageNo" v-model:page-size="countTable.pagination.value.pageSize" :total="countTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="countTable.refresh" />
        </el-tab-pane>

        <el-tab-pane label="计数异常" name="exceptions">
          <el-form inline :model="exceptionFilters" class="filters"><el-form-item label="接入配置"><el-select v-model="exceptionFilters.accessConfigId" clearable filterable><el-option v-for="o in accessConfigOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="设备"><el-select v-model="exceptionFilters.equipmentId" clearable filterable><el-option v-for="o in equipmentOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="处理状态"><el-select v-model="exceptionFilters.processingStatus" clearable><el-option v-for="o in COUNT_EXCEPTION_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="exceptionTable.query(exceptionFilters)">查询</el-button><el-button @click="exceptionTable.reset()">重置</el-button></el-form-item></el-form>
          <el-table v-loading="exceptionTable.loading.value" :data="exceptionTable.data.value" border><el-table-column prop="countRecordId" label="计数记录" width="100" /><el-table-column label="异常类型" width="150"><template #default="{ row }">{{ COUNT_EXCEPTION_TYPE_TEXT[row.exceptionType] ?? row.exceptionType }}</template></el-table-column><el-table-column prop="exceptionReason" label="异常原因" min-width="260" show-overflow-tooltip /><el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :status="row.processingStatus" :status-map="COUNT_EXCEPTION_STATUS_MAP" /></template></el-table-column><el-table-column prop="processingResult" label="处理结果" min-width="200" show-overflow-tooltip /><el-table-column prop="createTime" label="创建时间" width="170" /><el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><PermissionButton v-if="row.processingStatus === 'PENDING'" link type="primary" :roles="DEVICE_EXCEPTION_ROLES" @click="openExceptionDialog(row.id)">处理</PermissionButton></template></el-table-column></el-table>
          <el-pagination v-model:current-page="exceptionTable.pagination.value.pageNo" v-model:page-size="exceptionTable.pagination.value.pageSize" :total="exceptionTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="exceptionTable.refresh" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="accessDialogVisible" :title="accessEditingId ? '编辑接入配置' : '新增接入配置'" width="720px"><el-form :model="accessForm" label-width="110px"><el-row :gutter="16"><el-col :span="12"><el-form-item label="配置编码" required><el-input v-model="accessForm.configCode" :disabled="!!accessEditingId" maxlength="32" /></el-form-item></el-col><el-col :span="12"><el-form-item label="配置名称" required><el-input v-model="accessForm.configName" maxlength="128" /></el-form-item></el-col><el-col :span="12"><el-form-item label="设备台账" required><el-select v-model="accessForm.equipmentId" filterable><el-option v-for="o in equipmentOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="采集点编码" required><el-input v-model="accessForm.collectionPointCode" maxlength="64" /></el-form-item></el-col><el-col :span="12"><el-form-item label="关联工序"><el-select v-model="accessForm.processId" clearable filterable><el-option v-for="o in processOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="关联产线"><el-select v-model="accessForm.productionLineId" clearable filterable><el-option v-for="o in lineOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="计数模式"><el-select v-model="accessForm.countMode"><el-option v-for="o in COUNT_MODE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="跳变阈值"><el-input-number v-model="accessForm.spikeThreshold" :min="1" /></el-form-item></el-col><el-col :span="12"><el-form-item label="报工模式"><el-select v-model="accessForm.reportMode"><el-option v-for="o in REPORT_MODE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="正式采集"><el-switch v-model="accessForm.enabledStatus" :active-value="1" :inactive-value="0" /></el-form-item></el-col></el-row><el-form-item label="备注"><el-input v-model="accessForm.remark" type="textarea" maxlength="255" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="accessDialogVisible = false">取消</el-button><el-button type="primary" @click="submitAccess">保存</el-button></template></el-dialog>

    <el-dialog v-model="commissioningDialogVisible" title="新增联调记录" width="680px"><el-form :model="commissioningForm" label-width="110px"><el-form-item label="接入配置" required><el-select v-model="commissioningForm.accessConfigId" filterable><el-option v-for="o in accessConfigOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="联调时间" required><el-date-picker v-model="commissioningForm.testTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item><el-row :gutter="16"><el-col :span="8"><el-form-item label="通信结果" required><el-select v-model="commissioningForm.communicationResult"><el-option v-for="o in CHECK_RESULT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="格式结果" required><el-select v-model="commissioningForm.dataFormatResult"><el-option v-for="o in CHECK_RESULT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="联调结论" required><el-select v-model="commissioningForm.testResult"><el-option v-for="o in COMMISSIONING_RESULT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col></el-row><el-form-item label="问题说明" :required="commissioningFailed"><el-input v-model="commissioningForm.issueDescription" type="textarea" maxlength="500" show-word-limit /></el-form-item><el-form-item label="样例报文"><el-input v-model="commissioningForm.samplePayload" type="textarea" :rows="5" maxlength="5000" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="commissioningDialogVisible = false">取消</el-button><el-button type="primary" @click="submitCommissioning">保存</el-button></template></el-dialog>

    <el-dialog v-model="reportDialogVisible" title="模拟设备计数上报" width="680px"><el-alert title="此操作调用真实设备计数接入接口，可用于联调与幂等验证。" type="warning" :closable="false" /><el-form :model="reportForm" label-width="110px" class="dialog-form"><el-row :gutter="16"><el-col :span="12"><el-form-item label="配置编码" required><el-input v-model="reportForm.configCode" /></el-form-item></el-col><el-col :span="12"><el-form-item label="设备编码" required><el-input v-model="reportForm.equipmentCode" /></el-form-item></el-col><el-col :span="12"><el-form-item label="采集时间" required><el-date-picker v-model="reportForm.collectedAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item></el-col><el-col :span="12"><el-form-item label="设备流水号" required><el-input v-model="reportForm.serialNumber" /></el-form-item></el-col><el-col :span="12"><el-form-item label="计数值" required><el-input-number v-model="reportForm.countValue" :min="0" /></el-form-item></el-col><el-col :span="12"><el-form-item label="运行状态"><el-select v-model="reportForm.runtimeStatus"><el-option v-for="o in RUNTIME_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col></el-row><el-form-item label="故障状态"><el-input v-model="reportForm.faultStatus" maxlength="64" /></el-form-item><el-form-item label="原始报文"><el-input v-model="reportForm.rawPayload" type="textarea" :rows="4" maxlength="5000" /></el-form-item></el-form><template #footer><el-button @click="reportDialogVisible = false">取消</el-button><el-button type="primary" @click="submitReport">上报</el-button></template></el-dialog>

    <el-dialog v-model="reportResultVisible" title="计数处理结果" width="520px"><el-descriptions v-if="latestReportResult" :column="1" border><el-descriptions-item label="计数记录 ID">{{ latestReportResult.countRecordId }}</el-descriptions-item><el-descriptions-item label="有效增量">{{ latestReportResult.incrementCount }}</el-descriptions-item><el-descriptions-item label="匹配状态"><StatusTag :status="latestReportResult.matchStatus" :status-map="MATCH_STATUS_MAP" /></el-descriptions-item><el-descriptions-item label="报工状态"><StatusTag :status="latestReportResult.reportStatus" :status-map="REPORT_STATUS_MAP" /></el-descriptions-item><el-descriptions-item label="异常类型">{{ latestReportResult.exceptionType ? (COUNT_EXCEPTION_TYPE_TEXT[latestReportResult.exceptionType] ?? latestReportResult.exceptionType) : '-' }}</el-descriptions-item><el-descriptions-item label="处理说明">{{ latestReportResult.processingMessage }}</el-descriptions-item></el-descriptions></el-dialog>

    <el-dialog v-model="exceptionDialogVisible" title="处理计数异常" width="560px"><el-form :model="exceptionForm" label-width="100px"><el-form-item label="处理结论" required><el-radio-group v-model="exceptionForm.processingStatus"><el-radio value="RESOLVED">已解决</el-radio><el-radio value="IGNORED">忽略</el-radio></el-radio-group></el-form-item><el-form-item label="处理结果" required><el-input v-model="exceptionForm.processingResult" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="exceptionDialogVisible = false">取消</el-button><el-button type="primary" @click="submitException">提交</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.filters { margin-bottom: 4px; }
.filters :deep(.el-select), .filters :deep(.el-input) { width: 190px; }
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.el-pagination { justify-content: flex-end; margin-top: 16px; }
.dialog-form { margin-top: 16px; }
</style>
