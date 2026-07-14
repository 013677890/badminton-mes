import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
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
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 羽毛球 MES` : '羽毛球 MES'
})

export default router
