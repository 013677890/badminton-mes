// 旧的“微信票据 + 账号密码”绑定入口已停用，本页仅引导用户进入更安全的绑定码流程。
Page({
  accountLogin() { wx.redirectTo({ url: '/pages/account-login/account-login' }) },
  back() { wx.navigateBack({ fail: () => wx.reLaunch({ url: '/pages/login/login' }) }) }
})
