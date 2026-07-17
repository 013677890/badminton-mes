import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export interface VisitedTab {
  path: string
  title: string
  /** 路由 name，同时约定为页面组件 name，用于 keep-alive include */
  name?: string
  /** 固定标签不可关闭（如工作台） */
  affix?: boolean
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const visitedTabs = ref<VisitedTab[]>([])

  /** keep-alive include 列表（有 name 的已访问页面） */
  const cachedViews = computed(() =>
    visitedTabs.value.filter((tab) => tab.name).map((tab) => tab.name as string),
  )

  function toggleSidebar() {
    // 仅切换布局状态，不触发路由变化；布局组件根据响应式值调整菜单宽度。
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function addTab(tab: VisitedTab) {
    // 以路径去重，避免同一页面重复创建 keep-alive 缓存和标签页。
    if (visitedTabs.value.some((item) => item.path === tab.path)) return
    visitedTabs.value.push(tab)
  }

  /** 关闭标签，返回关闭后建议激活的相邻标签 */
  function removeTab(path: string): VisitedTab | undefined {
    // 记录原索引后删除，优先返回被关闭标签左侧标签作为新的激活目标。
    const index = visitedTabs.value.findIndex((item) => item.path === path)
    if (index === -1) return undefined
    visitedTabs.value.splice(index, 1)
    return visitedTabs.value[index - 1] ?? visitedTabs.value[index]
  }

  function clearTabs() {
    // 只保留 affix 固定标签，工作台等入口页不会因批量关闭操作消失。
    visitedTabs.value = visitedTabs.value.filter((tab) => tab.affix)
  }

  return { sidebarCollapsed, visitedTabs, cachedViews, toggleSidebar, addTab, removeTab, clearTabs }
})
