import { ensureLogin } from '../../services/auth'
// 微信登录与账号密码登录共用入口页；loading 状态用于拦截重复点击。
Page({ data: { loading: false, error: '' }, accountLogin() { wx.navigateTo({ url: '/pages/account-login/account-login' }) }, async wechatLogin() { if (this.data.loading) return; this.setData({ loading: true, error: '' }); try { await ensureLogin() } catch (error) { this.setData({ error: (error as Error).message || '微信登录失败，请重试' }) } finally { this.setData({ loading: false }) } } })
