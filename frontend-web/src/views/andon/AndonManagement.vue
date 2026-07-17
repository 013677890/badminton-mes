<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import type { OptionItem } from '@/types/components'
import { ENABLE_STATUS_MAP, ENABLE_STATUS_OPTIONS } from '@/constants/production'
import {
  ACTION_TYPE_TEXT,
  ANDON_CLOSE_ROLES,
  ANDON_MANAGE_ROLES,
  EVENT_STATUS_MAP,
  EVENT_STATUS_OPTIONS,
  EXCEPTION_CATEGORY_OPTIONS,
  EXCEPTION_CATEGORY_TEXT,
  HANDLING_MODE_OPTIONS,
  HANDLING_MODE_TEXT,
  LIGHT_STATUS_MAP,
  NOTIFICATION_CHANNEL_OPTIONS,
  ROLE_OPTIONS,
  SEVERITY_MAP,
  SEVERITY_OPTIONS,
  SOURCE_CHANNEL_OPTIONS,
  SOURCE_CHANNEL_TEXT,
  TIMEOUT_STATUS_MAP,
} from '@/constants/andon'
import {
  createAndonType,
  deleteAndonType,
  getAndonTypePage,
  loadAndonTypeOptions,
  updateAndonType,
} from '@/api/andon/type'
import type { AndonType, AndonTypeSaveReq } from '@/api/andon/type'
import {
  createAndonReason,
  deleteAndonReason,
  getAndonReasonPage,
  loadAndonReasonOptions,
  updateAndonReason,
} from '@/api/andon/reason'
import type { AndonReason, AndonReasonSaveReq } from '@/api/andon/reason'
import {
  createAndonConfiguration,
  deleteAndonConfiguration,
  getAndonConfigurationPage,
  updateAndonConfiguration,
} from '@/api/andon/configuration'
import type { AndonConfiguration, AndonConfigurationSaveReq } from '@/api/andon/configuration'
import {
  actionAndonEvent,
  createAndonEvent,
  getAndonEvent,
  getAndonEventPage,
} from '@/api/andon/event'
import type { AndonEvent, AndonEventActionReq, AndonEventCreateReq } from '@/api/andon/event'
import { loadLineOptions, loadWorkshopOptions } from '@/api/production/options'
import { loadEquipmentLedgerOptions } from '@/api/equipment/options'

defineOptions({ name: 'AndonManagement' })

const activeTab = ref('events')
const typeOptions = ref<OptionItem[]>([])
const reasonOptions = ref<OptionItem[]>([])
const lineOptions = ref<OptionItem[]>([])
const workshopOptions = ref<OptionItem[]>([])
const equipmentOptions = ref<OptionItem[]>([])

function optionLabel(options: OptionItem[], value: number | null | undefined) {
  return options.find((item) => item.value === value)?.label ?? (value ? `#${value}` : '全局')
}
async function reloadTypes() { typeOptions.value = await loadAndonTypeOptions() }
async function reloadReasons(typeId?: number) { reasonOptions.value = await loadAndonReasonOptions(typeId) }

onMounted(async () => {
  const results = await Promise.allSettled([
    loadAndonTypeOptions(), loadAndonReasonOptions(), loadLineOptions(),
    loadWorkshopOptions(), loadEquipmentLedgerOptions(),
  ])
  if (results[0].status === 'fulfilled') typeOptions.value = results[0].value
  if (results[1].status === 'fulfilled') reasonOptions.value = results[1].value
  if (results[2].status === 'fulfilled') lineOptions.value = results[2].value
  if (results[3].status === 'fulfilled') workshopOptions.value = results[3].value
  if (results[4].status === 'fulfilled') equipmentOptions.value = results[4].value
})

