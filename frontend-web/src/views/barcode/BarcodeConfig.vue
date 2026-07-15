<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import type { OptionItem } from '@/types/components'
import { ENABLE_STATUS_OPTIONS } from '@/constants/production'
import {
  BARCODE_ENTITY_STATUS_MAP,
  BARCODE_MANAGE_ROLES,
  BARCODE_MODE_OPTIONS,
  BARCODE_MODE_TEXT,
  BARCODE_OBJECT_OPTIONS,
  BARCODE_OBJECT_TEXT,
  BARCODE_SOURCE_OPTIONS,
  BARCODE_SOURCE_TEXT,
  RULE_ITEM_TYPE_OPTIONS,
  RULE_ITEM_TYPE_TEXT,
  SERIAL_RESET_OPTIONS,
  TEMPLATE_FIELD_TYPE_OPTIONS,
  TEMPLATE_FIELD_TYPE_TEXT,
} from '@/constants/barcode'
import {
  changeBarcodeApplicationRuleStatus,
  changeBarcodeRuleStatus,
  changeBarcodeTemplateStatus,
  changeBarcodeTypeStatus,
  createBarcodeApplicationRule,
  createBarcodeRule,
  createBarcodeTemplate,
  createBarcodeType,
  deleteBarcodeApplicationRule,
  deleteBarcodeRule,
  deleteBarcodeType,
  getBarcodeApplicationRulePage,
  getBarcodeRulePage,
  getBarcodeTemplatePage,
  getBarcodeTypeOptions,
  getBarcodeTypePage,
  previewBarcodeRule,
  previewBarcodeTemplate,
  updateBarcodeApplicationRule,
  updateBarcodeRule,
  updateBarcodeTemplate,
  updateBarcodeType,
  validateBarcodeRule,
} from '@/api/barcode'
import type {
  BarcodeApplicationRule,
  BarcodeRule,
  BarcodeRuleItem,
  BarcodeTemplate,
  BarcodeTemplateField,
  BarcodeType,
} from '@/api/barcode'
import { loadMaterialOptions, loadProductOptions } from '@/api/production/options'

defineOptions({ name: 'BarcodeConfig' })

const activeTab = ref('types')
const typeOptions = ref<OptionItem[]>([])
const ruleOptions = ref<OptionItem[]>([])
const templateOptions = ref<OptionItem[]>([])
const productOptions = ref<OptionItem[]>([])
const materialOptions = ref<OptionItem[]>([])
function optionLabel(options: OptionItem[], id: number | null | undefined) { return options.find((item) => item.value === id)?.label ?? (id ? `#${id}` : '-') }
async function reloadTypes() { const rows = await getBarcodeTypeOptions(); typeOptions.value = rows.map((item) => ({ label: `${item.typeCode} ${item.typeName}`, value: item.id })) }
async function reloadRules() { const page = await getBarcodeRulePage({ pageNo: 1, pageSize: 100, status: 1 }); ruleOptions.value = page.list.map((item) => ({ label: `${item.ruleCode} ${item.ruleName}`, value: item.id })) }
async function reloadTemplates() { const page = await getBarcodeTemplatePage({ pageNo: 1, pageSize: 100, status: 1 }); templateOptions.value = page.list.map((item) => ({ label: `${item.templateCode} ${item.templateName} (${item.version})`, value: item.id })) }
onMounted(async () => {
  const results = await Promise.allSettled([reloadTypes(), reloadRules(), reloadTemplates(), loadProductOptions(), loadMaterialOptions()])
  if (results[3].status === 'fulfilled') productOptions.value = results[3].value
  if (results[4].status === 'fulfilled') materialOptions.value = results[4].value
})

