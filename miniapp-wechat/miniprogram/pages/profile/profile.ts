import { logout, refreshProfile, unbindAccount } from '../../services/auth'
import { getDataSourceMode, getMockScenario, MockScenario, setMockScenario } from '../../services/config'
import { MiniAppLoginResponse } from '../../types/api'
// 个人页先展示本地用户快照，再尝试刷新服务端档案；网络失败时保留最近一次成功数据。
Page({
  data: { user: {} as MiniAppLoginResponse, roleText: '', isAdmin: false, workshopText: '未分配车间', lineText: '未分配产线', mode: '', scenario: '', dialog: '', processing: false },
  onShow() { this.getTabBar?.()?.setData({ selected: 3 }); void this.loadUser() },
  async loadUser() {
    let user = (wx.getStorageSync('mes_user') || {}) as MiniAppLoginResponse
    try { user = await refreshProfile() } catch { /* 保留最近一次成功同步的资料 */ }
    const roleCodes = user.roleCodes || []
    this.setData({ user, roleText: (user.roleNames || []).join('、') || roleCodes.join('、') || '现场成员', isAdmin: roleCodes.includes('ADMIN'), workshopText: user.workshopId ? `车间 #${user.workshopId}` : '未分配车间', lineText: user.lineId ? `产线 #${user.lineId}` : '未分配产线', mode: getDataSourceMode(), scenario: getMockScenario() })
  },
  goSecurity() { wx.navigateTo({ url: '/pages/account-security/account-security' }) },
  goUserManagement() { wx.navigateTo({ url: '/pages/user-management/user-management' }) },
  showAbout() { wx.showModal({ title: '关于羽智造 MES', content: '羽毛球智能制造执行系统\n版本 V2.4.0\n用于生产看板、数据分析与批次追溯。', showCancel: false, confirmColor: '#0f766e' }) },
  openLogout() { this.setData({ dialog: 'logout' }) },
  openUnbind() { if (this.data.user.wechatBound) this.setData({ dialog: 'unbind' }) },
  closeDialog() { if (!this.data.processing) this.setData({ dialog: '' }) },
  async confirmAction() { if (this.data.processing) return; this.setData({ processing: true }); try { if (this.data.dialog === 'unbind') { await unbindAccount(); wx.reLaunch({ url: '/pages/login/login' }) } else { await logout() } } catch (error) { wx.showToast({ title: (error as Error).message || '操作失败', icon: 'none' }) } finally { this.setData({ processing: false, dialog: '' }) } },
  chooseScenario(event: WechatMiniprogram.TouchEvent) { const scenario = event.currentTarget.dataset.scenario as MockScenario; setMockScenario(scenario); this.setData({ scenario }); wx.showToast({ title: '模拟场景已切换', icon: 'none' }) }
})
