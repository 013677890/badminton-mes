import type { RouteRecordRaw } from 'vue-router'
import { BASE_DATA_VIEW_ROLES, ROLES } from '@/constants/production'
import { WAGE_RECORD_VIEW_ROLES, WAGE_VIEW_ROLES } from '@/constants/wage'
import {
  ERP_CRAFT_ROLES,
  ERP_TASK_ROLES,
  INTEGRATION_GROUP_ROLES,
  INTEGRATION_MANAGE_ROLES,
} from '@/constants/integration'

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
    path: '/craft',
    redirect: '/craft/processes',
    // 工艺查询后端不限角色（生产模块要引用），写操作页面内按 CRAFT_WRITE_ROLES 控制
    meta: { title: '工艺管理', icon: 'SetUp' },
    children: [
      {
        path: '/craft/processes',
        name: 'CraftProcessList',
        component: () => import('@/views/craft/CraftProcessList.vue'),
        meta: { title: '工序管理' },
      },
      {
        path: '/craft/routes',
        name: 'CraftRouteList',
        component: () => import('@/views/craft/CraftRouteList.vue'),
        meta: { title: '工艺路线' },
      },
    ],
  },
  {
    path: '/wage',
    redirect: '/wage/rules',
    meta: { title: '计件工资', icon: 'Money', roles: WAGE_VIEW_ROLES },
    children: [
      {
        path: '/wage/rules',
        name: 'PieceRateRuleList',
        component: () => import('@/views/wage/PieceRateRuleList.vue'),
        meta: { title: '计件规则', roles: WAGE_VIEW_ROLES },
      },
      {
        path: '/wage/work-records',
        name: 'WageWorkRecordList',
        component: () => import('@/views/wage/WageWorkRecordList.vue'),
        meta: { title: '计件作业记录', roles: WAGE_RECORD_VIEW_ROLES },
      },
      {
        path: '/wage/settlements',
        name: 'WageSettlementList',
        component: () => import('@/views/wage/WageSettlementList.vue'),
        meta: { title: '工资结算', roles: WAGE_VIEW_ROLES },
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
    path: '/system',
    redirect: '/system/users',
    meta: { title: '系统管理', icon: 'Setting', roles: [ROLES.ADMIN] },
    children: [
      {
        path: '/system/users',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', roles: [ROLES.ADMIN] },
      },
      {
        path: '/system/roles',
        name: 'RoleList',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理', roles: [ROLES.ADMIN] },
      },
    ],
  },
  {
    path: '/integration',
    redirect: '/integration/write-logs',
    // 组级角色取并集：CRAFT_ENGINEER 仅可见 ERP 工艺待确认
    meta: { title: '接口管理', icon: 'Connection', roles: INTEGRATION_GROUP_ROLES },
    children: [
      {
        path: '/integration/erp-tasks',
        name: 'ErpTaskSyncList',
        component: () => import('@/views/integration/ErpTaskSyncList.vue'),
        meta: { title: 'ERP 任务同步', roles: ERP_TASK_ROLES },
      },
      {
        path: '/integration/erp-crafts',
        name: 'ErpCraftPendingList',
        component: () => import('@/views/integration/ErpCraftPendingList.vue'),
        meta: { title: 'ERP 工艺待确认', roles: ERP_CRAFT_ROLES },
      },
      {
        path: '/integration/write-logs',
        name: 'WriteLogList',
        component: () => import('@/views/integration/WriteLogList.vue'),
        meta: { title: '外部写入日志', roles: INTEGRATION_MANAGE_ROLES },
      },
      {
        path: '/integration/device-exceptions',
        name: 'DeviceCountExceptionList',
        component: () => import('@/views/integration/DeviceCountExceptionList.vue'),
        meta: { title: '设备计数异常池', roles: INTEGRATION_MANAGE_ROLES },
      },
      {
        path: '/integration/completion-orders',
        name: 'CompletionOrderList',
        component: () => import('@/views/integration/CompletionOrderList.vue'),
        meta: { title: '完工单读取', roles: INTEGRATION_MANAGE_ROLES },
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
