App({
  onLaunch() {
    if (wx.getStorageSync('mes_token')) wx.reLaunch({ url: '/pages/dashboard/dashboard' })
  }
})
