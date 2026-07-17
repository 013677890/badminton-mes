import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 系统模块接口，对齐后端 RoleController（/api/system/roles）
 * 与 UserController（/api/system/users，整体仅 ADMIN 可用）。
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

// ---------- 用户管理（仅 ADMIN） ----------

export interface SystemUser {
  id: number
  userNo: string
  userName: string
  /** 已脱敏 */
  mobile: string | null
  workshopId: number | null
  lineId: number | null
  status: number
  /** 编辑回显用 */
  roleIds: number[]
  roleCodes: string[]
  /** 与 roleIds 顺序一致 */
  roleNames: string[]
  createTime: string
}

export interface UserPageParams {
  userNo?: string
  userName?: string
  workshopId?: number
  /** 筛选拥有该角色的用户 */
  roleId?: number
  status?: number
}

export interface UserSaveReq {
  userNo: string
  userName: string
  /** 初始密码，仅创建时生效（6-32 位） */
  password?: string
  mobile?: string
  workshopId?: number
  lineId?: number
  roleIds: number[]
}

export function getUserPage(
  params: UserPageParams & PageParam,
): Promise<PageResult<SystemUser>> {
  return get('/system/users/page', params)
}

export function getUser(id: number): Promise<SystemUser> {
  return get(`/system/users/${id}`)
}

export function createUser(data: UserSaveReq): Promise<number> {
  return post('/system/users', data)
}

/** 修改基础信息与角色；userNo/password 字段被后端忽略 */
export function updateUser(id: number, data: UserSaveReq): Promise<void> {
  return put(`/system/users/${id}`, data)
}

/** 逻辑删除，不能删当前登录账号 */
export function deleteUser(id: number): Promise<void> {
  return del(`/system/users/${id}`)
}

/** 停用即强制下线 */
export function updateUserStatus(id: number, status: number): Promise<void> {
  return put(`/system/users/${id}/status`, { status })
}

/** 重置后该用户强制下线 */
export function resetUserPassword(id: number, newPassword: string): Promise<void> {
  return put(`/system/users/${id}/password/reset`, { newPassword })
}
