import { getProductionSummary } from '../../services/report'
import { handleSessionExpired } from '../../services/session'
import { ProductionSummary } from '../../types/api'

const periods = ['今日', '近7天', '近30天']

Page({
  data: {
    period: '今日', periods, summary: {} as ProductionSummary, state: 'loading', error: '', timeRange: '', defectNormal: true,
    trendBars: [{ outer: 42, inner: 34 }, { outer: 55, inner: 47 }, { outer: 70, inner: 62 }, { outer: 60, inner: 52 }, { outer: 86, inner: 78 }, { outer: 48, inner: 40 }]
  },
  onShow() { this.getTabBar?.()?.setData({ selected: 1 }); void this.load() },
  selectPeriod(event: WechatMiniprogram.TouchEvent) {
    const period = event.currentTarget.dataset.period as string
    if (period !== this.data.period) this.setData({ period }, () => void this.load())
  },
  async load() {
    this.setData({ state: 'loading', error: '' })
    const now = new Date()
    const days = this.data.period === '近30天' ? 30 : this.data.period === '近7天' ? 7 : 1
    const start = new Date(now.getTime() - days * 86400000)
    const pad = (value: number) => String(value).padStart(2, '0')
    const format = (date: Date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    this.setData({ timeRange: this.data.period === '今日' ? `${format(now)} 00:00 至当前` : `${format(start)} 至 ${format(now)}` })
    try {
      const summary = await getProductionSummary(start.toISOString(), now.toISOString(), undefined, undefined, this.data.period)
      this.setData({ summary, defectNormal: summary.defectRate < 1.5, state: summary.planQuantity ? 'ready' : 'empty' })
    } catch (error) {
      const message = (error as Error).message || '分析加载失败'
      this.setData({ state: message.includes('登录已失效') ? 'sessionExpired' : 'error', error: message })
    }
  },
  retry() { void this.load() },
  loginAgain() { handleSessionExpired() }
})