// ---------- 安灯类型 ----------
const typeFilters = reactive({ keyword: '', exceptionCategory: '', handlingMode: '', enabledStatus: undefined as number | undefined })
const typeTable = useTable({ fetcher: getAndonTypePage })
const typeDialogVisible = ref(false)
const typeEditingId = ref<number>()
const typeForm = reactive({
  typeCode: '', typeName: '', exceptionCategory: 'PRODUCTION', handlingMode: 'ASSISTANCE',
  responseMinutes: 30 as number | null, responsibleRoleCode: '', notificationChannels: ['IN_APP'] as string[],
  lightControlEnabled: false, enabledStatus: 1, remark: '',
})
function openTypeDialog(row?: AndonType | Record<string, any>) {
  typeEditingId.value = row?.id
  Object.assign(typeForm, row ? {
    ...row, responseMinutes: row.responseMinutes, responsibleRoleCode: row.responsibleRoleCode ?? '',
    notificationChannels: row.notificationChannels?.split(',').filter(Boolean) ?? [], remark: row.remark ?? '',
  } : {
    typeCode: '', typeName: '', exceptionCategory: 'PRODUCTION', handlingMode: 'ASSISTANCE',
    responseMinutes: 30, responsibleRoleCode: '', notificationChannels: ['IN_APP'],
    lightControlEnabled: false, enabledStatus: 1, remark: '',
  })
  typeDialogVisible.value = true
}
async function submitType() {
  if (!typeForm.typeCode || !typeForm.typeName || !typeForm.exceptionCategory || !typeForm.handlingMode) {
    ElMessage.warning('请完整填写类型编码、名称、异常类别和处理方式'); return
  }
  if (typeForm.handlingMode === 'ASSISTANCE'
    && (!typeForm.responseMinutes || (!typeForm.responsibleRoleCode && !typeForm.notificationChannels.length))) {
    ElMessage.warning('协同处理必须配置响应时限、责任角色和通知渠道'); return
  }
  const payload = {
    ...typeForm,
    responseMinutes: typeForm.responseMinutes ?? null,
    notificationChannels: typeForm.notificationChannels.join(',') || null,
    responsibleRoleCode: typeForm.responsibleRoleCode || null,
    remark: typeForm.remark || null,
  } as AndonTypeSaveReq
  if (typeEditingId.value) await updateAndonType(typeEditingId.value, payload)
  else await createAndonType(payload)
  ElMessage.success(typeEditingId.value ? '安灯类型已更新' : '安灯类型已创建')
  typeDialogVisible.value = false
  await Promise.all([typeTable.refresh(), reloadTypes()])
}
async function removeType(row: AndonType | Record<string, any>) {
  await ElMessageBox.confirm(`确认删除安灯类型“${row.typeName}”？`, '删除确认', { type: 'warning' })
  await deleteAndonType(row.id); ElMessage.success('安灯类型已删除')
  await Promise.all([typeTable.refresh(), reloadTypes()])
}

// ---------- 异常原因 ----------
const reasonFilters = reactive({ keyword: '', andonTypeId: undefined as number | undefined, enabledStatus: undefined as number | undefined })
const reasonTable = useTable({ fetcher: getAndonReasonPage })
const reasonDialogVisible = ref(false)
const reasonEditingId = ref<number>()
const reasonForm = reactive({ reasonCode: '', reasonName: '', andonTypeId: 0, reasonDescription: '', enabledStatus: 1 })
function openReasonDialog(row?: AndonReason | Record<string, any>) {
  reasonEditingId.value = row?.id
  Object.assign(reasonForm, row ? { ...row, reasonDescription: row.reasonDescription ?? '' }
    : { reasonCode: '', reasonName: '', andonTypeId: 0, reasonDescription: '', enabledStatus: 1 })
  reasonDialogVisible.value = true
}
async function submitReason() {
  if (!reasonForm.reasonCode || !reasonForm.reasonName || !reasonForm.andonTypeId) {
    ElMessage.warning('请完整填写原因编码、名称和安灯类型'); return
  }
  const payload = { ...reasonForm } as AndonReasonSaveReq
  if (reasonEditingId.value) await updateAndonReason(reasonEditingId.value, payload)
  else await createAndonReason(payload)
  ElMessage.success(reasonEditingId.value ? '异常原因已更新' : '异常原因已创建')
  reasonDialogVisible.value = false
  await Promise.all([reasonTable.refresh(), reloadReasons()])
}
async function removeReason(row: AndonReason | Record<string, any>) {
  await ElMessageBox.confirm(`确认删除异常原因“${row.reasonName}”？`, '删除确认', { type: 'warning' })
  await deleteAndonReason(row.id); ElMessage.success('异常原因已删除')
  await Promise.all([reasonTable.refresh(), reloadReasons()])
}

