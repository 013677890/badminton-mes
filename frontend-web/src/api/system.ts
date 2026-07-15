import { get } from '@/utils/request'

/**
 * 系统模块接口，对齐后端 RoleController（/api/system/roles）。
 * 用户管理接口仅管理员可用，当前前端只消费"按角色查用户"做选人下拉。
 */

export interface Role {
  id: number
  roleCode: string
  roleName: string
  remark: string | null
  status: number
}

export interface RoleUser {
  userId: number
  userNo: string
  userName: string
  workshopId: number | null
  lineId: number | null
}

/** 启用角色列表（仅 ADMIN 可调用） */
export function getEnabledRoles(): Promise<Role[]> {
  return get('/system/roles')
}

/** 某角色下的启用用户（登录即可），用于车间主管/欠料责任人等选人下拉 */
export function getRoleUsers(roleId: number): Promise<RoleUser[]> {
  return get(`/system/roles/${roleId}/users`)
}
