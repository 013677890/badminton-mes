import { getAssignableRoles, getProductionLineOptions, getSystemUser, getWorkshopOptions, updateUserAssignment } from '../../services/admin'
import { ProductionLineOption, SystemRole, SystemUser, WorkshopOption } from '../../types/api'

interface RoleChoice extends SystemRole { checked: boolean }

Page({
  data: { id: 0, user: {} as SystemUser, userInitial: '用', roleChoices: [] as RoleChoice[], selectedRoleIds: [] as number[], workshops: [] as WorkshopOption[], lines: [] as ProductionLineOption[], workshopIndex: -1, lineIndex: -1, workshopText: '请选择所属车间', lineText: '请选择所属产线', isAdminTarget: false, state: 'loading', error: '', saving: false },
  onLoad(query: { id?: string }) { const id = Number(query.id); if (!id) { this.setData({ state: 'error', error: '用户参数无效' }); return } this.setData({ id }); void this.load() },
  async load() {
    this.setData({ state: 'loading', error: '' })
    try {
      const [user, roles, workshops] = await Promise.all([getSystemUser(this.data.id), getAssignableRoles(), getWorkshopOptions()])
      const lines = await getProductionLineOptions(user.workshopId)
      const workshopIndex = workshops.findIndex(item => item.id === user.workshopId)
      const lineIndex = lines.findIndex(item => item.id === user.lineId)
      const selectedRoleIds = (user.roleIds || []).filter(id => roles.some(role => role.id === id))
      this.setData({ user, userInitial: user.userName ? user.userName.substring(0, 1) : '用', roleChoices: roles.map(role => ({ ...role, checked: selectedRoleIds.includes(role.id) })), selectedRoleIds, workshops, lines, workshopIndex, lineIndex, workshopText: workshopIndex >= 0 ? workshops[workshopIndex].workshopName : '请选择所属车间', lineText: lineIndex >= 0 ? lines[lineIndex].lineName : '请选择所属产线', isAdminTarget: (user.roleCodes || []).includes('ADMIN'), state: 'ready' })
    } catch (error) { this.setData({ state: 'error', error: (error as Error).message || '用户资料加载失败' }) }
  },
  chooseRoles(event: WechatMiniprogram.CheckboxGroupChange) { const selectedRoleIds = event.detail.value.map(value => Number(value)); this.setData({ selectedRoleIds, roleChoices: this.data.roleChoices.map(role => ({ ...role, checked: selectedRoleIds.includes(role.id) })) }) },
  async chooseWorkshop(event: WechatMiniprogram.PickerChange) {
    const workshopIndex = Number(event.detail.value)
    const workshop = this.data.workshops[workshopIndex]
    this.setData({ workshopIndex, workshopText: workshop?.workshopName || '请选择所属车间', lineIndex: -1, lineText: '请选择所属产线', lines: [] })
    if (!workshop) return
    try { this.setData({ lines: await getProductionLineOptions(workshop.id) }) } catch (error) { wx.showToast({ title: (error as Error).message || '产线加载失败', icon: 'none' }) }
  },
  chooseLine(event: WechatMiniprogram.PickerChange) { const lineIndex = Number(event.detail.value); this.setData({ lineIndex, lineText: this.data.lines[lineIndex]?.lineName || '请选择所属产线' }) },
  async save() {
    if (this.data.saving) return
    if (!this.data.isAdminTarget && !this.data.selectedRoleIds.length) { wx.showToast({ title: '请至少选择一个职位', icon: 'none' }); return }
    const workshop = this.data.workshops[this.data.workshopIndex]
    const line = this.data.lines[this.data.lineIndex]
    if (line && workshop && line.workshopId !== workshop.id) { wx.showToast({ title: '所选产线不属于当前车间', icon: 'none' }); return }
    this.setData({ saving: true })
    try { await updateUserAssignment(this.data.id, { roleIds: this.data.selectedRoleIds, workshopId: workshop?.id, lineId: line?.id }); wx.showToast({ title: '分配已保存', icon: 'success' }); setTimeout(() => wx.navigateBack(), 700) }
    catch (error) { wx.showToast({ title: (error as Error).message || '保存失败', icon: 'none' }) }
    finally { this.setData({ saving: false }) }
  },
  back() { wx.navigateBack() }
})
