let redirectingToLogin = false

export function clearSession(): void {
  wx.removeStorageSync('mes_token')
  wx.removeStorageSync('mes_user')
}

export function handleSessionExpired(message = '登录已失效，请重新登录'): void {
  clearSession()
  if (redirectingToLogin) return

  redirectingToLogin = true
  wx.showToast({ title: message, icon: 'none' })
  setTimeout(() => {
    wx.reLaunch({
      url: '/pages/login/login',
      complete: () => { redirectingToLogin = false }
    })
  }, 300)
}
