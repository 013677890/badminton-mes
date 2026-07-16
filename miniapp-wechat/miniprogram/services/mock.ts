import { AuthProfile, MiniAppLoginResponse, PageResult, ProductionLineOption, ProductionSummary, RealtimeDashboard, RegisterRequest, RegistrationRole, SystemRole, SystemUser, Trace, UserAssignmentRequest, WechatBindingCode, WechatBindingPreview, WechatBindingStatus, WorkshopOption } from '../types/api'
import { getMockScenario } from './config'

const wait = <T>(value: T): Promise<T> => new Promise(resolve => setTimeout(() => resolve(value), 420))
const MOCK_USERS_KEY = 'mes_mock_users'
const MOCK_BINDING_TICKET_KEY = 'mes_mock_binding_ticket'
const MOCK_QR_BASE64 = 'iVBORw0KGgoAAAANSUhEUgAAAFEAAABRCAYAAACqj0o2AAABFklEQVR42u3bMRaCMBBFUeDQs1NKF2HpTl0BVnosLAQCmQn39Yo8/2RIQroOAAAAAAAAAP6jL/El021etn72eX+k/w2DHO1nrPXvR+J9H1sTKYkFILF2OR/ZKI6+bsmhSBJJJJFEkEgiiSSCxCwzlrMXM2rNkMIlcc8ULMJKUp9ZYJREDi0IrJ1IjaWlxrK2LCOtqkviFSROt3mJvpczZNhAir4pNmQQGF2kMbFlib+6dIQpXrokfkuLKjDkc2KW9BkTW09ixvd7JDG7xJLjXc2xsy9deltuZm8Jl7pm6lfr9qQoQvcePcpoLCSSCBJJJJFEkEhid5mlsLXz4AgHJCVROSvnTylmP23qvDMAAAAAAACAs3gBTOhiZsSglsMAAAAASUVORK5CYII='

interface MockUser extends SystemUser { password: string; wechatBindingTime?: string; wechatLastLoginTime?: string }
const MOCK_ROLES: SystemRole[] = [
  { id: 1, roleCode: 'ADMIN', roleName: '管理员', status: 1 },
  { id: 2, roleCode: 'PMC', roleName: 'PMC计划员', status: 1 },
  { id: 3, roleCode: 'WORKSHOP_MANAGER', roleName: '车间主管', status: 1 },
  { id: 4, roleCode: 'TEAM_LEADER', roleName: '班组长', status: 1 },
  { id: 5, roleCode: 'OPERATOR', roleName: '操作工', status: 1 },
  { id: 6, roleCode: 'INSPECTOR', roleName: '质检员', status: 1 },
  { id: 7, roleCode: 'CRAFT_ENGINEER', roleName: '工艺工程师', status: 1 }
]
const MOCK_WORKSHOPS: WorkshopOption[] = [{ id: 1, workshopCode: 'WS-01', workshopName: '羽毛球一车间', status: 1 }, { id: 2, workshopCode: 'WS-02', workshopName: '羽毛球二车间', status: 1 }]
const MOCK_LINES: ProductionLineOption[] = [{ id: 101, lineCode: 'LINE-01', lineName: '1 号产线', workshopId: 1, workshopName: '羽毛球一车间', status: 1 }, { id: 102, lineCode: 'LINE-02', lineName: '2 号产线', workshopId: 1, workshopName: '羽毛球一车间', status: 1 }, { id: 201, lineCode: 'LINE-03', lineName: '3 号产线', workshopId: 2, workshopName: '羽毛球二车间', status: 1 }]