// ---------- 处理配置 ----------
const configFilters = reactive({ andonTypeId: undefined as number | undefined, productionLineId: undefined as number | undefined, enabledStatus: undefined as number | undefined })
const configTable = useTable({ fetcher: getAndonConfigurationPage })
const configDialogVisible = ref(false)
const configEditingId = ref<number>()
const configForm = reactive({
  andonTypeId: 0, productionLineId: undefined as number | undefined,
  handlerUserId: undefined as number | undefined, handlerRoleCode: '',
  escalationUserId: undefined as number | undefined, escalationRoleCode: '',
  responseMinutes: 30, escalationMinutes: undefined as number | undefined,
  notificationChannels: ['IN_APP'] as string[], enabledStatus: 1, remark: '',
})
function openConfigDialog(row?: AndonConfiguration | Record<string, any>) {
  configEditingId.value = row?.id
  Object.assign(configForm, row ? {
    ...row, productionLineId: row.productionLineId ?? undefined,
    handlerUserId: row.handlerUserId ?? undefined, handlerRoleCode: row.handlerRoleCode ?? '',
    escalationUserId: row.escalationUserId ?? undefined, escalationRoleCode: row.escalationRoleCode ?? '',
    escalationMinutes: row.escalationMinutes ?? undefined,
    notificationChannels: row.notificationChannels.split(',').filter(Boolean), remark: row.remark ?? '',
  } : {
    andonTypeId: 0, productionLineId: undefined, handlerUserId: undefined, handlerRoleCode: '',
    escalationUserId: undefined, escalationRoleCode: '', responseMinutes: 30,
    escalationMinutes: undefined, notificationChannels: ['IN_APP'], enabledStatus: 1, remark: '',
  })
  configDialogVisible.value = true
}
async function submitConfig() {
  if (!configForm.andonTypeId || !configForm.responseMinutes || !configForm.notificationChannels.length) {
    ElMessage.warning('请选择安灯类型，并填写响应时限和通知渠道'); return
  }
  if (!configForm.handlerUserId && !configForm.handlerRoleCode) {
    ElMessage.warning('处理人和处理角色至少填写一个'); return
  }
  const payload = {
    ...configForm,
    handlerRoleCode: configForm.handlerRoleCode || undefined,
    escalationRoleCode: configForm.escalationRoleCode || undefined,
    notificationChannels: configForm.notificationChannels.join(','),
  } as AndonConfigurationSaveReq
  if (configEditingId.value) await updateAndonConfiguration(configEditingId.value, payload)
  else await createAndonConfiguration(payload)
  ElMessage.success(configEditingId.value ? '处理配置已更新' : '处理配置已创建')
  configDialogVisible.value = false; await configTable.refresh()
}
async function removeConfig(row: AndonConfiguration | Record<string, any>) {
  await ElMessageBox.confirm('确认删除这条安灯处理配置？', '删除确认', { type: 'warning' })
  await deleteAndonConfiguration(row.id); ElMessage.success('处理配置已删除'); await configTable.refresh()
}

// ---------- 安灯事件 ----------
const eventFilters = reactive({ keyword: '', andonTypeId: undefined as number | undefined, eventStatus: '', severity: '' })
const eventTable = useTable({ fetcher: getAndonEventPage })
const eventDialogVisible = ref(false)
const eventForm = reactive<AndonEventCreateReq>({ andonTypeId: 0, sourceChannel: 'WEB', severity: 'NORMAL', description: '' })
async function openEventDialog() {
  Object.keys(eventForm).forEach((key) => delete (eventForm as Record<string, unknown>)[key])
  Object.assign(eventForm, { andonTypeId: 0, sourceChannel: 'WEB', severity: 'NORMAL', description: '' })
  reasonOptions.value = []
  eventDialogVisible.value = true
}
watch(() => eventForm.andonTypeId, async (id) => {
  if (eventDialogVisible.value && id) {
    eventForm.reasonId = undefined
    await reloadReasons(id)
  }
})
async function submitEvent() {
  if (!eventForm.andonTypeId || !eventForm.description.trim()) {
    ElMessage.warning('请选择安灯类型并填写异常描述'); return
  }
  const id = await createAndonEvent(eventForm)
  ElMessage.success(`安灯异常已发起：#${id}`)
  eventDialogVisible.value = false; await eventTable.refresh()
}

