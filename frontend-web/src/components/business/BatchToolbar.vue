<script setup lang="ts">
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { BatchAction } from '@/types/components'
import { usePermission } from '@/composables/usePermission'

defineOptions({ name: 'BatchToolbar' })

const props = defineProps<{
  selection: Record<string, any>[]
  actions: BatchAction[]
}>()

const emit = defineEmits<{
  'batch-action': [key: string, selection: Record<string, any>[]]
  clear: []
}>()

const { hasRole } = usePermission()

const visibleActions = computed(() => props.actions.filter((action) => hasRole(action.roles)))
const empty = computed(() => props.selection.length === 0)

async function handleAction(action: BatchAction) {
  if (action.confirm) {
    // 批量操作影响面大，配置了 confirm 的必须二次确认
    try {
      await ElMessageBox.confirm(action.confirm, '操作确认', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
  }
  emit('batch-action', action.key, props.selection)
}
</script>

<template>
  <div class="batch-toolbar">
    <div class="batch-toolbar__info">
      <span>
        已选
        <span class="batch-toolbar__count">{{ selection.length }}</span>
        项
      </span>
      <el-button link type="primary" :disabled="empty" @click="emit('clear')">清空</el-button>
    </div>
    <div class="batch-toolbar__actions">
      <el-button
        v-for="action in visibleActions"
        :key="action.key"
        :type="action.type ?? 'primary'"
        :disabled="empty"
        @click="handleAction(action)"
      >
        {{ action.label }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.batch-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.batch-toolbar__info {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.batch-toolbar__count {
  font-weight: 600;
  color: var(--el-color-primary);
}

.batch-toolbar__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
