import { logout } from '../../services/auth'
// 个人页展示登录时缓存的用户快照；退出动作直接复用认证服务的本地和服务端清理流程。
Page({ data: { user: {} }, onShow() { /* 每次回到前台重新读取，兼容登录后档案变化。 */ this.setData({ user: wx.getStorageSync('mes_user') || {} }) }, logout })
