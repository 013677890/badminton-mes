import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface AndonType {
  id: number
  typeCode: string
  typeName: string
  exceptionCategory: string
  handlingMode: string
  responseMinutes: number | null
  responsibleRoleCode: string | null
  notificationChannels: string | null
  lightControlEnabled: boolean
  enabledStatus: number
  remark: string | null
  createTime: string
  updateTime: string
}

export interface AndonTypePageParams {
  keyword?: string
  exceptionCategory?: string
  handlingMode?: string
  enabledStatus?: number
}

export type AndonTypeSaveReq = Omit<AndonType, 'id' | 'createTime' | 'updateTime'>

const BASE_URL = '/andon/types'

export function getAndonTypePage(
  params: AndonTypePageParams & PageParam,
): Promise<PageResult<AndonType>> {
  // 分页查询安灯类型主档，分类、处理模式和启用状态由后端筛选。
  return get(`${BASE_URL}/page`, params)
}

export function getAndonType(id: number): Promise<AndonType> {
  // 读取安灯类型详情及通知、灯控和责任角色配置。
  return get(`${BASE_URL}/${id}`)
}

export function createAndonType(data: AndonTypeSaveReq): Promise<number> {
  // 创建类型后后端负责校验编码、超时配置和通知通道组合。
  return post(BASE_URL, data)
}

export function updateAndonType(id: number, data: AndonTypeSaveReq): Promise<void> {
  // 更新类型配置，已被事件引用的字段变更规则由后端统一判断。
  return put(`${BASE_URL}/${id}`, data)
}

export function deleteAndonType(id: number): Promise<void> {
  // 类型删除采用后端业务约束校验，前端不假设历史事件是否允许删除。
  return del(`${BASE_URL}/${id}`)
}

export async function loadAndonTypeOptions() {
  // 下拉选项只取启用类型，并将实体转换为组件需要的 label/value。
  const page = await getAndonTypePage({ pageNo: 1, pageSize: 100, enabledStatus: 1 })
  return page.list.map((item) => ({ label: `${item.typeCode} ${item.typeName}`, value: item.id }))
}