// 条码类型
const typeFilters = reactive({ typeCode: '', typeName: '', status: undefined as number | undefined })
const typeTable = useTable({ fetcher: getBarcodeTypePage })
const typeVisible = ref(false); const typeEditingId = ref<number>()
const typeForm = reactive({ typeCode: '', typeName: '', applyObject: '' })
function openType(row?: BarcodeType | Record<string, any>) { typeEditingId.value = row?.id; Object.assign(typeForm, row ? { typeCode: row.typeCode, typeName: row.typeName, applyObject: row.applyObject ?? '' } : { typeCode: '', typeName: '', applyObject: '' }); typeVisible.value = true }
async function saveType() { if (!typeForm.typeCode || !typeForm.typeName) { ElMessage.warning('请填写类型编码和名称'); return } if (typeEditingId.value) await updateBarcodeType(typeEditingId.value, typeForm); else await createBarcodeType(typeForm); ElMessage.success('条码类型已保存'); typeVisible.value = false; await Promise.all([typeTable.refresh(), reloadTypes()]) }
async function toggleType(row: BarcodeType | Record<string, any>) { await changeBarcodeTypeStatus(row.id, row.status === 0); ElMessage.success(row.status === 0 ? '已启用' : '已停用'); await Promise.all([typeTable.refresh(), reloadTypes()]) }
async function removeType(row: BarcodeType | Record<string, any>) { await ElMessageBox.confirm(`确认删除“${row.typeName}”？`, '删除确认', { type: 'warning' }); await deleteBarcodeType(row.id); ElMessage.success('已删除'); await Promise.all([typeTable.refresh(), reloadTypes()]) }

// 条码规则
const ruleFilters = reactive({ ruleCode: '', ruleName: '', barcodeTypeId: undefined as number | undefined, status: undefined as number | undefined })
const ruleTable = useTable({ fetcher: getBarcodeRulePage })
const ruleVisible = ref(false); const ruleEditingId = ref<number>(); const previewVisible = ref(false); const previewData = ref<Record<string, any>>()
const ruleForm = reactive({ ruleCode: '', ruleName: '', barcodeTypeId: 0, serialLength: 6, serialResetCycle: 1, items: [] as BarcodeRuleItem[] })
function newRuleItem(type = 1): BarcodeRuleItem { return { seq: ruleForm.items.length + 1, itemType: type, itemValue: type === 3 ? 'productCode' : '', dateFormat: type === 2 ? 'yyyyMMdd' : '', itemLength: undefined } }
function openRule(row?: BarcodeRule | Record<string, any>) { ruleEditingId.value = row?.id; Object.assign(ruleForm, row ? { ruleCode: row.ruleCode, ruleName: row.ruleName, barcodeTypeId: row.barcodeTypeId, serialLength: row.serialLength, serialResetCycle: row.serialResetCycle, items: row.items.map((item: BarcodeRuleItem) => ({ ...item })) } : { ruleCode: '', ruleName: '', barcodeTypeId: 0, serialLength: 6, serialResetCycle: 1, items: [{ seq: 1, itemType: 1, itemValue: 'BDM' }, { seq: 2, itemType: 2, dateFormat: 'yyyyMMdd' }, { seq: 3, itemType: 4 }] }); ruleVisible.value = true }
function removeRuleItem(index: number) { ruleForm.items.splice(index, 1); ruleForm.items.forEach((item, i) => { item.seq = i + 1 }) }
async function saveRule() { if (!ruleForm.ruleCode || !ruleForm.ruleName || !ruleForm.barcodeTypeId || !ruleForm.items.length) { ElMessage.warning('请完整填写规则主信息和组成项'); return } const validation = await validateBarcodeRule({ serialLength: ruleForm.serialLength, items: ruleForm.items }); if (!validation.valid) { ElMessage.error(validation.errors.join('；')); return } const payload = { ...ruleForm, items: ruleForm.items.map((item) => ({ ...item })) }; if (ruleEditingId.value) await updateBarcodeRule(ruleEditingId.value, payload as any); else await createBarcodeRule(payload as any); ElMessage.success('条码规则已保存'); ruleVisible.value = false; await Promise.all([ruleTable.refresh(), reloadRules()]) }
async function showRulePreview() { const result = await previewBarcodeRule({ serialLength: ruleForm.serialLength, items: ruleForm.items, sampleProductCode: 'BD-100', sampleLineCode: 'LINE-01' }); previewData.value = result; previewVisible.value = true }
async function toggleRule(row: BarcodeRule | Record<string, any>) { await changeBarcodeRuleStatus(row.id, row.status === 0); ElMessage.success(row.status === 0 ? '已启用' : '已停用'); await Promise.all([ruleTable.refresh(), reloadRules()]) }
async function removeRule(row: BarcodeRule | Record<string, any>) { await ElMessageBox.confirm(`确认删除规则“${row.ruleName}”？`, '删除确认', { type: 'warning' }); await deleteBarcodeRule(row.id); ElMessage.success('已删除'); await Promise.all([ruleTable.refresh(), reloadRules()]) }

