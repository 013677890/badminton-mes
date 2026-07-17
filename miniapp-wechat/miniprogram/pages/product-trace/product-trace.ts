import { getTrace } from '../../services/report'
import { handleSessionExpired } from '../../services/session'
import { Trace } from '../../types/api'

const HISTORY_KEY = 'mes_trace_history'
const emptyTrace = (): Trace => ({ dataCompleteness: '', warnings: [], processHistories: [], workReports: [], repairRecords: [] })
interface TraceHistory { batchCode: string; productName: string; time: string; date: string }

// 手工输入、历史记录和扫码最终复用同一查询入口，并保存最近三条成功追溯记录。
Page({
  data: { batchCode: '', trace: emptyTrace(), state: 'idle', error: '', loading: false, history: [] as TraceHistory[] },
  onShow() { this.getTabBar?.()?.setData({ selected: 2 }); this.setData({ history: (wx.getStorageSync(HISTORY_KEY) || []) as TraceHistory[] }) },
  input(event: WechatMiniprogram.Input) { this.setData({ batchCode: event.detail.value, state: 'idle', error: '' }) },
  clear() { this.setData({ batchCode: '', trace: emptyTrace(), state: 'idle', error: '' }) },
  clearHistory() { wx.removeStorageSync(HISTORY_KEY); this.setData({ history: [] }) },
  useHistory(event: WechatMiniprogram.TouchEvent) { this.setData({ batchCode: event.currentTarget.dataset.code }, () => void this.query()) },
  scan() {
    // 扫码成功后写入批次码并复用 query，保证扫码和手工查询的校验及错误处理一致。
    if (this.data.loading) return
    wx.scanCode({ scanType: ['barCode', 'qrCode'], success: result => this.setData({ batchCode: result.result }, () => void this.query()), fail: error => { if (!error.errMsg.includes('cancel')) this.setData({ error: '无法读取二维码，请手动输入批次码' }) } })
  },
  async query() {
    // 空值和重复提交在客户端拦截，批次有效性及追溯链路完整性由后端判定。
    const batchCode = this.data.batchCode.trim()
    if (!batchCode) { this.setData({ error: '请输入批次码后再查询' }); return }
    if (this.data.loading) return
    this.setData({ loading: true, state: 'loading', error: '' })
    try {
      const trace = await getTrace(batchCode)
      if (trace.task) this.saveHistory(batchCode, trace.task.productName)
      this.setData({ trace, state: trace.task ? 'ready' : 'empty' })
    } catch (error) {
      const message = (error as Error).message || '追溯查询失败'
      this.setData({ state: message.includes('登录已失效') ? 'sessionExpired' : 'error', error: message })
    } finally { this.setData({ loading: false }) }
  },
  saveHistory(batchCode: string, productName: string) {
    const now = new Date(); const pad = (value: number) => String(value).padStart(2, '0')
    const next = [{ batchCode, productName, time: `${pad(now.getHours())}:${pad(now.getMinutes())}`, date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}` }, ...this.data.history.filter(item => item.batchCode !== batchCode)].slice(0, 3)
    wx.setStorageSync(HISTORY_KEY, next); this.setData({ history: next })
  },
  retry() { void this.query() },
  loginAgain() { handleSessionExpired() }
})
