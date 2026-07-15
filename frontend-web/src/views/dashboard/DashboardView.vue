<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/base/PageHeader.vue'
import StatCard from '@/components/base/StatCard.vue'
import { useAutoRefresh } from '@/composables/useAutoRefresh'
import { formatDateTime } from '@/utils/format'

defineOptions({ name: 'DashboardView' })

const router = useRouter()

interface DashboardStat {
  label: string
  value: number
  unit: string
  icon: string
  iconColor: string
  trend: number
}

const stats = ref<DashboardStat[]>([])

function randomBetween(min: number, max: number): number {
  return Math.round(min + Math.random() * (max - min))
}

/** 演示 useAutoRefresh：30s 自动刷新指标（真实场景替换为看板接口） */
const { lastUpdated, refreshing, refreshNow } = useAutoRefresh(async () => {
  await new Promise((resolve) => setTimeout(resolve, 300))
  stats.value = [
    {
      label: '今日产量（打）',
      value: randomBetween(4200, 5200),
      unit: '打',
      icon: 'Box',
      iconColor: '#409eff',
      trend: randomBetween(-8, 15),
    },
    {
      label: '在制工单',
      value: randomBetween(12, 30),
      unit: '张',
      icon: 'Tickets',
      iconColor: '#67c23a',
      trend: randomBetween(-5, 8),
    },
    {
      label: '设备稼动率',
      value: randomBetween(78, 96),
      unit: '%',
      icon: 'Odometer',
      iconColor: '#e6a23c',
      trend: randomBetween(-3, 6),
    },
    {
      label: '待处理异常',
      value: randomBetween(0, 9),
      unit: '条',
      icon: 'Warning',
      iconColor: '#f56c6c',
      trend: randomBetween(-20, 20),
    },
  ]
}, 30000)

interface DemoEntry {
  title: string
  description: string
  path: string
}

const businessEntries: DemoEntry[] = [
  {
    title: '生产工单',
    description: '工单全生命周期：创建 → 下达 → 生产 → 完工 → 关闭，进度与齐套一目了然',
    path: '/production/work-orders',
  },
  {
    title: '派工管理',
    description: '排产建议采纳、审核下发、超派校验与产线排程甘特',
    path: '/production/dispatch-orders',
  },
  {
    title: '欠料看板',
    description: '按物料聚合欠料汇总，下钻影响工单并登记处理',
    path: '/production/shortage-board',
  },
  {
    title: '工艺管理',
    description: '工序主档与 SOP / 不良原因维护，工艺路线主从编辑与审核生效',
    path: '/craft/processes',
  },
  {
    title: '计件工资',
    description: '单价规则 → 作业记录 → 结算计算 → 审核，明细可调整并全程留痕',
    path: '/wage/settlements',
  },
  {
    title: '基础资料',
    description: '产品 / 物料 / BOM / 车间 / 产线主档维护，乐观锁与引用校验',
    path: '/basedata/products',
  },
  {
    title: '系统管理',
    description: '用户账号、角色分配与密码重置（仅管理员），停用即强制下线',
    path: '/system/users',
  },
]

const demoEntries: DemoEntry[] = [
  {
    title: '筛选列表页',
    description: 'FilterTable + useTable + FormDialog + 行操作权限，覆盖 80% 的列表页',
    path: '/demo/table',
  },
  {
    title: '主从表单与详情',
    description: 'MasterDetailForm 行内编辑明细 + DescList 脱敏详情 + ScanInput + FileUploader',
    path: '/demo/form',
  },
  {
    title: '业务通用组件',
    description: 'StatusTimeline / ApprovalActionBar / TreeManager / StatusCardGrid / ConfigForm 等',
    path: '/demo/business',
  },
  {
    title: '图表组件',
    description: 'ChartWrapper 系列 + QueryChartPanel + GanttSchedule + TraceLinkGraph',
    path: '/demo/charts',
  },
  {
    title: '平板触摸组件',
    description: 'Touch 系列组件（独立 /tablet 路由分组，新窗口打开体验）',
    path: '/tablet/workbench',
  },
]

function openEntry(entry: DemoEntry) {
  if (entry.path.startsWith('/tablet')) {
    window.open(entry.path, '_blank')
    return
  }
  router.push(entry.path)
}
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="工作台"
      description="MES 通用组件库演示工程 —— 对应 wiki/23 前端组件设计规划"
    >
      <template #extra>
        <span class="dashboard__updated">
          最后更新：{{ lastUpdated ? formatDateTime(lastUpdated) : '-' }}
        </span>
        <el-button :loading="refreshing" size="small" @click="refreshNow">刷新</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="16">
      <el-col v-for="stat in stats" :key="stat.label" :xs="24" :sm="12" :md="6">
        <StatCard
          :label="stat.label"
          :value="stat.value"
          :unit="stat.unit"
          :icon="stat.icon"
          :icon-color="stat.iconColor"
          :trend="stat.trend"
        />
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>业务功能入口</template>
      <div class="dashboard__entries">
        <div
          v-for="entry in businessEntries"
          :key="entry.path"
          class="dashboard__entry"
          @click="openEntry(entry)"
        >
          <div class="dashboard__entry-title">{{ entry.title }}</div>
          <div class="dashboard__entry-desc">{{ entry.description }}</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>组件示例导航</template>
      <div class="dashboard__entries">
        <div
          v-for="entry in demoEntries"
          :key="entry.path"
          class="dashboard__entry"
          @click="openEntry(entry)"
        >
          <div class="dashboard__entry-title">{{ entry.title }}</div>
          <div class="dashboard__entry-desc">{{ entry.description }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard__updated {
  margin-right: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.dashboard__entries {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.dashboard__entry {
  padding: 16px;
  cursor: pointer;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.dashboard__entry:hover {
  border-color: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}

.dashboard__entry-title {
  font-size: 15px;
  font-weight: 600;
}

.dashboard__entry-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
</style>
