import { getDashboard } from '../../services/report'
import { connectDashboardRealtime, DashboardRealtimeConnection, DashboardRealtimeUpdate, RealtimeState } from '../../services/realtime'
import { handleSessionExpired } from '../../services/session'
import { Overview, RealtimeDashboard, Task } from '../../types/api'
type ViewTask = Task & { progress: number }
Page({
  data: {
    overview: {} as Overview,
    tasks: [] as ViewTask[],
    state: 'loading',
    lastRefreshTime: '',
    dataStatus: '',
    connectionText: '连接中',
    error: ''
    , overallProgress: 0
  },
  timer: 0 as unknown as number,
  realtimeConnection: null as DashboardRealtimeConnection | null,
  onShow() {
    this.getTabBar?.()?.setData({ selected: 0 })
    this.stopPolling()
    void this.load()
    this.startRealtime()
  },
  onHide() {
    this.stopRealtime()
    this.stopPolling()
  },
  onUnload() {
    this.stopRealtime()
    this.stopPolling()
  },
  async onPullDownRefresh() {
    try {
      await this.load()
    } finally {
      wx.stopPullDownRefresh()
    }
  },
  async load() {
    if (this.data.state === 'loading' && this.data.tasks.length) return

    this.setData({ state: 'loading', error: '' })
    try {
      this.applyDashboard(await getDashboard())
    } catch (error) {
      const message = (error as Error).message || '看板加载失败'
      this.setData({ state: message.includes('登录已失效') ? 'sessionExpired' : 'error', error: message })
    }
  },
  applyDashboard(result: RealtimeDashboard) {
    this.applyRealtimeUpdate(result)
  },
  applyRealtimeUpdate(update: DashboardRealtimeUpdate) {
    const sourceTasks = update.tasks || this.data.tasks
    const tasks = sourceTasks.map(task => ({
      ...task,
      progress: task.planQuantity
        ? Math.min(100, Math.round(task.finishQuantity / task.planQuantity * 100))
        : 0
    }))
    const overview = { ...this.data.overview, ...update.overview }
    const hasData = tasks.length > 0 || Number(overview.planQuantity || 0) > 0
    this.setData({
      overview,
      tasks,
      lastRefreshTime: overview.lastRefreshTime,
      dataStatus: overview.dataStatus,
      state: hasData ? 'ready' : 'empty',
      overallProgress: Number(overview.planQuantity || 0) ? Math.min(100, Math.round(Number(overview.goodQuantity || 0) / Number(overview.planQuantity) * 100)) : 0,
      error: ''
    })
  },
  startRealtime() {
    this.stopRealtime()
    this.realtimeConnection = connectDashboardRealtime({
      onUpdate: update => this.applyRealtimeUpdate(update),
      onState: state => this.handleRealtimeState(state)
    })
  },
  stopRealtime() {
    if (this.realtimeConnection) this.realtimeConnection.disconnect()
    this.realtimeConnection = null
  },
  handleRealtimeState(state: RealtimeState) {
    const labels: Record<RealtimeState, string> = {
      connecting: '实时连接中',
      connected: '实时推送',
      disconnected: '轮询兜底',
      unavailable: '定时刷新',
      sessionExpired: '会话已失效'
    }
    this.setData({ connectionText: labels[state] })
    if (state === 'connected') this.stopPolling()
    else if (state === 'sessionExpired') this.setData({ state: 'sessionExpired', error: '登录已失效，请重新登录' })
    else this.startPolling()
  },
  startPolling() {
    if (this.timer) return
    this.timer = setInterval(() => { void this.load() }, 60000)
  },
  stopPolling() {
    if (this.timer) clearInterval(this.timer)
    this.timer = 0 as unknown as number
  },
  retry() {
    void this.load()
    this.startRealtime()
  },
  loginAgain() {
    this.stopRealtime()
    this.stopPolling()
    handleSessionExpired()
  }
})
