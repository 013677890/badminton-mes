import { getRegistrationRoles, registerAccount } from '../../services/auth'
import { RegistrationRole } from '../../types/api'

Page({
  data: { userNo: '', userName: '', password: '', confirmPassword: '', roles: [] as RegistrationRole[], roleIndex: -1, roleName: '', loadingRoles: true, loading: false, error: '', showPassword: false },
  onLoad() { void this.loadRoles() },
  async loadRoles() {
    try {
      const roles = await getRegistrationRoles()
      this.setData({ roles, loadingRoles: false })
    } catch (error) {
      this.setData({ loadingRoles: false, error: (error as Error).message || '职位加载失败' })
    }
  },
  input(event: WechatMiniprogram.Input) { this.setData({ [event.currentTarget.dataset.key]: event.detail.value, error: '' }) },
  chooseRole(event: WechatMiniprogram.PickerChange) {
    const roleIndex = Number(event.detail.value)
    this.setData({ roleIndex, roleName: this.data.roles[roleIndex]?.roleName || '', error: '' })
  },
  togglePassword() { this.setData({ showPassword: !this.data.showPassword }) },
  back() { wx.navigateBack() },
  async register() {
    if (this.data.loading) return
    const userNo = this.data.userNo.trim()
    const userName = this.data.userName.trim()
    if (!userNo || !userName || !this.data.password || !this.data.confirmPassword) {
      this.setData({ error: '请完整填写注册信息' }); return
    }
    if (this.data.password.length < 6 || this.data.password.length > 32) {
      this.setData({ error: '密码长度必须在 6 到 32 位之间' }); return
    }
    if (this.data.password !== this.data.confirmPassword) {
      this.setData({ error: '两次输入的密码不一致' }); return
    }
    const role = this.data.roles[this.data.roleIndex]
    if (!role) { this.setData({ error: '请选择职位' }); return }
    this.setData({ loading: true, error: '' })
    try {
      await registerAccount({ userNo, userName, password: this.data.password, roleId: role.id })
      wx.setStorageSync('mes_register_prefill', { userNo })
      wx.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 500)
    } catch (error) {
      this.setData({ error: (error as Error).message || '注册失败，请重试' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
