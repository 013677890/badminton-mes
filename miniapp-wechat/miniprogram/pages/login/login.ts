import { ensureLogin } from '../../services/auth'
// 登录按钮复用启动时的登录流程，成功后由认证服务按绑定状态跳转。
Page({ login() { /* 防止重复点击期间隐藏登录请求的 loading 状态。 */ wx.showLoading({ title: '登录中' }); ensureLogin().catch(error => wx.showToast({ title: error.message, icon: 'none' })).finally(() => wx.hideLoading()) } })