function readMockUsers(): MockUser[] {
  const stored = wx.getStorageSync(MOCK_USERS_KEY) as MockUser[] | undefined
  if (stored?.length) return stored
  const users: MockUser[] = [
    { id: 1, userNo: 'admin', userName: '系统管理员', password: 'admin123', status: 1, roleIds: [1], roleCodes: ['ADMIN'], roleNames: ['管理员'], workshopId: 1, wechatBound: true },
    { id: 1001, userNo: 'MES-1001', userName: '林工', password: '123456', status: 1, roleIds: [4], roleCodes: ['TEAM_LEADER'], roleNames: ['班组长'], workshopId: 1, lineId: 101, wechatBound: false }
  ]
  wx.setStorageSync(MOCK_USERS_KEY, users)
  return users
}
function saveMockUsers(users: MockUser[]): void { wx.setStorageSync(MOCK_USERS_KEY, users) }
function findMockUser(userNo?: string): MockUser {
  const target = readMockUsers().find(item => item.userNo === userNo) || readMockUsers()[0]
  if (!target) throw new Error('用户不存在')
  return target
}
function toProfile(user: MockUser): AuthProfile { return { userId: user.id, userNo: user.userNo, userName: user.userName, mobile: user.mobile, workshopId: user.workshopId, lineId: user.lineId, roleCodes: user.roleCodes, roleNames: user.roleNames, wechatBound: user.wechatBound, wechatBindingTime: user.wechatBindingTime, wechatLastLoginTime: user.wechatLastLoginTime } }
function guard(): void {
  if (getMockScenario() === 'error') throw new Error('模拟网络异常，请稍后重试')
  if (getMockScenario() === 'sessionExpired') throw new Error('登录已失效，请重新登录')
}

export async function mockLogin(): Promise<MiniAppLoginResponse> {
  const scenario = getMockScenario()
  if (scenario === 'error') throw new Error('模拟登录失败，请重试')
  if (scenario === 'unbound') return wait({ bindingRequired: true, bindTicket: 'mock-bind-ticket', roleCodes: [] })
  const user = findMockUser('MES-1001')
  return wait({ bindingRequired: false, token: 'mock-token', ...toProfile(user) })
}

export async function mockAccountLogin(userNo: string, password: string): Promise<MiniAppLoginResponse> {
  guard()
  const user = findMockUser(userNo)
  if (user.password !== password) throw new Error('工号或密码错误')
  return wait({ bindingRequired: false, token: 'mock-token', ...toProfile(user) })
}

export async function mockProfile(userNo = 'MES-1001'): Promise<AuthProfile> {
  guard()
  return wait(toProfile(findMockUser(userNo)))
}

export async function mockRegisterAccount(data: RegisterRequest): Promise<number> {
  const users = readMockUsers()
  if (users.some(item => item.userNo === data.userNo)) throw new Error('工号已存在')
  const role = MOCK_ROLES.find(item => item.id === data.roleId)
  if (!role) throw new Error('职位不存在')
  const id = Math.max(...users.map(item => item.id), 1001) + 1
  users.push({ id, userNo: data.userNo, userName: data.userName, password: data.password, status: 1, roleIds: [role.id], roleCodes: [role.roleCode], roleNames: [role.roleName], wechatBound: false })
  saveMockUsers(users)
  return wait(id)
}

export async function mockBindingCode(): Promise<WechatBindingCode> {
  const ticket = `mock-${Date.now()}`
  const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString()
  const current = (wx.getStorageSync('mes_user') || {}) as MiniAppLoginResponse
  wx.setStorageSync(MOCK_BINDING_TICKET_KEY, { ticket, expiresAt, status: 'PENDING', userNo: current.userNo })
  return wait({ ticket, expiresAt, codeImageBase64: MOCK_QR_BASE64, status: 'PENDING' })
}
export async function mockBindingPreview(ticket: string): Promise<WechatBindingPreview> { const state = wx.getStorageSync(MOCK_BINDING_TICKET_KEY) || {}; if (state.ticket !== ticket || !state.userNo || new Date(state.expiresAt).getTime() <= Date.now()) throw new Error('绑定码已失效'); const user = findMockUser(state.userNo); return wait({ userName: user.userName, maskedUserNo: maskUserNo(user.userNo), expiresAt: state.expiresAt }) }
export async function mockBindingStatus(ticket: string): Promise<WechatBindingStatus> { const state = wx.getStorageSync(MOCK_BINDING_TICKET_KEY) || {}; if (state.ticket !== ticket || !state.expiresAt || (state.status !== 'BOUND' && new Date(state.expiresAt).getTime() <= Date.now())) return wait({ status: 'EXPIRED' }); return wait({ status: state.status || 'PENDING' }) }
export async function mockBindByCode(ticket: string): Promise<void> { const state = wx.getStorageSync(MOCK_BINDING_TICKET_KEY) || {}; if (state.ticket !== ticket || !state.userNo || new Date(state.expiresAt).getTime() <= Date.now()) throw new Error('绑定码已失效'); const users = readMockUsers(); const user = users.find(item => item.userNo === state.userNo); if (!user) throw new Error('绑定账号不存在'); user.wechatBound = true; user.wechatBindingTime = new Date().toISOString(); saveMockUsers(users); wx.setStorageSync(MOCK_BINDING_TICKET_KEY, { ...state, status: 'BOUND' }); await wait(undefined) }

