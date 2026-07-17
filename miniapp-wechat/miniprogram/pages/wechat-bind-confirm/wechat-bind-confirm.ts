import { bindWechatByCode, getWechatBindingPreview } from '../../services/auth'
import { WechatBindingPreview } from '../../types/api'

Page({
  data: { ticket: '', preview: {} as WechatBindingPreview, state: 'loading', error: '', loading: false },
  onLoad(query: { scene?: string; ticket?: string }) { const ticket = decodeURIComponent(query.scene || query.ticket || ''); this.setData({ ticket }); void this.loadPreview() },
  async loadPreview() { if (!this.data.ticket) { this.setData({ state: 'error', error: '绑定码无效' }); return } try { const preview = await getWechatBindingPreview(this.data.ticket); this.setData({ preview, state: 'ready' }) } catch (error) { this.setData({ state: 'error', error: (error as Error).message || '绑定码已失效' }) } },
  async confirm() { if (this.data.loading || this.data.state !== 'ready') return; this.setData({ loading: true, error: '' }); try { const code = await new Promise<string>((resolve, reject) => wx.login({ success: result => resolve(result.code), fail: reject })); await bindWechatByCode(this.data.ticket, code); this.setData({ state: 'success' }); wx.showToast({ title: '微信绑定成功', icon: 'success' }) } catch (error) { this.setData({ error: (error as Error).message || '绑定失败，请重试' }) } finally { this.setData({ loading: false }) } },
  goHome() { if (wx.getStorageSync('mes_token')) wx.reLaunch({ url: '/pages/profile/profile' }); else wx.reLaunch({ url: '/pages/login/login' }) }
})
