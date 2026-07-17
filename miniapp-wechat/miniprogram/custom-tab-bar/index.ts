const tabs = [
  { pagePath: '/pages/dashboard/dashboard', text: '看板', icon: 'dashboard' },
  { pagePath: '/pages/production-analysis/production-analysis', text: '分析', icon: 'analysis' },
  { pagePath: '/pages/product-trace/product-trace', text: '追溯', icon: 'trace' },
  { pagePath: '/pages/profile/profile', text: '我的', icon: 'profile' }
]

Component({
  data: { selected: 0, tabs },
  pageLifetimes: { show() { this.syncSelected() } },
  lifetimes: { attached() { this.syncSelected() } },
  methods: {
    syncSelected() {
      const pages = getCurrentPages()
      const route = pages[pages.length - 1]?.route || ''
      const selected = tabs.findIndex(item => item.pagePath.slice(1) === route)
      if (selected >= 0) this.setData({ selected })
    },
    switchTab(event: WechatMiniprogram.TouchEvent) {
      const index = Number(event.currentTarget.dataset.index)
      if (index === this.data.selected) return
      wx.switchTab({ url: tabs[index].pagePath })
    }
  }
})
