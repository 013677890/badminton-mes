<script setup lang="ts">
import type { StatusMap, TouchCardItem } from '@/types/components'
import EmptyState from '@/components/base/EmptyState.vue'
import StatusTag from '@/components/base/StatusTag.vue'

defineOptions({ name: 'TouchCardList' })

const props = withDefaults(
  defineProps<{
    items: TouchCardItem[]
    statusMap?: StatusMap
    loading?: boolean
    /** 当前选中卡片 key，高亮显示 */
    activeKey?: string | number
    /** 卡片最小宽度（px），控制一行几张 */
    minCardWidth?: number
  }>(),
  { loading: false, minCardWidth: 320 },
)

const emit = defineEmits<{
  'item-click': [item: TouchCardItem]
}>()

function statusColor(item: TouchCardItem): string {
  const type = item.status ? props.statusMap?.[item.status]?.type : undefined
  return type ? `var(--el-color-${type})` : 'var(--el-border-color)'
}

function handleClick(item: TouchCardItem) {
  if (item.disabled) return
  emit('item-click', item)
}
</script>

<template>
  <div
    v-loading="loading"
    class="touch-card-list"
    :style="{ gridTemplateColumns: `repeat(auto-fill, minmax(${minCardWidth}px, 1fr))` }"
  >
    <div
      v-for="item in items"
      :key="item.key"
      class="touch-card-list__card"
      :class="{
        'touch-card-list__card--active': item.key === activeKey,
        'touch-card-list__card--disabled': item.disabled,
      }"
      :style="{ borderLeftColor: statusColor(item) }"
      @click="handleClick(item)"
    >
      <div class="touch-card-list__head">
        <div>
          <div class="touch-card-list__title">{{ item.title }}</div>
          <div v-if="item.subtitle" class="touch-card-list__subtitle">{{ item.subtitle }}</div>
        </div>
        <StatusTag v-if="item.status" :status="item.status" :status-map="statusMap" />
      </div>
      <div v-if="item.fields?.length" class="touch-card-list__fields">
        <div v-for="field in item.fields" :key="field.label" class="touch-card-list__field">
          <span class="touch-card-list__field-label">{{ field.label }}</span>
          <span class="touch-card-list__field-value">{{ field.value }}</span>
        </div>
      </div>
      <slot name="card-extra" :item="item" />
    </div>
    <div v-if="!loading && items.length === 0" class="touch-card-list__empty">
      <EmptyState />
    </div>
  </div>
</template>

<style scoped>
.touch-card-list {
  display: grid;
  gap: 16px;
}

.touch-card-list__card {
  padding: 16px 20px;
  cursor: pointer;
  user-select: none;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-left: 6px solid var(--el-border-color);
  border-radius: 8px;
  transition: box-shadow 0.15s, border-color 0.15s, transform 0.1s;
}

.touch-card-list__card:active {
  transform: scale(0.99);
}

.touch-card-list__card--active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
}

.touch-card-list__card--disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.touch-card-list__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.touch-card-list__title {
  font-size: 19px;
  font-weight: 600;
  line-height: 1.3;
}

.touch-card-list__subtitle {
  margin-top: 4px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.touch-card-list__fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 16px;
  margin-top: 12px;
}

.touch-card-list__field {
  display: flex;
  gap: 8px;
  font-size: 15px;
}

.touch-card-list__field-label {
  color: var(--el-text-color-secondary);
}

.touch-card-list__field-value {
  font-weight: 500;
}

.touch-card-list__empty {
  grid-column: 1 / -1;
  padding: 24px 0;
}
</style>
