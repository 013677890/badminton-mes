import { post, put } from '@/utils/request'

export interface CompletionOrderSaveReq {
  productionTaskId: number
  workOrderId?: number
  batchNo: string
  completionQuantity: number
  goodQuantity: number
  defectQuantity: number
}

export function approveSceneWorkReport(id: number, employeeId: number): Promise<boolean> {
  return put(`/scene/work_reports/${id}/approve`, { employeeId })
}

export function createSceneCompletionOrder(data: CompletionOrderSaveReq): Promise<number> {
  return post('/scene/completion_orders', data)
}

export function approveSceneCompletionOrder(id: number, remark?: string): Promise<boolean> {
  return put(`/scene/completion_orders/${id}/approve`, { remark })
}

export function voidSceneCompletionOrder(id: number, remark?: string): Promise<boolean> {
  return put(`/scene/completion_orders/${id}/void`, { remark })
}
