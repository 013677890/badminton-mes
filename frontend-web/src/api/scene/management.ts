import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface SceneProductionTask {
  id: number; taskNo: string; workOrderId: number; workOrderNo: string; productId: number
  productCode: string; productName: string; batchNo: string; routingId: number; routingCode: string
  routingVersion: string; workshopId: number; workshopName: string; lineId: number; lineName: string
  shiftId: number | null; planDate: string; planQuantity: number; inputQuantity: number; goodQuantity: number
  defectQuantity: number; reworkQuantity: number; finishQuantity: number; planStartTime: string
  planEndTime: string; actualStartTime: string | null; actualEndTime: string | null; taskStatus: number; pauseReason: string | null
}
export interface SceneDispatchDetail {
  id: number; processId: number; processCode: string; processName: string; seq: number; keyProcess: boolean
  inspect: boolean; scanRequired: boolean; sopId: number | null; sopCode: string | null; sopName: string | null
  sopVersion: string | null; stationId: number | null; userId: number | null; equipmentId: number | null
  planQuantity: number; detailStatus: number; paused: boolean; pauseReason: string | null
  actualStartTime: string | null; actualEndTime: string | null
}
export interface SceneDispatchOrder { id: number; dispatchNo: string; taskId: number; routingId: number; routingCode: string; routingVersion: string; dispatchStatus: number; operations: SceneDispatchDetail[] }
export interface SceneProductStatus { id: number; batchNo: string; taskId: number; productId: number; currentProcessId: number | null; currentProcessName: string | null; batchStatus: number; abnormal: boolean; updateTime: string }
export interface SceneProductionParameter { id: number; paramCode: string; paramName: string; paramValue: string; valueType: number; workshopId: number | null; lineId: number | null; productId: number | null; remark: string | null; status: number; createTime: string; updateTime: string }
export interface SceneRepair { id: number; repairNo: string; sourceReportId: number; taskId: number; batchNo: string; defectQuantity: number; repairQuantity: number; status: string; reason: string; assigneeId: number | null; recheckResult: string | null; recheckQuantity: number | null; createdTime: string; updatedTime: string }

export function getSceneTaskPage(params: { taskNo?: string; workshopId?: number; lineId?: number; taskStatus?: number; planDate?: string } & PageParam): Promise<PageResult<SceneProductionTask>> { return get('/scene/production_tasks/page', params) }
export function getSceneTask(id: number): Promise<SceneProductionTask> { return get(`/scene/production_tasks/${id}`) }
export function getSceneTaskProgress(id: number): Promise<Record<string, any>> { return get(`/scene/production_tasks/${id}/progress`) }
export function saveSceneTask(id: number | undefined, data: { workOrderId: number; lineId: number; shiftId?: number; planDate: string; planQuantity: number; planStartTime: string; planEndTime: string }): Promise<number | void> { return id ? put(`/scene/production_tasks/${id}`, data) : post('/scene/production_tasks', data) }
export function actionSceneTask(id: number, action: 'audit' | 'release' | 'start' | 'resume'): Promise<void> { return put(`/scene/production_tasks/${id}/${action}`) }
export function reasonActionSceneTask(id: number, action: 'pause' | 'close', reason: string): Promise<void> { return put(`/scene/production_tasks/${id}/${action}`, { reason }) }

export function getSceneDispatchPage(params: { dispatchNo?: string; taskId?: number; dispatchStatus?: number } & PageParam): Promise<PageResult<SceneDispatchOrder>> { return get('/scene/dispatch_orders/page', params) }
export function getSceneDispatch(id: number): Promise<SceneDispatchOrder> { return get(`/scene/dispatch_orders/${id}`) }
export function generateSceneDispatch(taskId: number): Promise<number> { return post('/scene/dispatch_orders/generate', { taskId }) }
export function actionSceneDispatch(id: number, action: 'confirm' | 'cancel'): Promise<void> { return put(`/scene/dispatch_orders/${id}/${action}`) }
export function getSceneDispatchOperations(id: number): Promise<SceneDispatchDetail[]> { return get(`/scene/dispatch_orders/${id}/operations`) }

export function getSceneOperationPage(params: { taskId?: number; userId?: number; stationId?: number; equipmentId?: number; detailStatus?: number } & PageParam): Promise<PageResult<SceneDispatchDetail>> { return get('/scene/operation_jobs/page', params) }
export function getMySceneOperations(): Promise<SceneDispatchDetail[]> { return get('/scene/operation_jobs/my') }
export function getSceneOperation(id: number): Promise<SceneDispatchDetail> { return get(`/scene/operation_jobs/${id}`) }
export function scanSceneOperation(id: number, barcodeValue: string, equipmentId?: number): Promise<void> { return post(`/scene/operation_jobs/${id}/scan`, { barcodeValue, equipmentId }) }
export function actionSceneOperation(id: number, action: 'start' | 'finish'): Promise<void> { return put(`/scene/operation_jobs/${id}/${action}`) }
export function pauseSceneOperation(id: number, reason: string): Promise<void> { return put(`/scene/operation_jobs/${id}/pause`, { reason }) }

