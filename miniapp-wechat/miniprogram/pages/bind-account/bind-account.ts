Page({
  accountLogin() { wx.redirectTo({ url: '/pages/account-login/account-login' }) },
  back() { wx.navigateBack({ fail: () => wx.reLaunch({ url: '/pages/login/login' }) }) }
})
