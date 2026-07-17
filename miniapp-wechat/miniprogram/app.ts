App({
  // 小程序启动时检查本地会话；没有会话时由认证服务完成微信登录或引导绑定 MES 账号。
  onLaunch() {
    if (wx.getStorageSync('mes_token')) wx.reLaunch({ url: '/pages/dashboard/dashboard' })
  }
})
