<script setup lang="ts" generic="Row extends Record<string, any>">
import type { DescItem } from '@/types/components'
import { maskText } from '@/utils/format'
import StatusTag from './StatusTag.vue'

defineOptions({ name: 'DescList' })

const props = withDefaults(
  defineProps<{
    items: DescItem<Row>[]
    data: Row
    column?: number
    border?: boolean
    title?: string
  }>(),
  { column: 2, border: true },
)

// 支持点号路径取值、状态映射、格式化和脱敏显示；组件本身不修改传入数据。
function rawValue(prop: string): unknown {
  return prop.split('.').reduce<any>((acc, key) => (acc == null ? acc : acc[key]), props.data)
}

function display(item: DescItem<Row>): string {
  // 优先使用业务格式化器，再处理空值占位和敏感字段脱敏。
  if (item.formatter) return item.formatter(props.data)
  const value = rawValue(item.prop)
  if (value === undefined || value === null || value === '') return '—'
  const text = String(value)
  return item.mask ? maskText(text) : text
}
</script>

<template>
  <el-descriptions :title="title" :column="column" :border="border">
    <template #extra>
      <slot name="extra" />
    </template>
    <el-descriptions-item
      v-for="item in items"
      :key="item.prop"
      :label="item.label"
      :span="item.span ?? 1"
    >
      <slot :name="`desc-${item.prop}`" :data="data" :value="rawValue(item.prop)">
        <StatusTag
          v-if="item.statusMap"
          :status="rawValue(item.prop) as string | number"
          :status-map="item.statusMap"
        />
        <template v-else>{{ display(item) }}</template>
      </slot>
    </el-descriptions-item>
  </el-descriptions>
</template>
