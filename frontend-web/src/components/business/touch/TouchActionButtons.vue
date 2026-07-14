<script setup lang="ts">
import type { TouchActionItem } from '@/types/components'

defineOptions({ name: 'TouchActionButtons' })

withDefaults(
  defineProps<{
    actions: TouchActionItem[]
    /** 正在执行的动作 key，对应按钮转 loading，其余禁用 */
    loadingKey?: string
    /** 一行按钮数，缺省自适应 */
    columns?: number
  }>(),
  {},
)

const emit = defineEmits<{
  action: [key: string]
}>()
</script>

<template>
  <div
    class="touch-action-buttons"
    :style="{
      gridTemplateColumns: columns
        ? `repeat(${columns}, 1fr)`
        : 'repeat(auto-fit, minmax(140px, 1fr))',
    }"
  >
    <el-button
      v-for="action in actions"
      :key="action.key"
      :type="action.type ?? 'primary'"
      size="large"
      class="touch-action-buttons__btn"
      :disabled="action.disabled || (!!loadingKey && loadingKey !== action.key)"
      :loading="loadingKey === action.key"
      @click="emit('action', action.key)"
    >
      {{ action.label }}
    </el-button>
  </div>
</template>

<style scoped>
.touch-action-buttons {
  display: grid;
  gap: 12px;
}

/* 覆盖 el-button 同级 margin，交给 grid gap 控制 */
.touch-action-buttons__btn {
  height: 64px;
  margin: 0 !important;
  font-size: 19px;
  font-weight: 600;
  border-radius: 8px;
}
</style>