export function getSceneProductStatusPage(params: { batchNo?: string; taskId?: number; batchStatus?: number; abnormal?: boolean } & PageParam): Promise<PageResult<SceneProductStatus>> { return get('/scene/product_statuses/page', params) }
export function getSceneProductStatusByBatch(batchCode: string): Promise<SceneProductStatus> { return get(`/scene/product_statuses/by_batch/${encodeURIComponent(batchCode)}`) }
export function getSceneStatusHistories(id: number): Promise<Array<Record<string, any>>> { return get(`/scene/product_statuses/${id}/histories`) }
export function getSceneOperationHistories(id: number): Promise<Array<Record<string, any>>> { return get(`/scene/product_statuses/${id}/operation_histories`) }

export function getSceneParameterPage(params: { paramCode?: string; workshopId?: number; lineId?: number; productId?: number; status?: number } & PageParam): Promise<PageResult<SceneProductionParameter>> { return get('/scene/production_parameters/page', params) }
export function getSceneParameter(id: number): Promise<SceneProductionParameter> { return get(`/scene/production_parameters/${id}`) }
export function getEffectiveSceneParameter(params: { paramCode: string; workshopId?: number; lineId?: number; productId?: number }): Promise<SceneProductionParameter> { return get('/scene/production_parameters/effective', params) }
export function saveSceneParameter(id: number | undefined, data: { paramCode: string; paramName: string; paramValue: string; valueType: number; workshopId?: number; lineId?: number; productId?: number; remark?: string; changeReason: string }): Promise<number | void> { return id ? put(`/scene/production_parameters/${id}`, data) : post('/scene/production_parameters', data) }
export function changeSceneParameterStatus(id: number, enabled: boolean, reason: string): Promise<void> { return put(`/scene/production_parameters/${id}/${enabled ? 'enable' : 'disable'}`, { reason }) }
export function getSceneParameterLogs(id: number): Promise<Array<Record<string, any>>> { return get(`/scene/production_parameters/${id}/change_logs`) }

export interface SceneWorkReportReq { requestNo: string; dispatchDetailId: number; inputQuantity: number; goodQuantity: number; defectQuantity: number; reworkQuantity: number; barcodeValue?: string; reportTime: string }
export function submitSceneWorkReport(data: SceneWorkReportReq, device = false): Promise<number> { return post(`/scene/work_reports/${device ? 'device_count' : 'submit'}`, data) }
export function reverseSceneWorkReport(id: number, requestNo: string, reason: string): Promise<number> { return put(`/scene/work_reports/${id}/reverse`, { requestNo, reason }) }

export function createSceneCompletion(taskId: number, finishQuantity: number): Promise<number> { return post('/scene/completion_orders/create_from_task', { taskId, finishQuantity }) }
export function updateSceneCompletion(id: number, finishQuantity: number): Promise<void> { return put(`/scene/completion_orders/${id}`, { finishQuantity }) }
export function submitSceneCompletion(id: number): Promise<void> { return put(`/scene/completion_orders/${id}/submit`) }
export function auditSceneCompletion(id: number, approved: boolean, remark?: string): Promise<void> { return put(`/scene/completion_orders/${id}/audit`, { approved, remark }) }
export function syncSceneCompletion(id: number): Promise<void> { return post(`/scene/completion_orders/${id}/sync`) }

export function createSceneRepair(data: { sourceReportId: number; batchNo: string; defectQuantity: number; repairQuantity: number; reason: string }): Promise<number> { return post('/scene/repair_work_orders', data) }
export function getSceneRepair(id: number): Promise<SceneRepair> { return get(`/scene/repair_work_orders/${id}`) }
export function assignSceneRepair(id: number, assigneeId: number): Promise<void> { return put(`/scene/repair_work_orders/${id}/assign`, undefined, { params: { assigneeId } }) }
export function startSceneRepair(id: number): Promise<void> { return put(`/scene/repair_work_orders/${id}/start`) }
export function addSceneRepairRecord(id: number, quantity: number, description: string): Promise<void> { return post(`/scene/repair_work_orders/${id}/records`, { quantity, description }) }
export function recheckSceneRepair(id: number, result: string, quantity: number): Promise<void> { return post(`/scene/repair_work_orders/${id}/recheck`, { result, quantity }) }
export function closeSceneRepair(id: number): Promise<void> { return put(`/scene/repair_work_orders/${id}/close`) }
