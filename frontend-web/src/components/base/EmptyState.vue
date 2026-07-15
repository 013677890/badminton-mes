<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'

defineOptions({ name: 'EmptyState' })

const props = withDefaults(
  defineProps<{
    /** empty=暂无数据；error=数据暂不可用（加载失败等） */
    type?: 'empty' | 'error'
    description?: string
  }>(),
  { type: 'empty' },
)

const desc = computed(
  () => props.description ?? (props.type === 'error' ? '数据暂不可用，请稍后重试' : '暂无数据'),
)
</script>

<template>
  <el-empty :description="desc" :image-size="80">
    <template v-if="type === 'error'" #image>
      <el-icon :size="56" color="var(--el-color-warning)"><WarningFilled /></el-icon>
    </template>
    <!-- 默认插槽放操作按钮（如重试） -->
    <slot />
  </el-empty>
</template>