// 标签模板
const templateFilters = reactive({ templateCode: '', templateName: '', status: undefined as number | undefined })
const templateTable = useTable({ fetcher: getBarcodeTemplatePage })
const templateVisible = ref(false); const templateEditingId = ref<number>()
const templateForm = reactive({ templateCode: '', templateName: '', paperWidth: 60, paperHeight: 40, fields: [] as BarcodeTemplateField[] })
function newTemplateField(type = 1): BarcodeTemplateField { return { fieldName: type === 2 ? '条码值' : '文本', fieldType: type, dataSource: type === 2 ? 'barcodeValue' : 'productName', posX: 2, posY: 2, fontSize: 10 } }
function openTemplate(row?: BarcodeTemplate | Record<string, any>) { templateEditingId.value = row?.id; Object.assign(templateForm, row ? { templateCode: row.templateCode, templateName: row.templateName, paperWidth: row.paperWidth, paperHeight: row.paperHeight, fields: row.fields.map((item: BarcodeTemplateField) => ({ ...item })) } : { templateCode: '', templateName: '', paperWidth: 60, paperHeight: 40, fields: [newTemplateField(2)] }); templateVisible.value = true }
async function saveTemplate() { if (!templateForm.templateCode || !templateForm.templateName || !templateForm.fields.length) { ElMessage.warning('请完整填写模板信息和字段'); return } if (!templateForm.fields.some((item) => [2, 3].includes(item.fieldType))) { ElMessage.warning('模板必须包含条码或二维码字段'); return } const payload = { ...templateForm, fields: templateForm.fields.map((item) => ({ ...item })) }; if (templateEditingId.value) await updateBarcodeTemplate(templateEditingId.value, payload as any); else await createBarcodeTemplate(payload as any); ElMessage.success('标签模板已保存'); templateVisible.value = false; await Promise.all([templateTable.refresh(), reloadTemplates()]) }
async function showTemplatePreview(row: BarcodeTemplate | Record<string, any>) { previewData.value = await previewBarcodeTemplate(row.id, 'BDM20260715000001', { productName: '比赛级羽毛球', batchNo: 'B20260715' }); previewVisible.value = true }
async function toggleTemplate(row: BarcodeTemplate | Record<string, any>) { await changeBarcodeTemplateStatus(row.id, row.status === 0); ElMessage.success(row.status === 0 ? '已启用' : '已停用'); await Promise.all([templateTable.refresh(), reloadTemplates()]) }

