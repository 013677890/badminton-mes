import { getProductionSummary } from '../../services/report'
import { handleSessionExpired } from '../../services/session'
import { ProductionSummary } from '../../types/api'

const periods = ['今日', '近7天', '近30天']

const padDatePart = (value: number) => String(value).padStart(2, '0')

const formatLocalDateTime = (date: Date) =>
  `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())}` +
  `T${padDatePart(date.getHours())}:${padDatePart(date.getMinutes())}:${padDatePart(date.getSeconds())}`

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
    const days = this.data.period === periods[2] ? 30 : this.data.period === periods[1] ? 7 : 1
    const start = new Date(now)
    start.setDate(now.getDate() - (days - 1))
    start.setHours(0, 0, 0, 0)
    const pad = (value: number) => String(value).padStart(2, '0')
    const format = (date: Date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    this.setData({ timeRange: this.data.period === '今日' ? `${format(now)} 00:00 至当前` : `${format(start)} 至 ${format(now)}` })
    try {
      const summary = await getProductionSummary(formatLocalDateTime(start), formatLocalDateTime(now), undefined, undefined, this.data.period)
      const viewSummary: ProductionSummary = {
        ...summary,
        completionRate: Number((summary.completionRate * 100).toFixed(2)),
        defectRate: Number((summary.defectRate * 100).toFixed(2))
      }
      this.setData({ summary: viewSummary, defectNormal: viewSummary.defectRate < 1.5, state: viewSummary.planQuantity ? 'ready' : 'empty' })
    } catch (error) {
      const message = (error as Error).message || '分析加载失败'
      this.setData({ state: message.includes('登录已失效') ? 'sessionExpired' : 'error', error: message })
    }
  },
  retry() { void this.load() },
  loginAgain() { handleSessionExpired() }
})
