<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'

defineOptions({ name: 'TabletLayout' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const now = ref(formatDateTime(new Date()))
let timer: number | undefined

onMounted(() => {
  // 车间终端需要持续显示本地当前时间，组件销毁时在 onBeforeUnmount 中释放定时器。
  timer = window.setInterval(() => {
    now.value = formatDateTime(new Date())
  }, 1000)
})

onBeforeUnmount(() => {
  // 避免离开平板页面后定时器仍更新已卸载组件的响应式状态。
  if (timer !== undefined) window.clearInterval(timer)
})
</script>

<template>
  <el-container class="tablet-layout tablet-mode">
    <el-header class="tablet-layout__header">
      <div class="tablet-layout__left">
        <el-button size="large" :icon="ArrowLeft" circle @click="router.back()" />
        <span class="tablet-layout__title">{{ route.meta.title ?? '车间终端' }}</span>
      </div>
      <div class="tablet-layout__right">
        <span class="tablet-layout__clock">{{ now }}</span>
        <span class="tablet-layout__user">
          <el-icon><User /></el-icon>
          {{ userStore.userName || '操作工' }}
        </span>
      </div>
    </el-header>
    <el-main class="tablet-layout__main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.tablet-layout {
  height: 100%;
}

.tablet-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  color: #fff;
  background: #001529;
}

.tablet-layout__left,
.tablet-layout__right {
  display: flex;
  gap: 16px;
  align-items: center;
}

.tablet-layout__title {
  font-size: 20px;
  font-weight: 600;
}

.tablet-layout__clock {
  font-variant-numeric: tabular-nums;
}

.tablet-layout__user {
  display: flex;
  gap: 4px;
  align-items: center;
}

.tablet-layout__main {
  padding: 16px;
  overflow: auto;
  background: var(--mes-page-bg);
}
</style>