// 应用规则
const appFilters = reactive({ objectType: undefined as number | undefined, barcodeTypeId: undefined as number | undefined, sourceType: undefined as number | undefined, status: undefined as number | undefined })
const appTable = useTable({ fetcher: getBarcodeApplicationRulePage })
const appVisible = ref(false); const appEditingId = ref<number>()
const appForm = reactive({ objectType: 1, productId: undefined as number | undefined, materialId: undefined as number | undefined, barcodeTypeId: 0, barcodeMode: 2, ruleId: undefined as number | undefined, templateId: 0, sourceType: 1, defaultFlag: true })
function openApp(row?: BarcodeApplicationRule | Record<string, any>) { appEditingId.value = row?.id; Object.assign(appForm, row ? { objectType: row.objectType, productId: row.productId ?? undefined, materialId: row.materialId ?? undefined, barcodeTypeId: row.barcodeTypeId, barcodeMode: row.barcodeMode, ruleId: row.ruleId ?? undefined, templateId: row.templateId, sourceType: row.sourceType, defaultFlag: row.defaultFlag } : { objectType: 1, productId: undefined, materialId: undefined, barcodeTypeId: 0, barcodeMode: 2, ruleId: undefined, templateId: 0, sourceType: 1, defaultFlag: true }); appVisible.value = true }
async function saveApp() { if (!appForm.barcodeTypeId || !appForm.templateId || (appForm.objectType === 1 ? !appForm.productId : !appForm.materialId) || (appForm.sourceType === 1 && !appForm.ruleId)) { ElMessage.warning('请完整填写应用对象、类型、规则和模板'); return } const payload = { ...appForm, productId: appForm.objectType === 1 ? appForm.productId : null, materialId: appForm.objectType === 2 ? appForm.materialId : null, ruleId: appForm.sourceType === 1 ? appForm.ruleId : null }; if (appEditingId.value) await updateBarcodeApplicationRule(appEditingId.value, payload as any); else await createBarcodeApplicationRule(payload as any); ElMessage.success('应用规则已保存'); appVisible.value = false; await appTable.refresh() }
async function toggleApp(row: BarcodeApplicationRule | Record<string, any>) { await changeBarcodeApplicationRuleStatus(row.id, row.status === 0); ElMessage.success(row.status === 0 ? '已启用' : '已停用'); await appTable.refresh() }
async function removeApp(row: BarcodeApplicationRule | Record<string, any>) { await ElMessageBox.confirm('确认删除该应用规则？', '删除确认', { type: 'warning' }); await deleteBarcodeApplicationRule(row.id); ElMessage.success('已删除'); await appTable.refresh() }
</script>

