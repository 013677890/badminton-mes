import { bindWechatByCode, changePassword, createWechatBindingCode, getWechatBindingStatus, refreshProfile } from '../../services/auth'
import { getDataSourceMode } from '../../services/config'
import { MiniAppLoginResponse } from '../../types/api'

Page({
  data: { user: {} as MiniAppLoginResponse, roleText: '现场成员', oldPassword: '', newPassword: '', confirmPassword: '', showPassword: false, loading: false, error: '', bindingLoading: false, bindingTicket: '', bindingImage: '', bindingStatus: '', countdown: 0, mode: '' },
  timer: 0 as unknown as number,
  poller: 0 as unknown as number,
  onShow() { void this.loadProfile() },
  onHide() { this.stopTimers() },
  onUnload() { this.stopTimers() },
  async loadProfile() {
    try {
      const user = await refreshProfile()
      this.setData({ user, roleText: (user.roleNames || []).join('、') || (user.roleCodes || []).join('、') || '现场成员', mode: getDataSourceMode() })
    } catch { const user = (wx.getStorageSync('mes_user') || {}) as MiniAppLoginResponse; this.setData({ user, roleText: (user.roleNames || []).join('、') || (user.roleCodes || []).join('、') || '现场成员', mode: getDataSourceMode() }) }
  },
  back() { wx.navigateBack() },
  input(event: WechatMiniprogram.Input) { this.setData({ [event.currentTarget.dataset.key]: event.detail.value, error: '' }) },
  togglePassword() { this.setData({ showPassword: !this.data.showPassword }) },
  async generateBindingCode() {
    if (this.data.bindingLoading) return
    this.stopTimers(); this.setData({ bindingLoading: true, bindingStatus: '', bindingImage: '' })
    try {
      const result = await createWechatBindingCode()
      const path = await this.writeBindingImage(result.ticket, result.codeImageBase64)
      const countdown = Math.max(0, Math.ceil((new Date(result.expiresAt).getTime() - Date.now()) / 1000))
      this.setData({ bindingTicket: result.ticket, bindingImage: path, bindingStatus: result.status, countdown })
      this.startCountdown(); this.startPolling()
    } catch (error) { wx.showToast({ title: (error as Error).message || '绑定码生成失败', icon: 'none' }) }
    finally { this.setData({ bindingLoading: false }) }
  },
  writeBindingImage(ticket: string, base64: string): Promise<string> {
    const path = `${wx.env.USER_DATA_PATH}/wechat-bind-${ticket}.png`
    const content = base64.replace(/^data:image\/png;base64,/, '')
    return new Promise((resolve, reject) => wx.getFileSystemManager().writeFile({ filePath: path, data: content, encoding: 'base64', success: () => resolve(path), fail: reject }))
  },
  startCountdown() { this.timer = setInterval(() => { const countdown = this.data.countdown - 1; if (countdown <= 0) { this.stopTimers(); this.setData({ countdown: 0, bindingStatus: 'EXPIRED' }); return } this.setData({ countdown }) }, 1000) as unknown as number },
  startPolling() { this.poller = setInterval(async () => { if (!this.data.bindingTicket) return; try { const result = await getWechatBindingStatus(this.data.bindingTicket); if (result.status === 'BOUND') { this.stopTimers(); this.setData({ bindingStatus: 'BOUND' }); wx.showToast({ title: '微信绑定成功', icon: 'success' }); await this.loadProfile() } else if (result.status === 'EXPIRED') { this.stopTimers(); this.setData({ bindingStatus: 'EXPIRED', countdown: 0 }) } } catch { /* 短时轮询失败不打断二维码展示 */ } }, 2000) as unknown as number },
  stopTimers() { if (this.timer) clearInterval(this.timer); if (this.poller) clearInterval(this.poller); this.timer = 0 as unknown as number; this.poller = 0 as unknown as number },
  previewCode() { if (this.data.bindingImage) wx.previewImage({ urls: [this.data.bindingImage] }) },
  async mockConfirmBinding() { if (!this.data.bindingTicket) return; await bindWechatByCode(this.data.bindingTicket, 'mock-code'); const result = await getWechatBindingStatus(this.data.bindingTicket); if (result.status === 'BOUND') { this.stopTimers(); this.setData({ bindingStatus: 'BOUND' }); await this.loadProfile(); wx.showToast({ title: '微信绑定成功', icon: 'success' }) } },
  async submit() {
    const { oldPassword, newPassword, confirmPassword } = this.data
    if (!oldPassword || !newPassword || !confirmPassword) return this.setData({ error: '请完整填写旧密码和新密码' })
    if (newPassword.length < 6 || newPassword.length > 32) return this.setData({ error: '新密码长度需为 6–32 位' })
    if (newPassword !== confirmPassword) return this.setData({ error: '两次输入的新密码不一致' })
    this.setData({ loading: true, error: '' })
    try { await changePassword(oldPassword, newPassword); wx.showToast({ title: '密码已修改', icon: 'success' }); setTimeout(() => wx.reLaunch({ url: '/pages/account-login/account-login' }), 800) }
    catch (error) { this.setData({ error: (error as Error).message || '密码修改失败' }) }
    finally { this.setData({ loading: false }) }
  }
})
