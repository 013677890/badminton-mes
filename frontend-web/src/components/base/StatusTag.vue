<script setup lang="ts">
import { computed } from 'vue'
import type { StatusMap } from '@/types/components'

defineOptions({ name: 'StatusTag' })

const props = defineProps<{
  status: string | number | undefined | null
  statusMap?: StatusMap
  /** 兼容早期页面使用的 map 属性，优先使用 statusMap。 */
  map?: StatusMap
}>()

const meta = computed(() => {
  if (props.status === undefined || props.status === null) return undefined
  return (props.statusMap ?? props.map)?.[String(props.status)]
})

const hasValue = computed(
  () => props.status !== undefined && props.status !== null && props.status !== '',
)
</script>

<template>
  <el-tag v-if="meta" :type="meta.type" disable-transitions>{{ meta.text }}</el-tag>
  <!-- 未配置映射的状态码原样降级展示，便于发现遗漏 -->
  <el-tag v-else-if="hasValue" type="info" disable-transitions>{{ status }}</el-tag>
  <span v-else>-</span>
</template>
