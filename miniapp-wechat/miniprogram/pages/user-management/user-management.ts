import { getAssignableRoles, getSystemUserPage, getWorkshopOptions } from '../../services/admin'
import { SystemRole, SystemUser, WorkshopOption } from '../../types/api'

interface SystemUserView extends SystemUser { initial: string; roleText: string }

Page({
  data: { keyword: '', users: [] as SystemUserView[], roles: [] as SystemRole[], workshops: [] as WorkshopOption[], roleIndex: -1, workshopIndex: -1, bindFilter: '', pageNo: 1, pageSize: 10, total: 0, loading: false, state: 'loading', error: '' },
  onLoad() { void this.loadOptions(); void this.load(true) },
  onShow() { if (this.data.users.length) void this.load(true) },
  input(event: WechatMiniprogram.Input) { this.setData({ keyword: event.detail.value }) },
  async loadOptions() { try { const [roles, workshops] = await Promise.all([getAssignableRoles(), getWorkshopOptions()]); this.setData({ roles, workshops }) } catch { /* 列表仍可按关键字使用 */ } },
  chooseRole(event: WechatMiniprogram.PickerChange) { this.setData({ roleIndex: Number(event.detail.value) }, () => void this.load(true)) },
  chooseWorkshop(event: WechatMiniprogram.PickerChange) { this.setData({ workshopIndex: Number(event.detail.value) }, () => void this.load(true)) },
  chooseBinding(event: WechatMiniprogram.TouchEvent) { this.setData({ bindFilter: event.currentTarget.dataset.value || '' }, () => void this.load(true)) },
  clearFilters() { this.setData({ keyword: '', roleIndex: -1, workshopIndex: -1, bindFilter: '' }, () => void this.load(true)) },
  search() { void this.load(true) },
  async load(reset = false) {
    if (this.data.loading) return
    const pageNo = reset ? 1 : this.data.pageNo
    this.setData({ loading: true, state: reset ? 'loading' : this.data.state, error: '' })
    try {
      const keyword = this.data.keyword.trim()
      const result = await getSystemUserPage({ keyword: keyword || undefined, roleId: this.data.roles[this.data.roleIndex]?.id, workshopId: this.data.workshops[this.data.workshopIndex]?.id, wechatBound: this.data.bindFilter === 'bound' ? true : this.data.bindFilter === 'unbound' ? false : undefined, pageNo, pageSize: this.data.pageSize })
      const list = result.list.map(item => ({ ...item, initial: item.userName ? item.userName.substring(0, 1) : '用', roleText: (item.roleNames || []).join('、') || '未分配职位' }))
      const users = reset ? list : [...this.data.users, ...list]
      this.setData({ users, total: result.total, pageNo: result.pageNo + 1, state: users.length ? 'ready' : 'empty' })
    } catch (error) { this.setData({ state: this.data.users.length ? 'ready' : 'error', error: (error as Error).message || '用户列表加载失败' }) }
    finally { this.setData({ loading: false }) }
  },
  loadMore() { if (this.data.users.length < this.data.total) void this.load(false) },
  openUser(event: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: `/pages/user-assignment/user-assignment?id=${event.currentTarget.dataset.id}` }) },
  back() { wx.navigateBack() }
})
