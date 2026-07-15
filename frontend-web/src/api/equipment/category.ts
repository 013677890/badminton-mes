import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/** 设备类别接口，对齐后端 EquipmentCategoryController（/api/equipment/categories）。 */

export interface EquipmentCategory {
  id: number
  categoryCode: string
  categoryName: string
  parentId: number | null
  sortOrder: number
  remark: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface EquipmentCategorySaveParams {
  categoryCode: string
  categoryName: string
  parentId?: number | null
  sortOrder?: number | null
  remark?: string | null
  status?: number | null
}

export interface EquipmentCategoryPageParams {
  keyword?: string
  parentId?: number
  status?: number
}

/** 分页查询设备类别。 */
export function getEquipmentCategoryPage(
  params: EquipmentCategoryPageParams & PageParam,
): Promise<PageResult<EquipmentCategory>> {
  return get('/equipment/categories/page', params)
}

/** 查询设备类别详情。 */
export function getEquipmentCategory(id: number): Promise<EquipmentCategory> {
  return get(`/equipment/categories/${id}`)
}

/** 创建设备类别。 */
export function createEquipmentCategory(data: EquipmentCategorySaveParams): Promise<number> {
  return post('/equipment/categories', data)
}

/** 修改设备类别。 */
export function updateEquipmentCategory(
  id: number,
  data: EquipmentCategorySaveParams,
): Promise<void> {
  return put(`/equipment/categories/${id}`, data)
}

/** 删除设备类别。 */
export function deleteEquipmentCategory(id: number): Promise<void> {
  return del(`/equipment/categories/${id}`)
}
