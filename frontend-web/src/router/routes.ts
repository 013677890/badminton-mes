import type { RouteRecordRaw } from 'vue-router'

/**
 * 主框架菜单路由（树形，供侧边菜单/面包屑使用）。
 * 注册到 router 时经 flattenMenuRoutes 拍平，保证单层 <router-view> 渲染。
 */
export const menuRoutes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    name: 'DashboardView',
    component: () => import('@/views/dashboard/DashboardView.vue'),
    meta: { title: '工作台', icon: 'Odometer', affix: true },
  },
  {
    path: '/demo',
    redirect: '/demo/table',
    meta: { title: '组件示例', icon: 'Grid' },
    children: [
      {
        path: '/demo/table',
        name: 'TableDemo',
        component: () => import('@/views/demo/TableDemo.vue'),
        meta: { title: '筛选列表页' },
      },
      {
        path: '/demo/form',
        name: 'FormDemo',
        component: () => import('@/views/demo/FormDemo.vue'),
        meta: { title: '主从表单与详情' },
      },
      {
        path: '/demo/business',
        name: 'BusinessDemo',
        component: () => import('@/views/demo/BusinessDemo.vue'),
        meta: { title: '业务通用组件' },
      },
      {
        path: '/demo/charts',
        name: 'ChartsDemo',
        component: () => import('@/views/demo/ChartsDemo.vue'),
        meta: { title: '图表组件' },
      },
    ],
  },
]

/** 拍平菜单树：所有带 component/redirect 的节点注册为布局的直接子路由 */
export function flattenMenuRoutes(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  const result: RouteRecordRaw[] = []
  for (const route of routes) {
    const { children, ...rest } = route
    if (rest.component || rest.redirect) {
      result.push(rest as RouteRecordRaw)
    }
    if (children?.length) {
      result.push(...flattenMenuRoutes(children))
    }
  }
  return result
}

/** 在菜单树中查找 path 的祖先链（面包屑用） */
export function findMenuChain(
  routes: RouteRecordRaw[],
  path: string,
  chain: RouteRecordRaw[] = [],
): RouteRecordRaw[] | null {
  for (const route of routes) {
    const current = [...chain, route]
    if (route.path === path) return current
    if (route.children?.length) {
      const found = findMenuChain(route.children, path, current)
      if (found) return found
    }
  }
  return null
}
