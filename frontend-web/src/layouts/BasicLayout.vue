<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'
import { findMenuChain, menuRoutes } from '@/router/routes'
import SidebarMenuItem from './components/SidebarMenuItem.vue'
import TagsView from './components/TagsView.vue'

defineOptions({ name: 'BasicLayout' })

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const { hasRole } = usePermission()

/** 一级菜单按角色/hidden 过滤，子级过滤在 SidebarMenuItem 内递归处理 */
const menus = computed(() =>
  menuRoutes.filter((item) => !item.meta?.hidden && hasRole(item.meta?.roles)),
)

const activeMenu = computed(() => route.meta.activeMenu ?? route.path)

/** 面包屑：从菜单树反查祖先链（路由已拍平，route.matched 不含分组节点） */
const breadcrumbs = computed(() => {
  const chain = findMenuChain(menuRoutes, route.path)
  if (chain) return chain.map((item) => item.meta?.title).filter(Boolean)
  return route.meta.title ? [route.meta.title] : []
})

const asideWidth = computed(() =>
  appStore.sidebarCollapsed
    ? 'var(--mes-sidebar-collapsed-width)'
    : 'var(--mes-sidebar-width)',
)

function handleUserCommand(command: string | number | object) {
  if (command === 'logout') {
    userStore.logout()
    appStore.clearTabs()
    router.push('/login')
  }
}
</script>

<template>
  <el-container class="basic-layout">
    <el-aside :width="asideWidth" class="basic-layout__aside">
      <div class="basic-layout__logo">
        {{ appStore.sidebarCollapsed ? 'MES' : '羽毛球 MES' }}
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          unique-opened
          router
          class="basic-layout__menu"
        >
          <SidebarMenuItem v-for="item in menus" :key="item.path" :route="item" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="basic-layout__right">
      <el-header class="basic-layout__header">
        <div class="basic-layout__header-left">
          <el-icon class="basic-layout__collapse" :size="18" @click="appStore.toggleSidebar()">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(title, index) in breadcrumbs" :key="index">
              {{ title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <span class="basic-layout__user">
            <el-avatar :size="28" class="basic-layout__avatar">
              {{ (userStore.userName || '未').slice(0, 1) }}
            </el-avatar>
            <span>{{ userStore.userName || '未登录' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                角色：{{ userStore.roleCodes.join(' / ') || '-' }}
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <TagsView />

      <el-main class="basic-layout__main">
        <router-view v-slot="{ Component }">
          <!-- include 匹配组件名：约定页面 defineOptions({ name }) 与路由 name 一致 -->
          <keep-alive :include="appStore.cachedViews">
            <component :is="Component" :key="route.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.basic-layout {
  height: 100%;
}

.basic-layout__aside {
  display: flex;
  flex-direction: column;
  background: #001529;
  transition: width 0.2s;
}

.basic-layout__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--mes-header-height);
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
}

.basic-layout__menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgb(255 255 255 / 65%);
  --el-menu-hover-bg-color: rgb(255 255 255 / 10%);
  --el-menu-active-color: #fff;
}

.basic-layout__menu :deep(.el-menu) {
  background: transparent;
}

.basic-layout__menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary);
}

.basic-layout__right {
  min-width: 0;
}

.basic-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--mes-header-height);
  padding: 0 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.basic-layout__header-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.basic-layout__collapse {
  cursor: pointer;
}

.basic-layout__user {
  display: flex;
  gap: 6px;
  align-items: center;
  cursor: pointer;
}

.basic-layout__avatar {
  background: var(--el-color-primary);
}

.basic-layout__main {
  padding: 16px;
  overflow: auto;
  background: var(--mes-page-bg);
}
</style>