type EventAction = 'confirm' | 'start-processing' | 'transfer' | 'complete' | 'close' | 'escalate'
const actionDialogVisible = ref(false)
const actionRow = ref<AndonEvent>()
const actionType = ref<EventAction>('confirm')
const actionForm = reactive<AndonEventActionReq>({})
const actionTitles: Record<EventAction, string> = {
  confirm: '确认异常', 'start-processing': '开始处理', transfer: '转派异常',
  complete: '完成处理', close: '关闭异常', escalate: '升级异常',
}
async function openAction(row: AndonEvent | Record<string, any>, action: EventAction) {
  actionRow.value = row as AndonEvent; actionType.value = action
  Object.keys(actionForm).forEach((key) => delete (actionForm as Record<string, unknown>)[key])
  if (action === 'confirm' || action === 'complete') {
    await reloadReasons(row.andonTypeId)
    actionForm.actualReasonId = row.actualReasonId ?? row.reasonId ?? undefined
  }
  actionDialogVisible.value = true
}
async function submitAction() {
  const row = actionRow.value
  if (!row) return
  if (actionType.value === 'confirm' && !actionForm.actualReasonId) {
    ElMessage.warning('请选择实际原因'); return
  }
  if (actionType.value === 'transfer'
    && (!actionForm.actionContent?.trim() || (!actionForm.targetUserId && !actionForm.targetRoleCode))) {
    ElMessage.warning('转派必须填写原因，并指定目标用户或角色'); return
  }
  if (actionType.value === 'complete'
    && (!actionForm.actualReasonId || !actionForm.processingResult?.trim())) {
    ElMessage.warning('完成处理必须填写实际原因和处理结果'); return
  }
  await actionAndonEvent(row.id, actionType.value, actionForm)
  ElMessage.success(`${actionTitles[actionType.value]}成功`)
  actionDialogVisible.value = false; await eventTable.refresh()
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AndonEvent>()
async function openDetail(row: AndonEvent | Record<string, any>) {
  detailVisible.value = true; detailLoading.value = true
  try { detail.value = await getAndonEvent(row.id) } finally { detailLoading.value = false }
}
</script>

<template>
  <div class="page-container">
    <PageHeader title="安灯管理" description="异常类型与响应规则配置，现场异常发起、处理、升级和关闭全程留痕" />
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="安灯事件" name="events">
          <el-form inline :model="eventFilters" class="filters"><el-form-item label="关键字"><el-input v-model="eventFilters.keyword" clearable /></el-form-item><el-form-item label="安灯类型"><el-select v-model="eventFilters.andonTypeId" clearable filterable><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="状态"><el-select v-model="eventFilters.eventStatus" clearable><el-option v-for="o in EVENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="级别"><el-select v-model="eventFilters.severity" clearable><el-option v-for="o in SEVERITY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="eventTable.query(eventFilters)">查询</el-button><el-button @click="eventTable.reset()">重置</el-button></el-form-item></el-form>
          <div class="toolbar"><el-button type="primary" @click="openEventDialog">发起安灯</el-button></div>
          <el-table v-loading="eventTable.loading.value" :data="eventTable.data.value" border><el-table-column prop="eventNo" label="异常单号" width="150" /><el-table-column prop="andonTypeName" label="安灯类型" width="130" /><el-table-column label="级别" width="80"><template #default="{ row }"><StatusTag :status="row.severity" :status-map="SEVERITY_MAP" /></template></el-table-column><el-table-column prop="description" label="异常描述" min-width="220" show-overflow-tooltip /><el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag :status="row.eventStatus" :status-map="EVENT_STATUS_MAP" /></template></el-table-column><el-table-column label="超时" width="100"><template #default="{ row }"><StatusTag :status="row.timeoutStatus" :status-map="TIMEOUT_STATUS_MAP" /></template></el-table-column><el-table-column label="灯控" width="100"><template #default="{ row }"><StatusTag :status="row.lightStatus" :status-map="LIGHT_STATUS_MAP" /></template></el-table-column><el-table-column prop="assignedRoleCode" label="当前处理角色" width="140" /><el-table-column prop="createTime" label="发起时间" width="170" /><el-table-column label="操作" width="330" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详情</el-button><el-button v-if="row.eventStatus === 'PENDING_CONFIRMATION'" link type="primary" @click="openAction(row, 'confirm')">确认</el-button><el-button v-if="row.eventStatus === 'CONFIRMED'" link type="success" @click="openAction(row, 'start-processing')">开始处理</el-button><el-button v-if="['CONFIRMED','PROCESSING'].includes(row.eventStatus)" link type="warning" @click="openAction(row, 'transfer')">转派</el-button><el-button v-if="row.eventStatus === 'PROCESSING'" link type="success" @click="openAction(row, 'complete')">完成</el-button><PermissionButton v-if="row.eventStatus === 'WAITING_CLOSE'" link type="success" :roles="ANDON_CLOSE_ROLES" @click="openAction(row, 'close')">关闭</PermissionButton><el-button v-if="['CONFIRMED','PROCESSING'].includes(row.eventStatus) && row.timeoutStatus !== 'ESCALATED'" link type="danger" @click="openAction(row, 'escalate')">升级</el-button></template></el-table-column></el-table>
          <el-pagination v-model:current-page="eventTable.pagination.value.pageNo" v-model:page-size="eventTable.pagination.value.pageSize" :total="eventTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="eventTable.refresh" />
        </el-tab-pane>

        <el-tab-pane label="安灯类型" name="types">
          <el-form inline :model="typeFilters" class="filters"><el-form-item label="关键字"><el-input v-model="typeFilters.keyword" clearable /></el-form-item><el-form-item label="异常类别"><el-select v-model="typeFilters.exceptionCategory" clearable><el-option v-for="o in EXCEPTION_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="处理方式"><el-select v-model="typeFilters.handlingMode" clearable><el-option v-for="o in HANDLING_MODE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="状态"><el-select v-model="typeFilters.enabledStatus" clearable><el-option v-for="o in ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="typeTable.query(typeFilters)">查询</el-button><el-button @click="typeTable.reset()">重置</el-button></el-form-item></el-form>
          <div class="toolbar"><PermissionButton :roles="ANDON_MANAGE_ROLES" type="primary" @click="openTypeDialog()">新增安灯类型</PermissionButton></div>
          <el-table v-loading="typeTable.loading.value" :data="typeTable.data.value" border><el-table-column prop="typeCode" label="类型编码" width="140" /><el-table-column prop="typeName" label="类型名称" min-width="150" /><el-table-column label="异常类别" width="120"><template #default="{ row }">{{ EXCEPTION_CATEGORY_TEXT[row.exceptionCategory] ?? row.exceptionCategory }}</template></el-table-column><el-table-column label="处理方式" width="110"><template #default="{ row }">{{ HANDLING_MODE_TEXT[row.handlingMode] ?? row.handlingMode }}</template></el-table-column><el-table-column prop="responseMinutes" label="响应时限(分)" width="120" /><el-table-column prop="responsibleRoleCode" label="责任角色" width="140" /><el-table-column prop="notificationChannels" label="通知渠道" min-width="140" /><el-table-column label="状态" width="80"><template #default="{ row }"><StatusTag :status="row.enabledStatus" :status-map="ENABLE_STATUS_MAP" /></template></el-table-column><el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><PermissionButton link type="primary" :roles="ANDON_MANAGE_ROLES" @click="openTypeDialog(row)">编辑</PermissionButton><PermissionButton link type="danger" :roles="ANDON_MANAGE_ROLES" @click="removeType(row)">删除</PermissionButton></template></el-table-column></el-table>
          <el-pagination v-model:current-page="typeTable.pagination.value.pageNo" v-model:page-size="typeTable.pagination.value.pageSize" :total="typeTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="typeTable.refresh" />
        </el-tab-pane>

        <el-tab-pane label="处理配置" name="configurations">
          <el-form inline :model="configFilters" class="filters"><el-form-item label="安灯类型"><el-select v-model="configFilters.andonTypeId" clearable filterable><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="产线"><el-select v-model="configFilters.productionLineId" clearable filterable><el-option v-for="o in lineOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="状态"><el-select v-model="configFilters.enabledStatus" clearable><el-option v-for="o in ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="configTable.query(configFilters)">查询</el-button><el-button @click="configTable.reset()">重置</el-button></el-form-item></el-form>
          <div class="toolbar"><PermissionButton :roles="ANDON_MANAGE_ROLES" type="primary" @click="openConfigDialog()">新增处理配置</PermissionButton></div>
          <el-table v-loading="configTable.loading.value" :data="configTable.data.value" border><el-table-column prop="andonTypeName" label="安灯类型" min-width="160" /><el-table-column label="适用产线" min-width="180"><template #default="{ row }">{{ optionLabel(lineOptions, row.productionLineId) }}</template></el-table-column><el-table-column prop="handlerUserId" label="处理人 ID" width="100" /><el-table-column prop="handlerRoleCode" label="处理角色" width="140" /><el-table-column prop="responseMinutes" label="响应(分)" width="90" /><el-table-column prop="escalationMinutes" label="升级(分)" width="90" /><el-table-column prop="notificationChannels" label="通知渠道" min-width="130" /><el-table-column label="状态" width="80"><template #default="{ row }"><StatusTag :status="row.enabledStatus" :status-map="ENABLE_STATUS_MAP" /></template></el-table-column><el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><PermissionButton link type="primary" :roles="ANDON_MANAGE_ROLES" @click="openConfigDialog(row)">编辑</PermissionButton><PermissionButton link type="danger" :roles="ANDON_MANAGE_ROLES" @click="removeConfig(row)">删除</PermissionButton></template></el-table-column></el-table>
          <el-pagination v-model:current-page="configTable.pagination.value.pageNo" v-model:page-size="configTable.pagination.value.pageSize" :total="configTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="configTable.refresh" />
        </el-tab-pane>

        <el-tab-pane label="异常原因" name="reasons">
          <el-form inline :model="reasonFilters" class="filters"><el-form-item label="关键字"><el-input v-model="reasonFilters.keyword" clearable /></el-form-item><el-form-item label="安灯类型"><el-select v-model="reasonFilters.andonTypeId" clearable filterable><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="状态"><el-select v-model="reasonFilters.enabledStatus" clearable><el-option v-for="o in ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="reasonTable.query(reasonFilters)">查询</el-button><el-button @click="reasonTable.reset()">重置</el-button></el-form-item></el-form>
          <div class="toolbar"><PermissionButton :roles="ANDON_MANAGE_ROLES" type="primary" @click="openReasonDialog()">新增异常原因</PermissionButton></div>
          <el-table v-loading="reasonTable.loading.value" :data="reasonTable.data.value" border><el-table-column prop="reasonCode" label="原因编码" width="140" /><el-table-column prop="reasonName" label="原因名称" min-width="170" /><el-table-column prop="andonTypeName" label="安灯类型" min-width="150" /><el-table-column prop="reasonDescription" label="原因说明" min-width="240" show-overflow-tooltip /><el-table-column label="状态" width="80"><template #default="{ row }"><StatusTag :status="row.enabledStatus" :status-map="ENABLE_STATUS_MAP" /></template></el-table-column><el-table-column prop="updateTime" label="更新时间" width="170" /><el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><PermissionButton link type="primary" :roles="ANDON_MANAGE_ROLES" @click="openReasonDialog(row)">编辑</PermissionButton><PermissionButton link type="danger" :roles="ANDON_MANAGE_ROLES" @click="removeReason(row)">删除</PermissionButton></template></el-table-column></el-table>
          <el-pagination v-model:current-page="reasonTable.pagination.value.pageNo" v-model:page-size="reasonTable.pagination.value.pageSize" :total="reasonTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="reasonTable.refresh" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="typeDialogVisible" :title="typeEditingId ? '编辑安灯类型' : '新增安灯类型'" width="720px"><el-form :model="typeForm" label-width="110px"><el-row :gutter="16"><el-col :span="12"><el-form-item label="类型编码" required><el-input v-model="typeForm.typeCode" :disabled="!!typeEditingId" maxlength="32" /></el-form-item></el-col><el-col :span="12"><el-form-item label="类型名称" required><el-input v-model="typeForm.typeName" maxlength="128" /></el-form-item></el-col><el-col :span="12"><el-form-item label="异常类别" required><el-select v-model="typeForm.exceptionCategory"><el-option v-for="o in EXCEPTION_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="处理方式" required><el-select v-model="typeForm.handlingMode"><el-option v-for="o in HANDLING_MODE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="响应时限"><el-input-number v-model="typeForm.responseMinutes" :min="1" :max="10080" /></el-form-item></el-col><el-col :span="12"><el-form-item label="责任角色"><el-select v-model="typeForm.responsibleRoleCode" clearable><el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="通知渠道"><el-select v-model="typeForm.notificationChannels" multiple><el-option v-for="o in NOTIFICATION_CHANNEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="6"><el-form-item label="设备灯控"><el-switch v-model="typeForm.lightControlEnabled" /></el-form-item></el-col><el-col :span="6"><el-form-item label="状态"><el-switch v-model="typeForm.enabledStatus" :active-value="1" :inactive-value="0" /></el-form-item></el-col></el-row><el-form-item label="备注"><el-input v-model="typeForm.remark" type="textarea" maxlength="255" /></el-form-item></el-form><template #footer><el-button @click="typeDialogVisible = false">取消</el-button><el-button type="primary" @click="submitType">保存</el-button></template></el-dialog>

    <el-dialog v-model="reasonDialogVisible" :title="reasonEditingId ? '编辑异常原因' : '新增异常原因'" width="620px"><el-form :model="reasonForm" label-width="110px"><el-form-item label="原因编码" required><el-input v-model="reasonForm.reasonCode" :disabled="!!reasonEditingId" maxlength="32" /></el-form-item><el-form-item label="原因名称" required><el-input v-model="reasonForm.reasonName" maxlength="128" /></el-form-item><el-form-item label="安灯类型" required><el-select v-model="reasonForm.andonTypeId" filterable><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="原因说明"><el-input v-model="reasonForm.reasonDescription" type="textarea" maxlength="500" /></el-form-item><el-form-item label="状态"><el-switch v-model="reasonForm.enabledStatus" :active-value="1" :inactive-value="0" /></el-form-item></el-form><template #footer><el-button @click="reasonDialogVisible = false">取消</el-button><el-button type="primary" @click="submitReason">保存</el-button></template></el-dialog>

    <el-dialog v-model="configDialogVisible" :title="configEditingId ? '编辑处理配置' : '新增处理配置'" width="760px"><el-form :model="configForm" label-width="110px"><el-row :gutter="16"><el-col :span="12"><el-form-item label="安灯类型" required><el-select v-model="configForm.andonTypeId" filterable><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="适用产线"><el-select v-model="configForm.productionLineId" clearable filterable placeholder="空表示全局"><el-option v-for="o in lineOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="处理人 ID"><el-input-number v-model="configForm.handlerUserId" :min="1" /></el-form-item></el-col><el-col :span="12"><el-form-item label="处理角色"><el-select v-model="configForm.handlerRoleCode" clearable><el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="响应时限" required><el-input-number v-model="configForm.responseMinutes" :min="1" :max="10080" /><span class="unit">分钟</span></el-form-item></el-col><el-col :span="12"><el-form-item label="升级时限"><el-input-number v-model="configForm.escalationMinutes" :min="1" :max="10080" /><span class="unit">分钟</span></el-form-item></el-col><el-col :span="12"><el-form-item label="升级用户 ID"><el-input-number v-model="configForm.escalationUserId" :min="1" /></el-form-item></el-col><el-col :span="12"><el-form-item label="升级角色"><el-select v-model="configForm.escalationRoleCode" clearable><el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col></el-row><el-form-item label="通知渠道" required><el-select v-model="configForm.notificationChannels" multiple><el-option v-for="o in NOTIFICATION_CHANNEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="状态"><el-switch v-model="configForm.enabledStatus" :active-value="1" :inactive-value="0" /></el-form-item><el-form-item label="备注"><el-input v-model="configForm.remark" type="textarea" maxlength="255" /></el-form-item></el-form><template #footer><el-button @click="configDialogVisible = false">取消</el-button><el-button type="primary" @click="submitConfig">保存</el-button></template></el-dialog>

    <el-dialog v-model="eventDialogVisible" title="发起安灯异常" width="760px"><el-form :model="eventForm" label-width="110px"><el-row :gutter="16"><el-col :span="12"><el-form-item label="安灯类型" required><el-select v-model="eventForm.andonTypeId" filterable><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="异常原因"><el-select v-model="eventForm.reasonId" clearable filterable><el-option v-for="o in reasonOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="来源渠道" required><el-select v-model="eventForm.sourceChannel"><el-option v-for="o in SOURCE_CHANNEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="异常级别"><el-select v-model="eventForm.severity"><el-option v-for="o in SEVERITY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="车间"><el-select v-model="eventForm.workshopId" clearable filterable><el-option v-for="o in workshopOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="产线"><el-select v-model="eventForm.productionLineId" clearable filterable><el-option v-for="o in lineOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="设备"><el-select v-model="eventForm.equipmentId" clearable filterable><el-option v-for="o in equipmentOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="产品批次"><el-input v-model="eventForm.batchNo" maxlength="64" /></el-form-item></el-col><el-col :span="8"><el-form-item label="生产工单 ID"><el-input-number v-model="eventForm.workOrderId" :min="1" /></el-form-item></el-col><el-col :span="8"><el-form-item label="生产任务 ID"><el-input-number v-model="eventForm.productionTaskId" :min="1" /></el-form-item></el-col><el-col :span="8"><el-form-item label="工序 ID"><el-input-number v-model="eventForm.processId" :min="1" /></el-form-item></el-col></el-row><el-form-item label="异常描述" required><el-input v-model="eventForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item><el-form-item label="附件地址"><el-input v-model="eventForm.attachmentUrls" type="textarea" placeholder="JSON 字符串，可选" /></el-form-item></el-form><template #footer><el-button @click="eventDialogVisible = false">取消</el-button><el-button type="primary" @click="submitEvent">发起</el-button></template></el-dialog>

    <el-dialog v-model="actionDialogVisible" :title="actionTitles[actionType]" width="620px"><el-form :model="actionForm" label-width="110px"><el-form-item v-if="['confirm','complete'].includes(actionType)" label="实际原因" required><el-select v-model="actionForm.actualReasonId" filterable><el-option v-for="o in reasonOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><template v-if="['transfer','escalate'].includes(actionType)"><el-form-item label="目标用户 ID"><el-input-number v-model="actionForm.targetUserId" :min="1" /></el-form-item><el-form-item label="目标角色"><el-select v-model="actionForm.targetRoleCode" clearable><el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></template><el-form-item v-if="actionType === 'complete'" label="处理结果" required><el-input v-model="actionForm.processingResult" type="textarea" maxlength="1000" /></el-form-item><el-row v-if="actionType === 'complete'" :gutter="16"><el-col :span="12"><el-form-item label="影响时长"><el-input-number v-model="actionForm.impactMinutes" :min="0" /><span class="unit">分钟</span></el-form-item></el-col><el-col :span="12"><el-form-item label="影响数量"><el-input-number v-model="actionForm.affectedQuantity" :min="0" /></el-form-item></el-col></el-row><el-form-item label="操作说明" :required="actionType === 'transfer'"><el-input v-model="actionForm.actionContent" type="textarea" maxlength="1000" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="actionDialogVisible = false">取消</el-button><el-button type="primary" @click="submitAction">确认</el-button></template></el-dialog>

    <el-drawer v-model="detailVisible" title="安灯异常详情" size="720px"><div v-loading="detailLoading"><el-descriptions v-if="detail" :column="2" border><el-descriptions-item label="异常单号">{{ detail.eventNo }}</el-descriptions-item><el-descriptions-item label="安灯类型">{{ detail.andonTypeName }}</el-descriptions-item><el-descriptions-item label="来源">{{ SOURCE_CHANNEL_TEXT[detail.sourceChannel] ?? detail.sourceChannel }}</el-descriptions-item><el-descriptions-item label="级别"><StatusTag :status="detail.severity" :status-map="SEVERITY_MAP" /></el-descriptions-item><el-descriptions-item label="状态"><StatusTag :status="detail.eventStatus" :status-map="EVENT_STATUS_MAP" /></el-descriptions-item><el-descriptions-item label="灯控"><StatusTag :status="detail.lightStatus" :status-map="LIGHT_STATUS_MAP" /></el-descriptions-item><el-descriptions-item label="异常描述" :span="2">{{ detail.description }}</el-descriptions-item><el-descriptions-item label="处理结果" :span="2">{{ detail.processingResult || '-' }}</el-descriptions-item><el-descriptions-item label="灯控说明" :span="2">{{ detail.lightMessage || '-' }}</el-descriptions-item></el-descriptions><h4>处理过程</h4><el-table v-if="detail" :data="detail.processLogs" border><el-table-column label="动作" width="100"><template #default="{ row }">{{ ACTION_TYPE_TEXT[row.actionType] ?? row.actionType }}</template></el-table-column><el-table-column prop="fromStatus" label="原状态" width="130" /><el-table-column prop="toStatus" label="新状态" width="130" /><el-table-column prop="operatorId" label="操作人" width="90" /><el-table-column prop="actionContent" label="说明" min-width="180" /><el-table-column prop="createTime" label="时间" width="170" /></el-table></div></el-drawer>
  </div>
</template>

<style scoped>
.filters { margin-bottom: 4px; }
.filters :deep(.el-select), .filters :deep(.el-input) { width: 180px; }
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.el-pagination { justify-content: flex-end; margin-top: 16px; }
.unit { margin-left: 6px; color: var(--el-text-color-secondary); }
h4 { margin: 20px 0 12px; }
</style>
