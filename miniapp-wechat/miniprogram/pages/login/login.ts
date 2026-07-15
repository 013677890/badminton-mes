import { ensureLogin } from '../../services/auth'
Page({ login() { wx.showLoading({ title: '登录中' }); ensureLogin().catch(error => wx.showToast({ title: error.message, icon: 'none' })).finally(() => wx.hideLoading()) } })
