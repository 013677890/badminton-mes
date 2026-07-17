import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface AndonReason {
  id: number
  reasonCode: string
  reasonName: string
  andonTypeId: number
  andonTypeCode: string
  andonTypeName: string
  reasonDescription: string | null
  enabledStatus: number
  createTime: string
  updateTime: string
}

export interface AndonReasonPageParams {
  keyword?: string
  andonTypeId?: number
  enabledStatus?: number
}

export interface AndonReasonSaveReq {
  reasonCode: string
  reasonName: string
  andonTypeId: number
  reasonDescription?: string
  enabledStatus?: number
}

const BASE_URL = '/andon/reasons'

export function getAndonReasonPage(
  params: AndonReasonPageParams & PageParam,
): Promise<PageResult<AndonReason>> {
  // 按关键字、安灯类型和启用状态分页查询原因主档。
  return get(`${BASE_URL}/page`, params)
}

export function getAndonReason(id: number): Promise<AndonReason> {
  // 查询原因详情，供安灯类型关联编辑时回显。
  return get(`${BASE_URL}/${id}`)
}

export function createAndonReason(data: AndonReasonSaveReq): Promise<number> {
  // 创建原因时后端校验编码唯一及所属安灯类型有效性。
  return post(BASE_URL, data)
}

export function updateAndonReason(id: number, data: AndonReasonSaveReq): Promise<void> {
  // 更新原因主档，引用约束和启停规则由后端处理。
  return put(`${BASE_URL}/${id}`, data)
}

export function deleteAndonReason(id: number): Promise<void> {
  // 删除由后端执行逻辑删除并检查原因是否被历史事件使用。
  return del(`${BASE_URL}/${id}`)
}

export async function loadAndonReasonOptions(andonTypeId?: number) {
  // 选项只读取启用原因，并可按安灯类型缩小范围供事件表单使用。
  const page = await getAndonReasonPage({ pageNo: 1, pageSize: 100, enabledStatus: 1, andonTypeId })
  return page.list.map((item) => ({ label: `${item.reasonCode} ${item.reasonName}`, value: item.id }))
}
