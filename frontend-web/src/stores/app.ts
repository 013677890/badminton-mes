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
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function addTab(tab: VisitedTab) {
    if (visitedTabs.value.some((item) => item.path === tab.path)) return
    visitedTabs.value.push(tab)
  }

  /** 关闭标签，返回关闭后建议激活的相邻标签 */
  function removeTab(path: string): VisitedTab | undefined {
    const index = visitedTabs.value.findIndex((item) => item.path === path)
    if (index === -1) return undefined
    visitedTabs.value.splice(index, 1)
    return visitedTabs.value[index - 1] ?? visitedTabs.value[index]
  }

  function clearTabs() {
    visitedTabs.value = visitedTabs.value.filter((tab) => tab.affix)
  }

  return { sidebarCollapsed, visitedTabs, cachedViews, toggleSidebar, addTab, removeTab, clearTabs }
})
