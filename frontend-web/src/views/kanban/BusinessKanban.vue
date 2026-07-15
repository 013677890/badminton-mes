<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/base/PageHeader.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { useAutoRefresh } from '@/composables/useAutoRefresh'
import type { OptionItem } from '@/types/components'
import { loadLineOptions, loadWorkshopOptions } from '@/api/production/options'
import { getKanbanSnapshot, getRealtimeTasks } from '@/api/report'
import type { KanbanSnapshot, RealtimeTask } from '@/api/report'
import { SCENE_TASK_STATUS_MAP } from '@/constants/scene'

defineOptions({ name: 'BusinessKanban' })
const scope = ref<'central' | 'workshop' | 'line'>('central')
const scopeId = ref<number>()
const workshopOptions = ref<OptionItem[]>([])
const lineOptions = ref<OptionItem[]>([])
const snapshot = ref<KanbanSnapshot>()
const tasks = ref<RealtimeTask[]>([])

onMounted(async () => { [workshopOptions.value, lineOptions.value] = await Promise.all([loadWorkshopOptions(), loadLineOptions()]) })
async function loadBoard() {
  if (scope.value !== 'central' && !scopeId.value) { snapshot.value = undefined; tasks.value = []; return }
  const filters = scope.value === 'workshop' ? { workshopId: scopeId.value } : scope.value === 'line' ? { lineId: scopeId.value } : undefined
  ;[snapshot.value, tasks.value] = await Promise.all([getKanbanSnapshot(scope.value, scopeId.value), getRealtimeTasks(filters)])
}
const { lastUpdated, paused, refreshing, pause, resume, refreshNow } = useAutoRefresh(loadBoard, 30000)
function changeScope() { scopeId.value = undefined; void refreshNow() }
function changeScopeId() { void refreshNow() }
const overview = computed(() => snapshot.value?.overview)
function percentage(numerator = 0, denominator = 0) { return denominator > 0 ? Math.min(100, Math.round(numerator * 1000 / denominator) / 10) : 0 }
const completionRate = computed(() => percentage(overview.value?.goodQuantity, overview.value?.planQuantity))
const yieldRate = computed(() => percentage(overview.value?.goodQuantity, (overview.value?.goodQuantity || 0) + (overview.value?.defectQuantity || 0)))
const equipmentRate = computed(() => percentage(overview.value?.runningEquipmentCount, overview.value?.equipmentTotalCount))
const scopeName = computed(() => scope.value === 'central' ? '中控总览' : (scope.value === 'workshop' ? workshopOptions.value : lineOptions.value).find((item) => item.value === scopeId.value)?.label || '请选择范围')
async function toggleFullscreen() { try { if (document.fullscreenElement) await document.exitFullscreen(); else await document.documentElement.requestFullscreen() } catch { ElMessage.warning('浏览器不支持全屏显示') } }
</script>

