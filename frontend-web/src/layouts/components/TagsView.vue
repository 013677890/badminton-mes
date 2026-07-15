<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'

defineOptions({ name: 'TagsView' })

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

watch(
  () => route.fullPath,
  () => {
    if (!route.meta.title) return
    appStore.addTab({
      path: route.fullPath,
      title: route.meta.title,
      name: typeof route.name === 'string' ? route.name : undefined,
      affix: route.meta.affix,
    })
  },
  { immediate: true },
)

function handleClose(path: string) {
  const next = appStore.removeTab(path)
  // 关闭的是当前页时跳到相邻标签
  if (path === route.fullPath) {
    router.push(next?.path ?? '/')
  }
}
</script>

<template>
  <div class="tags-view">
    <el-scrollbar>
      <div class="tags-view__list">
        <div
          v-for="tab in appStore.visitedTabs"
          :key="tab.path"
          class="tags-view__item"
          :class="{ 'tags-view__item--active': tab.path === route.fullPath }"
          @click="router.push(tab.path)"
        >
          <span>{{ tab.title }}</span>
          <el-icon
            v-if="!tab.affix"
            class="tags-view__close"
            @click.stop="handleClose(tab.path)"
          >
            <Close />
          </el-icon>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<style scoped>
.tags-view {
  height: var(--mes-tags-height);
  padding: 0 12px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.tags-view__list {
  display: flex;
  gap: 6px;
  align-items: center;
  height: var(--mes-tags-height);
}

.tags-view__item {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid var(--el-border-color-light);
  border-radius: 2px;
  user-select: none;
}

.tags-view__item--active {
  color: #fff;
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.tags-view__close {
  font-size: 12px;
  border-radius: 50%;
}

.tags-view__close:hover {
  color: #fff;
  background: var(--el-color-danger);
}
</style>
