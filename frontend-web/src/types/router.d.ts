import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** 菜单/面包屑/标签页标题 */
    title?: string
    /** 菜单图标（全局注册的 Element 图标名） */
    icon?: string
    /** 可访问角色，缺省不限 */
    roles?: string[]
    /** 不在菜单中显示 */
    hidden?: boolean
    /** 固定标签页，不可关闭 */
    affix?: boolean
    /** 详情页高亮的菜单路径 */
    activeMenu?: string
  }
}
