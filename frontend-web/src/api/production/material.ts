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
  return get('/production/materials/page', params)
}

export function getMaterial(id: number): Promise<Material> {
  return get(`/production/materials/${id}`)
}

export function createMaterial(data: MaterialSaveParams): Promise<number> {
  return post('/production/materials', data)
}

export function updateMaterial(
  id: number,
  data: MaterialSaveParams & { version: number },
): Promise<void> {
  return put(`/production/materials/${id}`, data)
}

export function deleteMaterial(id: number, version: number): Promise<void> {
  return del(`/production/materials/${id}`, { version })
}

export function updateMaterialStatus(id: number, status: number, version: number): Promise<void> {
  return put(`/production/materials/${id}/status`, { status, version })
}
