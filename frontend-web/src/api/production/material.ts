import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 物料主档接口，对齐后端 MaterialController（/api/production/materials） */

export interface Material {
  id: number
  materialCode: string
  materialName: string
  spec: string | null
  /** 1 球头 2 羽毛 3 胶水 4 线材 5 包装 9 其他 */
  materialType: number
  unitId: number
  /** 关键物料参与齐套分析优先级 */
  keyMaterial: boolean
  status: number
  version: number
  createTime: string
  updateTime: string
}

export interface MaterialSaveParams {
  materialCode: string
  materialName: string
  spec?: string
  materialType: number
  unitId: number
  keyMaterial: boolean
  status: number
}

export interface MaterialPageParams {
  materialCode?: string
  materialName?: string
  materialType?: number
  unitId?: number
  keyMaterial?: boolean
  status?: number
}

export function getMaterialPage(
  params: MaterialPageParams & PageParam,
): Promise<PageResult<Material>> {
  // 分页筛选由后端动态条件执行，前端只传递当前页面的过滤条件。
  return get('/production/materials/page', params)
}

export function getMaterial(id: number): Promise<Material> {
  // 查询未删除物料主档详情。
  return get(`/production/materials/${id}`)
}

export function createMaterial(data: MaterialSaveParams): Promise<number> {
  // 创建物料时后端校验单位、类型、状态和编码唯一性。
  return post('/production/materials', data)
}

export function updateMaterial(
  id: number,
  data: MaterialSaveParams & { version: number },
): Promise<void> {
  // 携带版本更新物料；已被业务引用时单位变更会被后端拒绝。
  return put(`/production/materials/${id}`, data)
}

export function deleteMaterial(id: number, version: number): Promise<void> {
  // 后端只对无任何历史引用的物料执行逻辑删除。
  return del(`/production/materials/${id}`, { version })
}

export function updateMaterialStatus(id: number, status: number, version: number): Promise<void> {
  // 启停操作按版本 CAS 执行，停用前后端都会检查有效引用。
  return put(`/production/materials/${id}/status`, { status, version })
}
