import type { RouteRecordRaw } from 'vue-router'
import { BASE_DATA_VIEW_ROLES } from '@/constants/production'

/**
 * 主框架菜单路由（树形，供侧边菜单/面包屑使用）。
 * 注册到 router 时经 flattenMenuRoutes 拍平，保证单层 <router-view> 渲染。
 * meta.roles 与后端 Controller 的 @RequiresRoles 对齐（查询级）。
 */
export const menuRoutes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    name: 'DashboardView',
    component: () => import('@/views/dashboard/DashboardView.vue'),
    meta: { title: '工作台', icon: 'Odometer', affix: true },
  },
  {
    path: '/production',
    redirect: '/production/work-orders',
    meta: { title: '生产管理', icon: 'Operation' },
    children: [
      {
        path: '/production/work-orders',
        name: 'WorkOrderList',
        component: () => import('@/views/production/WorkOrderList.vue'),
        meta: { title: '生产工单' },
      },
      {
        path: '/production/work-orders/:id',
        name: 'WorkOrderDetail',
        component: () => import('@/views/production/WorkOrderDetail.vue'),
        meta: { title: '工单详情', hidden: true, activeMenu: '/production/work-orders' },
      },
      {
        path: '/production/dispatch-orders',
        name: 'DispatchList',
        component: () => import('@/views/production/DispatchList.vue'),
        meta: { title: '派工管理' },
      },
      {
        path: '/production/shortage-board',
        name: 'ShortageBoard',
        component: () => import('@/views/production/ShortageBoard.vue'),
        meta: { title: '欠料看板' },
      },
    ],
  },
  {
    path: '/basedata',
    redirect: '/basedata/products',
    meta: { title: '基础资料', icon: 'Files', roles: BASE_DATA_VIEW_ROLES },
    children: [
      {
        path: '/basedata/products',
        name: 'ProductList',
        component: () => import('@/views/basedata/ProductList.vue'),
        meta: { title: '产品主档', roles: BASE_DATA_VIEW_ROLES },
      },
      {
        path: '/basedata/materials',
        name: 'MaterialList',
        component: () => import('@/views/basedata/MaterialList.vue'),
        meta: { title: '物料主档', roles: BASE_DATA_VIEW_ROLES },
      },
      {
        path: '/basedata/boms',
        name: 'BomList',
        component: () => import('@/views/basedata/BomList.vue'),
        meta: { title: 'BOM 管理', roles: BASE_DATA_VIEW_ROLES },
      },
      {
        path: '/basedata/workshops',
        name: 'WorkshopList',
        component: () => import('@/views/basedata/WorkshopList.vue'),
        meta: { title: '车间管理', roles: BASE_DATA_VIEW_ROLES },
      },
      {
        path: '/basedata/lines',
        name: 'LineList',
        component: () => import('@/views/basedata/LineList.vue'),
        meta: { title: '产线管理', roles: BASE_DATA_VIEW_ROLES },
      },
    ],
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