<template>
  <div class="kanban-page">
    <PageHeader title="MES 业务看板" :description="`${scopeName} · 生产、质量、设备与安灯实时态势`">
      <template #extra>
        <span class="updated">更新：{{ lastUpdated?.toLocaleTimeString('zh-CN',{hour12:false}) || '-' }}</span>
        <el-button :loading="refreshing" @click="refreshNow">立即刷新</el-button>
        <el-button @click="paused ? resume() : pause()">{{ paused ? '恢复轮询' : '暂停轮询' }}</el-button>
        <el-button @click="toggleFullscreen">全屏</el-button>
      </template>
    </PageHeader>
    <el-card shadow="never" class="scope-card"><el-radio-group v-model="scope" @change="changeScope"><el-radio-button value="central">中控看板</el-radio-button><el-radio-button value="workshop">车间看板</el-radio-button><el-radio-button value="line">产线看板</el-radio-button></el-radio-group><el-select v-if="scope==='workshop'" v-model="scopeId" placeholder="选择车间" filterable @change="changeScopeId"><el-option v-for="o in workshopOptions" :key="o.value" v-bind="o" /></el-select><el-select v-if="scope==='line'" v-model="scopeId" placeholder="选择产线" filterable @change="changeScopeId"><el-option v-for="o in lineOptions" :key="o.value" v-bind="o" /></el-select><el-tag :type="snapshot?.dataStatus==='FRESH'?'success':'warning'">{{ snapshot?.dataStatus || '等待数据' }}</el-tag></el-card>
    <el-empty v-if="scope!=='central'&&!scopeId" description="请选择要展示的车间或产线" />
    <template v-else>
      <el-row :gutter="16" class="kpi-grid">
        <el-col v-for="item in [{label:'生产中任务',value:overview?.activeTaskCount||0,sub:`暂停 ${overview?.pausedTaskCount||0}`,tone:'blue'},{label:'计划数量',value:overview?.planQuantity||0,sub:`投入 ${overview?.inputQuantity||0}`,tone:'cyan'},{label:'良品数量',value:overview?.goodQuantity||0,sub:`不良 ${overview?.defectQuantity||0}`,tone:'green'},{label:'异常批次',value:overview?.abnormalBatchCount||0,sub:'需要现场关注',tone:'orange'},{label:'运行设备',value:overview?.runningEquipmentCount||0,sub:`总计 ${overview?.equipmentTotalCount||0}`,tone:'purple'},{label:'未关闭安灯',value:overview?.openAndonCount||0,sub:`严重 ${overview?.criticalAndonCount||0}`,tone:'red'}]" :key="item.label" :xs="12" :md="8" :lg="4"><div class="kpi" :class="`kpi--${item.tone}`"><span>{{ item.label }}</span><strong>{{ item.value.toLocaleString() }}</strong><small>{{ item.sub }}</small></div></el-col>
      </el-row>
      <el-row :gutter="16" class="section-row"><el-col :xs="24" :lg="8"><el-card shadow="never"><template #header>生产达成</template><div class="progress-item"><span>计划完成率</span><el-progress :percentage="completionRate" :stroke-width="14" /></div><div class="progress-item"><span>一次良品率</span><el-progress :percentage="yieldRate" status="success" :stroke-width="14" /></div></el-card></el-col><el-col :xs="24" :lg="8"><el-card shadow="never"><template #header>设备运行</template><div class="progress-item"><span>设备运行率</span><el-progress :percentage="equipmentRate" :stroke-width="14" /></div><div class="status-line"><span>不可用设备</span><strong class="danger">{{ overview?.unavailableEquipmentCount || 0 }}</strong></div></el-card></el-col><el-col :xs="24" :lg="8"><el-card shadow="never"><template #header>异常态势</template><div class="status-line"><span>严重安灯</span><strong class="danger">{{ overview?.criticalAndonCount || 0 }}</strong></div><div class="status-line"><span>异常批次</span><strong class="warning">{{ overview?.abnormalBatchCount || 0 }}</strong></div><div class="status-line"><span>暂停任务</span><strong>{{ overview?.pausedTaskCount || 0 }}</strong></div></el-card></el-col></el-row>
      <el-alert v-if="snapshot?.sourceWarnings?.length||overview?.warnings?.length" :title="[...(snapshot?.sourceWarnings||[]),...(overview?.warnings||[])].join('；')" type="warning" :closable="false" show-icon />
      <el-card shadow="never" class="task-card"><template #header><div class="card-header"><span>实时生产任务</span><el-tag>{{ tasks.length }} 项</el-tag></div></template><el-table :data="tasks" border stripe height="420"><el-table-column prop="taskNo" label="任务号" width="150" /><el-table-column prop="workOrderNo" label="工单号" width="150" /><el-table-column prop="productName" label="产品" min-width="160" /><el-table-column prop="workshopName" label="车间" width="120" /><el-table-column prop="lineName" label="产线" width="120" /><el-table-column prop="planQuantity" label="计划" width="80" /><el-table-column prop="inputQuantity" label="投入" width="80" /><el-table-column prop="goodQuantity" label="良品" width="80" /><el-table-column prop="defectQuantity" label="不良" width="80" /><el-table-column label="进度" width="150"><template #default="{ row }"><el-progress :percentage="percentage(row.finishQuantity,row.planQuantity)" :show-text="false" /></template></el-table-column><el-table-column label="状态" width="90"><template #default="{ row }"><StatusTag :status="row.taskStatus" :status-map="SCENE_TASK_STATUS_MAP" /></template></el-table-column><el-table-column label="异常" width="80"><template #default="{ row }"><el-tag :type="row.abnormal?'danger':'success'">{{ row.abnormal?'异常':'正常' }}</el-tag></template></el-table-column></el-table></el-card>
    </template>
  </div>
</template>

<style scoped>
.kanban-page { min-height:100%; padding:16px; background:#071426; color:#dbeafe; }.kanban-page :deep(.page-header),.kanban-page :deep(.el-card){ background:#0d2038; border-color:#1e3a5f; color:#dbeafe; }.updated{margin-right:12px;color:#93c5fd}.scope-card :deep(.el-card__body){display:flex;align-items:center;gap:14px}.scope-card .el-select{width:240px}.kpi-grid{margin-top:16px}.kpi{height:126px;padding:18px;border-radius:10px;background:linear-gradient(145deg,#102b48,#0b1d33);border-left:4px solid #3b82f6;display:flex;flex-direction:column;gap:7px}.kpi span,.kpi small{color:#93aeca}.kpi strong{font-size:30px;color:white}.kpi--green{border-color:#22c55e}.kpi--orange{border-color:#f59e0b}.kpi--red{border-color:#ef4444}.kpi--purple{border-color:#a855f7}.kpi--cyan{border-color:#06b6d4}.section-row{margin-top:16px}.section-row .el-card{height:220px}.progress-item{margin:12px 0 24px}.progress-item>span{display:block;margin-bottom:8px}.status-line{display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid #1e3a5f}.status-line strong{font-size:24px}.danger{color:#f87171}.warning{color:#fbbf24}.task-card{margin-top:16px}.card-header{display:flex;justify-content:space-between}.kanban-page :deep(.el-table){--el-table-bg-color:#0d2038;--el-table-tr-bg-color:#0d2038;--el-table-header-bg-color:#102b48;--el-table-text-color:#dbeafe;--el-table-header-text-color:#93c5fd;--el-table-border-color:#1e3a5f}
</style>
