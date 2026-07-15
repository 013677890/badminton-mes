import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ADMIN_ROLE, useUserStore } from '@/stores/user'
import { flattenMenuRoutes, menuRoutes } from './routes'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'LoginView',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    children: flattenMenuRoutes(menuRoutes),
  },
  {
    // 平板端独立分组：触摸优化布局
    path: '/tablet',
    component: () => import('@/layouts/TabletLayout.vue'),
    redirect: '/tablet/workbench',
    children: [
      {
        path: 'workbench',
        name: 'TabletDemo',
        component: () => import('@/views/tablet/TabletDemo.vue'),
        meta: { title: '平板工作台' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && userStore.isLoggedIn) {
    return { path: '/' }
  }
  // meta.roles 页面级权限：与 usePermission 同口径（ADMIN 全通过，命中任一即可）
  const required = to.meta.roles
  if (required && required.length > 0) {
    const owned = userStore.roleCodes
    const allowed = owned.includes(ADMIN_ROLE) || required.some((role) => owned.includes(role))
    if (!allowed) {
      ElMessage.warning('当前账号没有访问该页面的权限')
      return { path: '/dashboard' }
    }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 羽毛球 MES` : '羽毛球 MES'
})

export default router
