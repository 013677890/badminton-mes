<script setup lang="ts">
import { reactive, watchEffect } from 'vue'
import type { TabItem } from '@/types/components'

defineOptions({ name: 'TabDetailPage' })

const props = defineProps<{
  tabs: TabItem[]
}>()

const emit = defineEmits<{
  'tab-change': [name: string]
}>()

/** 未绑定 v-model:active 时缺省激活第一个 tab */
const active = defineModel<string>('active')

/** 已激活过的 tab：lazy tab 首次激活才渲染，之后保持挂载避免切换丢状态 */
const visited = reactive(new Set<string>())

watchEffect(() => {
  if (active.value === undefined && props.tabs.length > 0) {
    active.value = props.tabs[0].name
  }
  if (active.value !== undefined) visited.add(active.value)
})

function shouldRender(tab: TabItem): boolean {
  if (tab.lazy === false) return true
  return visited.has(tab.name)
}

/** el-tabs 的 TabPaneName 含 number，归一成 string 再写回模型 */
function handleChange(name: string | number) {
  const value = String(name)
  active.value = value
  emit('tab-change', value)
}
</script>

<template>
  <div class="tab-detail-page">
    <slot name="header" />
    <el-tabs :model-value="active" class="tab-detail-page__tabs" @tab-change="handleChange">
      <el-tab-pane v-for="tab in tabs" :key="tab.name" :name="tab.name" :label="tab.label">
        <template v-if="shouldRender(tab)">
          <slot :name="`tab-${tab.name}`" />
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.tab-detail-page__tabs {
  margin-top: 8px;
}
</style>
