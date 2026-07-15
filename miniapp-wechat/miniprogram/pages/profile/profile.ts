import { logout } from '../../services/auth'
Page({ data: { user: {} }, onShow() { this.setData({ user: wx.getStorageSync('mes_user') || {} }) }, logout })
