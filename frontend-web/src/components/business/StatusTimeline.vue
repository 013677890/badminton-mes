<script setup lang="ts">
import type { StatusMap, StatusNode } from '@/types/components'

defineOptions({ name: 'StatusTimeline' })

const props = withDefaults(
  defineProps<{
    nodes: StatusNode[]
    statusMap?: StatusMap
    /** 倒序展示（最新在上），交给 el-timeline 处理避免复制数组 */
    reverse?: boolean
  }>(),
  { reverse: false },
)

/** 未配置映射时降级为 info 灰，便于发现遗漏的状态码 */
function nodeColor(status: string): string {
  const type = props.statusMap?.[status]?.type
  return type ? `var(--el-color-${type})` : 'var(--el-color-info)'
}

function nodeText(status: string): string {
  return props.statusMap?.[status]?.text ?? status
}
</script>

<template>
  <el-timeline :reverse="reverse" class="status-timeline">
    <el-timeline-item
      v-for="(node, index) in nodes"
      :key="`${node.status}-${node.time}-${index}`"
      :timestamp="node.time"
      :color="nodeColor(node.status)"
      placement="top"
    >
      <slot name="node" :node="node" :index="index">
        <div class="status-timeline__title">
          <span class="status-timeline__status">{{ nodeText(node.status) }}</span>
          <span v-if="node.operator" class="status-timeline__operator">
            操作人：{{ node.operator }}
          </span>
        </div>
        <div v-if="node.remark" class="status-timeline__remark">{{ node.remark }}</div>
      </slot>
    </el-timeline-item>
  </el-timeline>
</template>

<style scoped>
.status-timeline {
  padding-left: 4px;
}

.status-timeline__title {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.status-timeline__status {
  font-size: 14px;
  font-weight: 600;
}

.status-timeline__operator {
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.status-timeline__remark {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
</style>
