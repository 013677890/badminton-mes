<script lang="ts">
export interface TraceTypeMeta {
  label: string
  color: string
}

/** 缺省节点类型配色：物料/工序/质检/成品。模块作用域，供 defineProps 默认值引用 */
const DEFAULT_TYPE_META: Record<string, TraceTypeMeta> = {
  material: { label: '物料批次', color: '#e6a23c' },
  process: { label: '生产工序', color: '#409eff' },
  quality: { label: '质检记录', color: '#67c23a' },
  product: { label: '成品批次', color: '#9254de' },
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import type { ECOption } from '@/utils/echarts'
import type { TraceNode } from '@/types/components'
import ChartWrapper from '@/components/base/charts/ChartWrapper.vue'

defineOptions({ name: 'TraceLinkGraph' })

const props = withDefaults(
  defineProps<{
    /** 追溯树根节点（正向=从原料到成品，反向=从成品回溯） */
    root?: TraceNode
    /** 仅影响文案与根节点方向说明，树始终从左向右展开 */
    direction?: 'forward' | 'backward'
    /** 节点类型 → 图例文案/颜色 */
    typeMeta?: Record<string, TraceTypeMeta>
    loading?: boolean
    height?: string
  }>(),
  {
    direction: 'forward',
    typeMeta: () => DEFAULT_TYPE_META,
    loading: false,
    height: '420px',
  },
)

const emit = defineEmits<{
  'node-click': [node: TraceNode]
}>()

interface TreeItem {
  name: string
  itemStyle: { color: string; borderColor: string }
  children?: TreeItem[]
  /** 挂原始节点，click 事件回传 */
  raw: TraceNode
}

function nodeColor(node: TraceNode): string {
  return (node.type && props.typeMeta[node.type]?.color) || '#409eff'
}

function toTreeItem(node: TraceNode): TreeItem {
  const color = nodeColor(node)
  return {
    name: node.name,
    itemStyle: { color, borderColor: color },
    children: node.children?.map(toTreeItem),
    raw: node,
  }
}

/** 图例：树中实际出现的类型 */
const legendItems = computed(() => {
  const used = new Set<string>()
  const walk = (node?: TraceNode) => {
    if (!node) return
    if (node.type) used.add(node.type)
    node.children?.forEach(walk)
  }
  walk(props.root)
  return Object.entries(props.typeMeta)
    .filter(([type]) => used.has(type))
    .map(([type, meta]) => ({ type, ...meta }))
})

const option = computed<ECOption | undefined>(() => {
  if (!props.root) return undefined
  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const raw = (params as { data?: TreeItem }).data?.raw
        if (!raw) return ''
        const typeLabel = raw.type ? props.typeMeta[raw.type]?.label : undefined
        const lines = [`<b>${raw.name}</b>`]
        if (typeLabel) lines.push(typeLabel)
        for (const [key, value] of Object.entries(raw.meta ?? {})) {
          lines.push(`${key}：${value}`)
        }
        return lines.join('<br/>')
      },
    },
    series: [
      {
        type: 'tree',
        data: [toTreeItem(props.root)],
        orient: 'LR',
        left: 24,
        right: 140,
        top: 16,
        bottom: 16,
        symbol: 'circle',
        symbolSize: 14,
        edgeShape: 'polyline',
        initialTreeDepth: -1,
        label: {
          position: 'top',
          distance: 6,
          fontSize: 12,
          color: 'inherit',
        },
        leaves: {
          label: { position: 'right', distance: 8, align: 'left' },
        },
        lineStyle: { color: '#c0c4cc', width: 1.5 },
        emphasis: { focus: 'descendant' },
        expandAndCollapse: true,
        animationDuration: 400,
      },
    ],
  }
})

function handleChartClick(params: unknown) {
  const raw = (params as { data?: TreeItem }).data?.raw
  if (raw) emit('node-click', raw)
}
</script>

<template>
  <div class="trace-link-graph">
    <div class="trace-link-graph__header">
      <el-tag size="small" :type="direction === 'forward' ? 'primary' : 'warning'">
        {{ direction === 'forward' ? '正向追溯：原料 → 成品' : '反向追溯：成品 → 原料' }}
      </el-tag>
      <div class="trace-link-graph__legend">
        <span v-for="item in legendItems" :key="item.type" class="trace-link-graph__legend-item">
          <span class="trace-link-graph__legend-dot" :style="{ backgroundColor: item.color }" />
          {{ item.label }}
        </span>
      </div>
    </div>
    <ChartWrapper
      :option="option"
      :loading="loading"
      :height="height"
      @chart-click="handleChartClick"
    />
  </div>
</template>

<style scoped>
.trace-link-graph__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.trace-link-graph__legend {
  display: flex;
  gap: 16px;
}

.trace-link-graph__legend-item {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.trace-link-graph__legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
</style>
