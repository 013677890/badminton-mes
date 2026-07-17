import { get, post, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

// ==================== 生产任务 ====================

/** 生产任务保存请求 */
export interface SceneTaskSaveReq {
  workOrderId: number
  lineId: number
  shiftId?: number
  planDate: string
  planQuantity: number
  planStartTime: string
  planEndTime: string
}

/** 生产任务分页查询参数 */
export interface SceneTaskPageParams extends PageParam {
  taskNo?: string
  workshopId?: number
  lineId?: number
  taskStatus?: number
  planDate?: string
}

/** 生产任务响应 */
export interface SceneTask {
  id: number
  taskNo: string
  workOrderId: number
  workOrderNo: string
  productId: number
  productCode: string
  productName: string
  batchNo: string
  routingId: number
  routingCode: string
  routingVersion: string
  workshopId: number
  workshopName: string
  lineId: number
  lineName: string
  shiftId: number
  planDate: string
  planQuantity: number
  inputQuantity: number
  goodQuantity: number
  defectQuantity: number
  reworkQuantity: number
  finishQuantity: number
  planStartTime: string
  planEndTime: string
  actualStartTime: string
  actualEndTime: string
  taskStatus: number
  pauseReason: string
}

/** 任务阶段进度 */
export interface SceneTaskProgress {
  taskId: number
  taskStatus: number
  planQuantity: number
  operationTotal: number
  operationCompleted: number
  currentOperationId: number
  currentProcessName: string
}

export function createSceneTask(data: SceneTaskSaveReq): Promise<number> {
  return post('/scene/production_tasks', data)
}

export function updateSceneTask(id: number, data: SceneTaskSaveReq): Promise<boolean> {
  return put(`/scene/production_tasks/${id}`, data)
}

export function auditSceneTask(id: number): Promise<boolean> {
  return put(`/scene/production_tasks/${id}/audit`)
}

export function releaseSceneTask(id: number): Promise<boolean> {
  return put(`/scene/production_tasks/${id}/release`)
}

export function startSceneTask(id: number): Promise<boolean> {
  return put(`/scene/production_tasks/${id}/start`)
}

export function pauseSceneTask(id: number, reason: string): Promise<boolean> {
  return put(`/scene/production_tasks/${id}/pause`, { reason })
}

export function resumeSceneTask(id: number): Promise<boolean> {
  return put(`/scene/production_tasks/${id}/resume`)
}

export function closeSceneTask(id: number, reason: string): Promise<boolean> {
  return put(`/scene/production_tasks/${id}/close`, { reason })
}

export function getSceneTaskPage(
  params: SceneTaskPageParams,
): Promise<PageResult<SceneTask>> {
  return get('/scene/production_tasks/page', params)
}

export function getSceneTask(id: number): Promise<SceneTask> {
  return get(`/scene/production_tasks/${id}`)
}

export function getSceneTaskProgress(id: number): Promise<SceneTaskProgress> {
  return get(`/scene/production_tasks/${id}/progress`)
}

// ==================== 生产参数 ====================

/** 生产参数保存请求 */
export interface SceneParameterSaveReq {
  paramCode: string
  paramName: string
  paramValue: string
  valueType: number
  workshopId?: number
  lineId?: number
  productId?: number
  remark?: string
  changeReason: string
}

export interface SceneParameterPageParams extends PageParam {
  paramCode?: string
  workshopId?: number
  lineId?: number
  productId?: number
  status?: number
}

export interface SceneParameter {
  id: number
  paramCode: string
  paramName: string
  paramValue: string
  valueType: number
  workshopId: number
  lineId: number
  productId: number
  remark: string
  status: number
  createTime: string
  updateTime: string
}

export interface SceneEffectiveParamReq {
  paramCode: string
  workshopId?: number
  lineId?: number
  productId?: number
}

export interface SceneParameterChangeLog {
  id: number
  beforeValue: string
  afterValue: string
  beforeStatus: number
  afterStatus: number
  changeReason: string
  operatorId: number
  operateTime: string
}

export function createSceneParameter(data: SceneParameterSaveReq): Promise<number> {
  return post('/scene/production_parameters', data)
}

export function updateSceneParameter(id: number, data: SceneParameterSaveReq): Promise<boolean> {
  return put(`/scene/production_parameters/${id}`, data)
}

export function enableSceneParameter(id: number, reason: string): Promise<boolean> {
  return put(`/scene/production_parameters/${id}/enable`, { reason })
}

export function disableSceneParameter(id: number, reason: string): Promise<boolean> {
  return put(`/scene/production_parameters/${id}/disable`, { reason })
}

export function getSceneParameterPage(
  params: SceneParameterPageParams,
): Promise<PageResult<SceneParameter>> {
  return get('/scene/production_parameters/page', params)
}

export function getSceneParameter(id: number): Promise<SceneParameter> {
  return get(`/scene/production_parameters/${id}`)
}

export function getEffectiveParameter(params: SceneEffectiveParamReq): Promise<SceneParameter> {
  return get('/scene/production_parameters/effective', params)
}

export function getSceneParameterChangeLogs(id: number): Promise<SceneParameterChangeLog[]> {
  return get(`/scene/production_parameters/${id}/change_logs`)
}

// ==================== 派工单 ====================

export interface SceneDispatchPageParams extends PageParam {
  dispatchNo?: string
  taskId?: number
  dispatchStatus?: number
}

/** 派工工序明细（同时作为工序作业行） */
export interface SceneDispatchDetail {
  id: number
  processId: number
  processCode: string
  processName: string
  seq: number
  keyProcess: boolean
  inspect: boolean
  scanRequired: boolean
  sopId: number
  sopCode: string
  sopName: string
  sopVersion: string
  stationId: number
  userId: number
  equipmentId: number
  planQuantity: number
  detailStatus: number
  paused: boolean
  pauseReason: string
  actualStartTime: string
  actualEndTime: string
}

export interface SceneDispatchOrder {
  id: number
  dispatchNo: string
  taskId: number
  routingId: number
  routingCode: string
  routingVersion: string
  dispatchStatus: number
  createTime?: string
  operations: SceneDispatchDetail[]
}

export function generateDispatchOrder(taskId: number): Promise<number> {
  return post('/scene/dispatch_orders/generate', { taskId })
}

export function confirmDispatchOrder(id: number): Promise<boolean> {
  return put(`/scene/dispatch_orders/${id}/confirm`)
}

export function cancelDispatchOrder(id: number): Promise<boolean> {
  return put(`/scene/dispatch_orders/${id}/cancel`)
}

export function getDispatchOrderPage(
  params: SceneDispatchPageParams,
): Promise<PageResult<SceneDispatchOrder>> {
  return get('/scene/dispatch_orders/page', params)
}

export function getDispatchOrder(id: number): Promise<SceneDispatchOrder> {
  return get(`/scene/dispatch_orders/${id}`)
}

export function getDispatchOperations(id: number): Promise<SceneDispatchDetail[]> {
  return get(`/scene/dispatch_orders/${id}/operations`)
}

// ==================== 工序作业 ====================

export interface SceneOperationJobPageParams extends PageParam {
  taskId?: number
  userId?: number
  stationId?: number
  equipmentId?: number
  detailStatus?: number
}

export interface SceneOperationScanReq {
  barcodeValue: string
  equipmentId?: number
}

export function getOperationJobPage(
  params: SceneOperationJobPageParams,
): Promise<PageResult<SceneDispatchDetail>> {
  return get('/scene/operation_jobs/page', params)
}

export function getMyOperationJobs(): Promise<SceneDispatchDetail[]> {
  return get('/scene/operation_jobs/my')
}

export function getOperationJob(id: number): Promise<SceneDispatchDetail> {
  return get(`/scene/operation_jobs/${id}`)
}

export function scanOperationJob(id: number, data: SceneOperationScanReq): Promise<boolean> {
  return post(`/scene/operation_jobs/${id}/scan`, data)
}

export function startOperationJob(id: number): Promise<boolean> {
  return put(`/scene/operation_jobs/${id}/start`)
}

export function pauseOperationJob(id: number, reason: string): Promise<boolean> {
  return put(`/scene/operation_jobs/${id}/pause`, { reason })
}

export function finishOperationJob(id: number): Promise<boolean> {
  return put(`/scene/operation_jobs/${id}/finish`)
}

// ==================== 产品批次状态 ====================

export interface SceneProductStatusPageParams extends PageParam {
  batchNo?: string
  taskId?: number
  batchStatus?: number
  abnormal?: boolean
}

export interface SceneProductStatus {
  id: number
  batchNo: string
  taskId: number
  productId: number
  currentProcessId: number
  currentProcessName: string
  batchStatus: number
  abnormal: boolean
  updateTime: string
}

export interface SceneStatusHistory {
  id: number
  fromStatus: number
  toStatus: number
  processId: number
  changeReason: string
  operatorId: number
  operateTime: string
}

export interface SceneProcessHistory {
  id: number
  dispatchDetailId: number
  processId: number
  processCode: string
  processName: string
  actionType: number
  operatorId: number
  actionReason: string
  operateTime: string
}

export function getProductStatusByBatch(batchCode: string): Promise<SceneProductStatus> {
  return get(`/scene/product_statuses/by_batch/${batchCode}`)
}

export function getProductStatusPage(
  params: SceneProductStatusPageParams,
): Promise<PageResult<SceneProductStatus>> {
  return get('/scene/product_statuses/page', params)
}

export function getProductStatusHistories(id: number): Promise<SceneStatusHistory[]> {
  return get(`/scene/product_statuses/${id}/histories`)
}

export function getProductStatusOperationHistories(id: number): Promise<SceneProcessHistory[]> {
  return get(`/scene/product_statuses/${id}/operation_histories`)
}

// ==================== 返修工单 ====================

export interface SceneRepairCreateReq {
  sourceReportId: number
  batchNo: string
  defectQuantity: number
  repairQuantity: number
  reason: string
}

export interface SceneRepairRecordCreateReq {
  quantity: number
  description: string
}

export interface SceneRepairRecheckReq {
  /** RELEASED | CONTINUE_REPAIR | SCRAPPED */
  result: string
  quantity: number
}

export interface SceneRepairWorkOrder {
  id: number
  repairNo: string
  sourceReportId: number
  taskId: number
  batchNo: string
  defectQuantity: number
  repairQuantity: number
  /** 返修状态枚举名（字符串） */
  status: string
  reason: string
  assigneeId: number
  recheckResult: string
  recheckQuantity: number
  createdTime: string
  updatedTime: string
}

export function createRepairWorkOrder(data: SceneRepairCreateReq): Promise<number> {
  return post('/scene/repair_work_orders', data)
}

export function assignRepairWorkOrder(id: number, assigneeId: number): Promise<boolean> {
  return put(`/scene/repair_work_orders/${id}/assign`, undefined, { params: { assigneeId } })
}

export function startRepairWorkOrder(id: number): Promise<boolean> {
  return put(`/scene/repair_work_orders/${id}/start`)
}

export function addRepairRecord(id: number, data: SceneRepairRecordCreateReq): Promise<boolean> {
  return post(`/scene/repair_work_orders/${id}/records`, data)
}

export function recheckRepairWorkOrder(id: number, data: SceneRepairRecheckReq): Promise<boolean> {
  return post(`/scene/repair_work_orders/${id}/recheck`, data)
}

export function closeRepairWorkOrder(id: number): Promise<boolean> {
  return put(`/scene/repair_work_orders/${id}/close`)
}

export function getRepairWorkOrder(id: number): Promise<SceneRepairWorkOrder> {
  return get(`/scene/repair_work_orders/${id}`)
}