<template>
  <div class="page-container">
    <PageHeader title="条码基础配置" description="条码类型、编码规则、标签模板与产品/物料应用规则" />
    <el-card shadow="never"><el-tabs v-model="activeTab">
      <el-tab-pane label="条码类型" name="types">
        <el-form inline :model="typeFilters" class="filters"><el-form-item label="类型编码"><el-input v-model="typeFilters.typeCode" clearable /></el-form-item><el-form-item label="类型名称"><el-input v-model="typeFilters.typeName" clearable /></el-form-item><el-form-item label="状态"><el-select v-model="typeFilters.status" clearable><el-option v-for="o in ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="typeTable.query(typeFilters)">查询</el-button><el-button @click="typeTable.reset()">重置</el-button></el-form-item></el-form>
        <div class="toolbar"><PermissionButton :roles="BARCODE_MANAGE_ROLES" type="primary" @click="openType()">新增类型</PermissionButton></div>
        <el-table v-loading="typeTable.loading.value" :data="typeTable.data.value" border><el-table-column prop="typeCode" label="类型编码" width="160" /><el-table-column prop="typeName" label="类型名称" min-width="180" /><el-table-column prop="applyObject" label="适用对象说明" min-width="220" /><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.status" :status-map="BARCODE_ENTITY_STATUS_MAP" /></template></el-table-column><el-table-column prop="updateTime" label="更新时间" width="170" /><el-table-column label="操作" width="190" fixed="right"><template #default="{ row }"><PermissionButton link type="primary" :roles="BARCODE_MANAGE_ROLES" @click="openType(row)">编辑</PermissionButton><PermissionButton link :type="row.status ? 'warning' : 'success'" :roles="BARCODE_MANAGE_ROLES" @click="toggleType(row)">{{ row.status ? '停用' : '启用' }}</PermissionButton><PermissionButton link type="danger" :roles="BARCODE_MANAGE_ROLES" @click="removeType(row)">删除</PermissionButton></template></el-table-column></el-table><el-pagination v-model:current-page="typeTable.pagination.value.pageNo" v-model:page-size="typeTable.pagination.value.pageSize" :total="typeTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="typeTable.refresh" />
      </el-tab-pane>
      <el-tab-pane label="条码规则" name="rules">
        <el-form inline :model="ruleFilters" class="filters"><el-form-item label="规则编码"><el-input v-model="ruleFilters.ruleCode" clearable /></el-form-item><el-form-item label="规则名称"><el-input v-model="ruleFilters.ruleName" clearable /></el-form-item><el-form-item label="条码类型"><el-select v-model="ruleFilters.barcodeTypeId" clearable><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="ruleTable.query(ruleFilters)">查询</el-button><el-button @click="ruleTable.reset()">重置</el-button></el-form-item></el-form>
        <div class="toolbar"><PermissionButton :roles="BARCODE_MANAGE_ROLES" type="primary" @click="openRule()">新增规则</PermissionButton></div>
        <el-table v-loading="ruleTable.loading.value" :data="ruleTable.data.value" border><el-table-column prop="ruleCode" label="规则编码" width="150" /><el-table-column prop="ruleName" label="规则名称" min-width="180" /><el-table-column label="条码类型" min-width="180"><template #default="{ row }">{{ optionLabel(typeOptions, row.barcodeTypeId) }}</template></el-table-column><el-table-column prop="serialLength" label="流水位数" width="100" /><el-table-column label="组成项" min-width="220"><template #default="{ row }">{{ row.items.map((i: any) => RULE_ITEM_TYPE_TEXT[i.itemType]).join(' + ') }}</template></el-table-column><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.status" :status-map="BARCODE_ENTITY_STATUS_MAP" /></template></el-table-column><el-table-column label="操作" width="190" fixed="right"><template #default="{ row }"><PermissionButton link type="primary" :roles="BARCODE_MANAGE_ROLES" @click="openRule(row)">编辑</PermissionButton><PermissionButton link :type="row.status ? 'warning' : 'success'" :roles="BARCODE_MANAGE_ROLES" @click="toggleRule(row)">{{ row.status ? '停用' : '启用' }}</PermissionButton><PermissionButton link type="danger" :roles="BARCODE_MANAGE_ROLES" @click="removeRule(row)">删除</PermissionButton></template></el-table-column></el-table><el-pagination v-model:current-page="ruleTable.pagination.value.pageNo" v-model:page-size="ruleTable.pagination.value.pageSize" :total="ruleTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="ruleTable.refresh" />
      </el-tab-pane>
      <el-tab-pane label="标签模板" name="templates">
        <el-form inline :model="templateFilters" class="filters"><el-form-item label="模板编码"><el-input v-model="templateFilters.templateCode" clearable /></el-form-item><el-form-item label="模板名称"><el-input v-model="templateFilters.templateName" clearable /></el-form-item><el-form-item label="状态"><el-select v-model="templateFilters.status" clearable><el-option v-for="o in ENABLE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="templateTable.query(templateFilters)">查询</el-button><el-button @click="templateTable.reset()">重置</el-button></el-form-item></el-form>
        <div class="toolbar"><PermissionButton :roles="BARCODE_MANAGE_ROLES" type="primary" @click="openTemplate()">新增模板</PermissionButton></div>
        <el-table v-loading="templateTable.loading.value" :data="templateTable.data.value" border><el-table-column prop="templateCode" label="模板编码" width="150" /><el-table-column prop="templateName" label="模板名称" min-width="180" /><el-table-column prop="version" label="版本" width="100" /><el-table-column label="纸张(mm)" width="130"><template #default="{ row }">{{ row.paperWidth }} × {{ row.paperHeight }}</template></el-table-column><el-table-column label="字段" min-width="200"><template #default="{ row }">{{ row.fields.map((i: any) => TEMPLATE_FIELD_TYPE_TEXT[i.fieldType]).join('、') }}</template></el-table-column><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.status" :status-map="BARCODE_ENTITY_STATUS_MAP" /></template></el-table-column><el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showTemplatePreview(row)">预览</el-button><PermissionButton link type="primary" :roles="BARCODE_MANAGE_ROLES" @click="openTemplate(row)">编辑</PermissionButton><PermissionButton link :type="row.status ? 'warning' : 'success'" :roles="BARCODE_MANAGE_ROLES" @click="toggleTemplate(row)">{{ row.status ? '停用' : '启用' }}</PermissionButton></template></el-table-column></el-table><el-pagination v-model:current-page="templateTable.pagination.value.pageNo" v-model:page-size="templateTable.pagination.value.pageSize" :total="templateTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="templateTable.refresh" />
      </el-tab-pane>
      <el-tab-pane label="应用规则" name="applications">
        <el-form inline :model="appFilters" class="filters"><el-form-item label="对象类型"><el-select v-model="appFilters.objectType" clearable><el-option v-for="o in BARCODE_OBJECT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="条码类型"><el-select v-model="appFilters.barcodeTypeId" clearable><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item label="来源"><el-select v-model="appFilters.sourceType" clearable><el-option v-for="o in BARCODE_SOURCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="appTable.query(appFilters)">查询</el-button><el-button @click="appTable.reset()">重置</el-button></el-form-item></el-form>
        <div class="toolbar"><PermissionButton :roles="BARCODE_MANAGE_ROLES" type="primary" @click="openApp()">新增应用规则</PermissionButton></div>
        <el-table v-loading="appTable.loading.value" :data="appTable.data.value" border><el-table-column label="适用对象" min-width="200"><template #default="{ row }">{{ BARCODE_OBJECT_TEXT[row.objectType] }}：{{ optionLabel(row.objectType === 1 ? productOptions : materialOptions, row.objectType === 1 ? row.productId : row.materialId) }}</template></el-table-column><el-table-column label="条码类型" min-width="170"><template #default="{ row }">{{ optionLabel(typeOptions, row.barcodeTypeId) }}</template></el-table-column><el-table-column label="模式" width="90"><template #default="{ row }">{{ BARCODE_MODE_TEXT[row.barcodeMode] }}</template></el-table-column><el-table-column label="来源" width="100"><template #default="{ row }">{{ BARCODE_SOURCE_TEXT[row.sourceType] }}</template></el-table-column><el-table-column label="默认" width="70"><template #default="{ row }">{{ row.defaultFlag ? '是' : '否' }}</template></el-table-column><el-table-column prop="version" label="版本" width="90" /><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.status" :status-map="BARCODE_ENTITY_STATUS_MAP" /></template></el-table-column><el-table-column label="操作" width="190" fixed="right"><template #default="{ row }"><PermissionButton link type="primary" :roles="BARCODE_MANAGE_ROLES" @click="openApp(row)">编辑</PermissionButton><PermissionButton link :type="row.status ? 'warning' : 'success'" :roles="BARCODE_MANAGE_ROLES" @click="toggleApp(row)">{{ row.status ? '停用' : '启用' }}</PermissionButton><PermissionButton link type="danger" :roles="BARCODE_MANAGE_ROLES" @click="removeApp(row)">删除</PermissionButton></template></el-table-column></el-table><el-pagination v-model:current-page="appTable.pagination.value.pageNo" v-model:page-size="appTable.pagination.value.pageSize" :total="appTable.pagination.value.total" layout="total, sizes, prev, pager, next" @change="appTable.refresh" />
      </el-tab-pane>
    </el-tabs></el-card>

    <el-dialog v-model="typeVisible" :title="typeEditingId ? '编辑条码类型' : '新增条码类型'" width="520px"><el-form :model="typeForm" label-width="100px"><el-form-item label="类型编码" required><el-input v-model="typeForm.typeCode" :disabled="!!typeEditingId" maxlength="32" /></el-form-item><el-form-item label="类型名称" required><el-input v-model="typeForm.typeName" maxlength="64" /></el-form-item><el-form-item label="适用对象"><el-input v-model="typeForm.applyObject" maxlength="64" /></el-form-item></el-form><template #footer><el-button @click="typeVisible = false">取消</el-button><el-button type="primary" @click="saveType">保存</el-button></template></el-dialog>

    <el-dialog v-model="ruleVisible" :title="ruleEditingId ? '编辑条码规则' : '新增条码规则'" width="900px"><el-form :model="ruleForm" label-width="100px"><el-row :gutter="12"><el-col :span="8"><el-form-item label="规则编码" required><el-input v-model="ruleForm.ruleCode" :disabled="!!ruleEditingId" /></el-form-item></el-col><el-col :span="8"><el-form-item label="规则名称" required><el-input v-model="ruleForm.ruleName" /></el-form-item></el-col><el-col :span="8"><el-form-item label="条码类型" required><el-select v-model="ruleForm.barcodeTypeId"><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="流水位数" required><el-input-number v-model="ruleForm.serialLength" :min="1" :max="9" /></el-form-item></el-col><el-col :span="8"><el-form-item label="重置周期" required><el-select v-model="ruleForm.serialResetCycle"><el-option v-for="o in SERIAL_RESET_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col></el-row><div class="sub-toolbar"><strong>规则组成项</strong><el-button size="small" @click="ruleForm.items.push(newRuleItem())">添加组成项</el-button></div><el-table :data="ruleForm.items" border><el-table-column label="顺序" width="80"><template #default="{ row }"><el-input-number v-model="row.seq" :min="1" controls-position="right" /></template></el-table-column><el-table-column label="类型" width="130"><template #default="{ row }"><el-select v-model="row.itemType"><el-option v-for="o in RULE_ITEM_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></template></el-table-column><el-table-column label="常量/变量"><template #default="{ row }"><el-input v-model="row.itemValue" :disabled="![1,3].includes(row.itemType)" placeholder="变量支持 productCode / lineCode" /></template></el-table-column><el-table-column label="日期格式" width="150"><template #default="{ row }"><el-input v-model="row.dateFormat" :disabled="row.itemType !== 2" /></template></el-table-column><el-table-column label="段长度" width="110"><template #default="{ row }"><el-input-number v-model="row.itemLength" :min="1" :max="64" /></template></el-table-column><el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="removeRuleItem($index)">删除</el-button></template></el-table-column></el-table></el-form><template #footer><el-button @click="ruleVisible = false">取消</el-button><el-button @click="showRulePreview">校验并预览</el-button><el-button type="primary" @click="saveRule">保存</el-button></template></el-dialog>

    <el-dialog v-model="templateVisible" :title="templateEditingId ? '编辑标签模板' : '新增标签模板'" width="940px"><el-form :model="templateForm" label-width="100px"><el-row :gutter="12"><el-col :span="8"><el-form-item label="模板编码" required><el-input v-model="templateForm.templateCode" :disabled="!!templateEditingId" /></el-form-item></el-col><el-col :span="8"><el-form-item label="模板名称" required><el-input v-model="templateForm.templateName" /></el-form-item></el-col><el-col :span="4"><el-form-item label="宽(mm)" required><el-input-number v-model="templateForm.paperWidth" :min="0.01" /></el-form-item></el-col><el-col :span="4"><el-form-item label="高(mm)" required><el-input-number v-model="templateForm.paperHeight" :min="0.01" /></el-form-item></el-col></el-row><div class="sub-toolbar"><strong>模板字段</strong><el-button size="small" @click="templateForm.fields.push(newTemplateField())">添加字段</el-button></div><el-table :data="templateForm.fields" border><el-table-column label="字段名称" width="150"><template #default="{ row }"><el-input v-model="row.fieldName" /></template></el-table-column><el-table-column label="类型" width="120"><template #default="{ row }"><el-select v-model="row.fieldType"><el-option v-for="o in TEMPLATE_FIELD_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></template></el-table-column><el-table-column label="数据来源"><template #default="{ row }"><el-input v-model="row.dataSource" /></template></el-table-column><el-table-column label="X" width="100"><template #default="{ row }"><el-input-number v-model="row.posX" :min="0" /></template></el-table-column><el-table-column label="Y" width="100"><template #default="{ row }"><el-input-number v-model="row.posY" :min="0" /></template></el-table-column><el-table-column label="字号" width="100"><template #default="{ row }"><el-input-number v-model="row.fontSize" :min="1" /></template></el-table-column><el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="templateForm.fields.splice($index, 1)">删除</el-button></template></el-table-column></el-table></el-form><template #footer><el-button @click="templateVisible = false">取消</el-button><el-button type="primary" @click="saveTemplate">保存</el-button></template></el-dialog>

    <el-dialog v-model="appVisible" :title="appEditingId ? '编辑应用规则' : '新增应用规则'" width="700px"><el-form :model="appForm" label-width="110px"><el-row :gutter="12"><el-col :span="12"><el-form-item label="对象类型" required><el-select v-model="appForm.objectType"><el-option v-for="o in BARCODE_OBJECT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item :label="appForm.objectType === 1 ? '产品' : '物料'" required><el-select v-if="appForm.objectType === 1" v-model="appForm.productId" filterable><el-option v-for="o in productOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select><el-select v-else v-model="appForm.materialId" filterable><el-option v-for="o in materialOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="条码类型" required><el-select v-model="appForm.barcodeTypeId"><el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="条码模式" required><el-select v-model="appForm.barcodeMode"><el-option v-for="o in BARCODE_MODE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="条码来源" required><el-select v-model="appForm.sourceType"><el-option v-for="o in BARCODE_SOURCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="编码规则" :required="appForm.sourceType === 1"><el-select v-model="appForm.ruleId" clearable><el-option v-for="o in ruleOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="标签模板" required><el-select v-model="appForm.templateId"><el-option v-for="o in templateOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="默认规则"><el-switch v-model="appForm.defaultFlag" /></el-form-item></el-col></el-row></el-form><template #footer><el-button @click="appVisible = false">取消</el-button><el-button type="primary" @click="saveApp">保存</el-button></template></el-dialog>

    <el-dialog v-model="previewVisible" title="预览结果" width="620px"><el-descriptions v-if="previewData?.barcodeValue" :column="1" border><el-descriptions-item label="条码值"><el-text type="primary" size="large">{{ previewData.barcodeValue }}</el-text></el-descriptions-item><el-descriptions-item label="总长度">{{ previewData.totalLength }}</el-descriptions-item><el-descriptions-item label="流水容量">{{ previewData.serialCapacity }}</el-descriptions-item></el-descriptions><pre v-else>{{ JSON.stringify(previewData, null, 2) }}</pre></el-dialog>
  </div>
</template>

<style scoped>
.filters :deep(.el-input), .filters :deep(.el-select) { width: 180px; }
.toolbar, .sub-toolbar { display: flex; justify-content: flex-end; align-items: center; margin-bottom: 12px; }
.sub-toolbar { justify-content: space-between; margin-top: 8px; }
.el-pagination { justify-content: flex-end; margin-top: 16px; }
pre { max-height: 56vh; overflow: auto; white-space: pre-wrap; }
</style>
