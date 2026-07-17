import { PageResult, ProductionLineOption, SystemRole, SystemUser, UserAssignmentRequest, WorkshopOption } from '../types/api'
import { isMockMode } from './config'
import { request } from './http'
import { mockAssignableRoles, mockProductionLines, mockSystemUser, mockSystemUserPage, mockUpdateUserAssignment, mockWorkshops } from './mock'

export interface UserPageQuery { keyword?: string; userNo?: string; userName?: string; roleId?: number; workshopId?: number; wechatBound?: boolean; pageNo: number; pageSize: number }

export function getSystemUserPage(query: UserPageQuery): Promise<PageResult<SystemUser>> {
  return isMockMode() ? mockSystemUserPage(query) : request<PageResult<SystemUser>>({ url: '/api/system/users/page', method: 'GET', data: query })
}
export function getSystemUser(id: number): Promise<SystemUser> { return isMockMode() ? mockSystemUser(id) : request<SystemUser>({ url: `/api/system/users/${id}`, method: 'GET' }) }
export function getAssignableRoles(): Promise<SystemRole[]> { return isMockMode() ? mockAssignableRoles() : request<SystemRole[]>({ url: '/api/system/roles', method: 'GET' }).then(items => items.filter(item => item.roleCode !== 'ADMIN')) }
export async function getWorkshopOptions(): Promise<WorkshopOption[]> { if (isMockMode()) return mockWorkshops(); const result = await request<PageResult<WorkshopOption>>({ url: '/api/production/workshops/page', method: 'GET', data: { pageNo: 1, pageSize: 100, status: 1 } }); return result.list }
export async function getProductionLineOptions(workshopId?: number): Promise<ProductionLineOption[]> { if (isMockMode()) return mockProductionLines(workshopId); const result = await request<PageResult<ProductionLineOption>>({ url: '/api/production/production_lines/page', method: 'GET', data: { pageNo: 1, pageSize: 100, status: 1, workshopId } }); return result.list }
export function updateUserAssignment(id: number, data: UserAssignmentRequest): Promise<void> { return isMockMode() ? mockUpdateUserAssignment(id, data) : request<void>({ url: `/api/system/users/${id}/assignment`, method: 'PUT', data }) }