export async function mockSystemUserPage(query: { keyword?: string; userNo?: string; userName?: string; roleId?: number; workshopId?: number; wechatBound?: boolean; pageNo: number; pageSize: number }): Promise<PageResult<SystemUser>> { let users: SystemUser[] = readMockUsers(); if (query.keyword) { const keyword = query.keyword.toLowerCase(); users = users.filter(item => item.userNo.toLowerCase().includes(keyword) || item.userName.toLowerCase().includes(keyword)) } else { if (query.userNo) users = users.filter(item => item.userNo.includes(query.userNo || '')); if (query.userName) users = users.filter(item => item.userName.includes(query.userName || '')) } if (query.roleId) users = users.filter(item => item.roleIds.includes(query.roleId as number)); if (query.workshopId) users = users.filter(item => item.workshopId === query.workshopId); if (typeof query.wechatBound === 'boolean') users = users.filter(item => item.wechatBound === query.wechatBound); const start = (query.pageNo - 1) * query.pageSize; return wait({ list: users.slice(start, start + query.pageSize), total: users.length, pageNo: query.pageNo, pageSize: query.pageSize }) }

function maskUserNo(userNo: string): string { if (userNo.length <= 4) return `${userNo.substring(0, 1)}***`; return `${userNo.substring(0, 2)}***${userNo.substring(userNo.length - 2)}` }
export async function mockSystemUser(id: number): Promise<SystemUser> { const user = readMockUsers().find(item => item.id === id); if (!user) throw new Error('用户不存在'); return wait(user) }
export async function mockAssignableRoles(): Promise<SystemRole[]> { return wait(MOCK_ROLES.filter(item => item.roleCode !== 'ADMIN')) }
export async function mockWorkshops(): Promise<WorkshopOption[]> { return wait(MOCK_WORKSHOPS) }
export async function mockProductionLines(workshopId?: number): Promise<ProductionLineOption[]> { return wait(MOCK_LINES.filter(item => !workshopId || item.workshopId === workshopId)) }
export async function mockUpdateUserAssignment(id: number, data: UserAssignmentRequest): Promise<void> { const users = readMockUsers(); const user = users.find(item => item.id === id); if (!user) throw new Error('用户不存在'); const roles = MOCK_ROLES.filter(item => data.roleIds.includes(item.id) && item.roleCode !== 'ADMIN'); const admin = user.roleCodes.includes('ADMIN') ? MOCK_ROLES[0] : undefined; user.roleIds = [...(admin ? [admin.id] : []), ...roles.map(item => item.id)]; user.roleCodes = [...(admin ? [admin.roleCode] : []), ...roles.map(item => item.roleCode)]; user.roleNames = [...(admin ? [admin.roleName] : []), ...roles.map(item => item.roleName)]; user.workshopId = data.workshopId; user.lineId = data.lineId; saveMockUsers(users); await wait(undefined) }

export async function mockRegistrationRoles(): Promise<RegistrationRole[]> {
  return wait([
    { id: 4, roleCode: 'TEAM_LEADER', roleName: '班组长', remark: '班组派工与现场作业', status: 1 },
    { id: 5, roleCode: 'OPERATOR', roleName: '操作工', remark: '工位报工与作业执行', status: 1 },
    { id: 6, roleCode: 'INSPECTOR', roleName: '质检员', remark: '检验与质量记录', status: 1 },
    { id: 7, roleCode: 'CRAFT_ENGINEER', roleName: '工艺工程师', remark: '工序、工艺路线与 SOP 维护', status: 1 }
  ])
}

