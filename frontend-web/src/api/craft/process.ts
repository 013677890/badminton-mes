import { del, get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 工序主档接口，对齐后端 CraftProcessController（/api/craft/processes）
 * 及其 SOP / 不良原因子资源。写操作限 ADMIN / CRAFT_ENGINEER。
 */

export interface CraftProcess {
  id: number
  processCode: string
  processName: string
  /** 类型编码，如 PREPARATION / PROCESSING / INSPECTION */
  processType: string
  /** 标准工时，单位秒 */
  standardTimeSeconds: number
  keyProcess: boolean
  /** 关键工序才有意义：强制报工 / 强制记录人员 */
  reportRequired: boolean | null
  personnelRequired: boolean | null
  qualityRequired: boolean
  scanRequired: boolean
  pieceRateEnabled: boolean
  equipmentCategoryId: number | null
  qualityPlanId: number | null
  remark: string | null
  /** 1 启用 0 停用 */
  status: number
  version: number
  createTime: string
  updateTime: string
}

export interface CraftProcessPageParams {
  processCode?: string
  processName?: string
  processType?: string
  /** Spring 从 'true'/'false' 字符串绑定 Boolean */
  keyProcess?: boolean | string
  qualityRequired?: boolean | string
  scanRequired?: boolean | string
  pieceRateEnabled?: boolean | string
  equipmentCategoryId?: number
  status?: number
}

export interface CraftProcessSaveReq {
  processCode: string
  processName: string
  processType: string
  standardTimeSeconds: number
  keyProcess: boolean
  qualityRequired: boolean
  scanRequired: boolean
  pieceRateEnabled: boolean
  equipmentCategoryId?: number
  /** 需要质检时必填 */
  qualityPlanId?: number
  remark?: string
  changeReason?: string
}

export interface CraftProcessChangeLog {
  id: number
  processId: number
  /** 见 constants/craft.ts PROCESS_CHANGE_TYPE_TEXT */
  changeType: number
  beforeSnapshot: string | null
  afterSnapshot: string | null
  changeReason: string | null
  operatorId: number
  createTime: string
}

export function getProcessPage(
  params: CraftProcessPageParams & PageParam,
): Promise<PageResult<CraftProcess>> {
  return get('/craft/processes/page', params)
}

export function getProcess(id: number): Promise<CraftProcess> {
  return get(`/craft/processes/${id}`)
}

export function createProcess(data: CraftProcessSaveReq): Promise<number> {
  return post('/craft/processes', data)
}

export function updateProcess(
  id: number,
  data: CraftProcessSaveReq & { version: number },
): Promise<void> {
  return put(`/craft/processes/${id}`, data)
}

export function updateProcessStatus(
  id: number,
  data: { version: number; status: number; reason: string },
): Promise<void> {
  return put(`/craft/processes/${id}/status`, data)
}

export function deleteProcess(id: number, version: number): Promise<void> {
  return del(`/craft/processes/${id}`, { version })
}

export function getProcessChangeLogPage(
  id: number,
  params: PageParam,
): Promise<PageResult<CraftProcessChangeLog>> {
  return get(`/craft/processes/${id}/change_logs`, params)
}

// ---------- 工序 SOP 子资源 ----------

export interface CraftProcessSop {
  id: number
  processId: number
  sopCode: string
  sopName: string
  /** SOP 业务版本（区别于乐观锁 version） */
  sopVersion: string
  fileUrl: string
  status: number
  version: number
  rebindRequired: boolean | null
  createTime: string
  updateTime: string
}

export interface CraftProcessSopSaveReq {
  sopCode: string
  sopName: string
  sopVersion: string
  fileUrl: string
  status: number
  changeReason?: string
}

export function getProcessSops(processId: number): Promise<CraftProcessSop[]> {
  return get(`/craft/processes/${processId}/sops`)
}

export function createProcessSop(
  processId: number,
  data: CraftProcessSopSaveReq,
): Promise<number> {
  return post(`/craft/processes/${processId}/sops`, data)
}

export function updateProcessSop(
  processId: number,
  sopId: number,
  data: CraftProcessSopSaveReq & { version: number },
): Promise<void> {
  return put(`/craft/processes/${processId}/sops/${sopId}`, data)
}

export function deleteProcessSop(
  processId: number,
  sopId: number,
  version: number,
): Promise<void> {
  return del(`/craft/processes/${processId}/sops/${sopId}`, { version })
}

// ---------- 工序不良原因子资源 ----------

export interface CraftProcessDefectReason {
  id: number
  processId: number
  reasonCode: string
  reasonName: string
  status: number
  version: number
  createTime: string
  updateTime: string
}

export interface CraftProcessDefectReasonSaveReq {
  reasonCode: string
  reasonName: string
  status: number
  changeReason?: string
}

export function getProcessDefectReasons(
  processId: number,
): Promise<CraftProcessDefectReason[]> {
  return get(`/craft/processes/${processId}/defect_reasons`)
}

export function createProcessDefectReason(
  processId: number,
  data: CraftProcessDefectReasonSaveReq,
): Promise<number> {
  return post(`/craft/processes/${processId}/defect_reasons`, data)
}

export function updateProcessDefectReason(
  processId: number,
  reasonId: number,
  data: CraftProcessDefectReasonSaveReq & { version: number },
): Promise<void> {
  return put(`/craft/processes/${processId}/defect_reasons/${reasonId}`, data)
}

export function deleteProcessDefectReason(
  processId: number,
  reasonId: number,
  version: number,
): Promise<void> {
  return del(`/craft/processes/${processId}/defect_reasons/${reasonId}`, { version })
}
