<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

defineOptions({ name: 'PageHeader' })

defineProps<{
  title: string
  description?: string
  /** 显示返回按钮（详情页用） */
  showBack?: boolean
}>()

const router = useRouter()
</script>

<template>
  <div class="page-header">
    <div class="page-header__main">
      <div class="page-header__title-row">
        <el-button v-if="showBack" link class="page-header__back" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span class="page-header__title">{{ title }}</span>
      </div>
      <div v-if="description" class="page-header__desc">{{ description }}</div>
    </div>
    <div class="page-header__extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--el-bg-color);
  border-radius: 4px;
}

.page-header__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-header__title {
  font-size: 18px;
  font-weight: 600;
}

.page-header__desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.page-header__extra {
  flex-shrink: 0;
}
</style>