export async function mockDashboard(): Promise<RealtimeDashboard> {
  guard()
  if (getMockScenario() === 'empty') return wait({ overview: { activeTaskCount: 0, pausedTaskCount: 0, abnormalBatchCount: 0, planQuantity: 0, inputQuantity: 0, goodQuantity: 0, defectQuantity: 0, equipmentTotalCount: 0, runningEquipmentCount: 0, unavailableEquipmentCount: 0, openAndonCount: 0, criticalAndonCount: 0, lastRefreshTime: new Date().toLocaleTimeString(), dataStatus: '暂无生产任务', warnings: [] }, tasks: [] })
  return wait({ overview: { activeTaskCount: 3, pausedTaskCount: 1, abnormalBatchCount: 2, planQuantity: 1280, inputQuantity: 934, goodQuantity: 872, defectQuantity: 28, equipmentTotalCount: 12, runningEquipmentCount: 10, unavailableEquipmentCount: 2, openAndonCount: 3, criticalAndonCount: 1, lastRefreshTime: new Date().toLocaleTimeString(), dataStatus: '数据已同步', warnings: ['2 号拉丝机需要点检'] }, tasks: [
    { taskId: 1, taskNo: 'WO-260715-01', productName: '训练级羽毛球', batchNo: 'B260715A', planQuantity: 620, finishQuantity: 488, taskStatus: 1, abnormal: false, updateTime: '10:32' },
    { taskId: 2, taskNo: 'WO-260715-02', productName: '比赛级羽毛球', batchNo: 'B260715B', planQuantity: 420, finishQuantity: 264, taskStatus: 1, abnormal: true, updateTime: '10:28' },
    { taskId: 3, taskNo: 'WO-260715-03', productName: '耐打训练球', batchNo: 'B260715C', planQuantity: 240, finishQuantity: 120, taskStatus: 2, abnormal: true, updateTime: '09:55' }
  ] })
}

export async function mockSummary(period: string): Promise<ProductionSummary> {
  guard()
  if (getMockScenario() === 'empty') return wait({ planQuantity: 0, inputQuantity: 0, goodQuantity: 0, defectQuantity: 0, reworkQuantity: 0, finishQuantity: 0, completionRate: 0, defectRate: 0, warnings: [] })
  const factor = period === '近30天' ? 8 : period === '近7天' ? 2.2 : 1
  return wait({ planQuantity: Math.round(1280 * factor), inputQuantity: Math.round(934 * factor), goodQuantity: Math.round(872 * factor), defectQuantity: Math.round(28 * factor), reworkQuantity: Math.round(12 * factor), finishQuantity: Math.round(860 * factor), completionRate: 67.2, defectRate: 2.17, warnings: ['不良率较昨日上升 0.4%'] })
}

export async function mockTrace(batchCode: string): Promise<Trace> {
  guard()
  if (getMockScenario() === 'empty' || batchCode === 'NOT_FOUND') return wait({ dataCompleteness: '无匹配记录', warnings: [], processHistories: [], workReports: [], repairRecords: [] })
  return wait({ dataCompleteness: '完整', warnings: ['末道包装工序已完成抽检'], task: { taskNo: 'WO-260715-02', productName: '比赛级羽毛球', batchNo: batchCode }, processHistories: [{ processName: '羽毛球成型', operateTime: '2026-07-15 08:15' }, { processName: '称重分拣', operateTime: '2026-07-15 09:20' }, { processName: '包装入库', operateTime: '2026-07-15 10:05' }], workReports: [{ reportNo: 'RPT-10021', reportTime: '2026-07-15 10:05', netGoodQuantity: 256, netDefectQuantity: 8 }], repairRecords: [{ summary: '更换拉丝机张力轮', eventTime: '2026-07-15 09:02' }] })
}
