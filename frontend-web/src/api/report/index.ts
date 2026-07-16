import { download, get } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 报表分析模块接口，对齐后端 report 模块 Controller：
 * - ProductionOutputReportController  /report/production_outputs
 * - WorkshopPeriodReportController    /report/workshop_periods
 * - RealtimeProductionController      /report/realtime_production
 * - DefectQueryController             /report/defects
 * - TraceController                   /report/traces
 * - KanbanController                  /report/kanban
 *
 * 前端 baseURL 已含 /api 前缀，此处路径不再带 /api。
 */

// ---------- 通用查询条件 ----------

/** 产量/时段/不良报表统一查询条件（不含分页，分页由调用方按需叠加） */
export interface ReportQueryReq {
  /** ISO DATE_TIME，如 2026-07-16T08:00:00 */
  startTime: string
  endTime: string
  workshopId?: number
  lineId?: number
  productId?: number
  workOrderId?: number
  taskId?: number
  processId?: number
  shiftId?: number
  batchNo?: string
  status?: number
}

/** 实时生产查询条件 */
export interface RealtimeReportQueryReq {
  workshopId?: number
  lineId?: number
  productId?: number
}

// ---------- 产量 / 车间时段报表 ----------

/** 产量汇总（ProductionReportRespVO.Summary） */
export interface ProductionReportSummary {
  planQuantity: number
  inputQuantity: number
  goodQuantity: number
  defectQuantity: number
  reworkQuantity: number
  finishQuantity: number
  occurrenceInputQuantity: number
  reversalInputQuantity: number
  occurrenceGoodQuantity: number
  reversalGoodQuantity: number
  occurrenceDefectQuantity: number
  reversalDefectQuantity: number
  completionRate: number
  defectRate: number
  warnings: string[]
}

/** 报工净额与审计发生额明细（ProductionReportRespVO.Detail） */
export interface ProductionReportDetail {
  reportId?: number
  reportNo?: string
  taskId?: number
  taskNo?: string
  workOrderNo?: string
  productId?: number
  productName?: string
  batchNo?: string
  workshopId?: number
  workshopName?: string
  lineId?: number
  lineName?: string
  processId?: number
  processName?: string
  /** 1 正常发生 / 2 冲销 */
  recordType?: number
  sourceReportId?: number
  occurrenceInputQuantity: number
  reversalInputQuantity: number
  netInputQuantity: number
  occurrenceGoodQuantity: number
  reversalGoodQuantity: number
  netGoodQuantity: number
  occurrenceDefectQuantity: number
  reversalDefectQuantity: number
  netDefectQuantity: number
  occurrenceReworkQuantity: number
  reversalReworkQuantity: number
  netReworkQuantity: number
  reportTime?: string
}

// ---------- 实时生产 ----------

/** 实时生产总览（RealtimeProductionRespVO.Overview） */
export interface RealtimeProductionOverview {
  activeTaskCount: number
  pausedTaskCount: number
  abnormalBatchCount: number
  planQuantity: number
  inputQuantity: number
  goodQuantity: number
  defectQuantity: number
  equipmentTotalCount: number
  runningEquipmentCount: number
  unavailableEquipmentCount: number
  openAndonCount: number
  criticalAndonCount: number
  lastRefreshTime?: string
  dataStatus?: string
  warnings: string[]
}

/** 当前在制任务（RealtimeProductionRespVO.Task） */
export interface RealtimeProductionTask {
  taskId?: number
  taskNo?: string
  workOrderNo?: string
  productId?: number
  productName?: string
  batchNo?: string
  workshopId?: number
  workshopName?: string
  lineId?: number
  lineName?: string
  planQuantity?: number
  inputQuantity?: number
  goodQuantity?: number
  defectQuantity?: number
  finishQuantity?: number
  taskStatus?: number
  abnormal: boolean
  actualStartTime?: string
  updateTime?: string
}

// ---------- 不良报表 ----------

/** 不良来源明细 / 综合归并行（DefectReportRespVO.Detail） */
export interface DefectReportDetail {
  sourceType?: string
  sourceId?: number
  sourceDetailId?: number
  defectGroupNo?: string
  taskId?: number
  taskNo?: string
  workOrderNo?: string
  productId?: number
  productName?: string
  batchNo?: string
  workshopId?: number
  lineId?: number
  processId?: number
  processName?: string
  defectCode?: string
  defectName?: string
  occurrenceQuantity: number
  reversalQuantity: number
  netQuantity: number
  detectedTime?: string
}

/** 不良聚合汇总（DefectReportRespVO.Summary） */
export interface DefectReportSummary {
  sceneDefectQuantity: number
  qualityDefectQuantity: number
  repairRecheckDefectQuantity: number
  comprehensiveDefectQuantity: number
  sceneOccurrenceQuantity: number
  sceneReversalQuantity: number
  sourceRecordCount: number
  comprehensiveEventCount: number
  mergedDuplicateCount: number
  reportInputQuantity: number
  sceneDefectRate: number
  comprehensiveDefectRate: number
  warnings: string[]
}

/** 不良明细分页参数：在统一查询条件上叠加视图类型 */
export interface DefectPageParams extends ReportQueryReq {
  /** SOURCE 来源明细 / COMPREHENSIVE 综合归并 */
  view: string
}

// ---------- 产品追溯 ----------

/** 产品追溯入口条件，至少提供一个业务键 */
export interface ProductTraceQueryReq {
  batchCode?: string
  barcodeValue?: string
  workOrderNo?: string
  taskNo?: string
}

/** 跨组可选来源的稳定展示投影（ProductTraceRespVO.OptionalSourceItem） */
export interface TraceOptionalSourceItem {
  sourceType?: string
  sourceId?: string
  summary?: string
  eventTime?: string
}

/** 生产任务快照（ProductTraceRespVO.Task） */
export interface TraceTask {
  id?: number
  taskNo?: string
  productId?: number
  productCode?: string
  productName?: string
  batchNo?: string
  workshopId?: number
  workshopName?: string
  lineId?: number
  lineName?: string
  planQuantity?: number
  inputQuantity?: number
  goodQuantity?: number
  defectQuantity?: number
  reworkQuantity?: number
  finishQuantity?: number
  taskStatus?: number
  actualStartTime?: string
  actualEndTime?: string
}

/** 上游工单信息（ProductTraceRespVO.WorkOrder） */
export interface TraceWorkOrder {
  id?: number
  workOrderNo?: string
  batchNo?: string
  productId?: number
  productName?: string
  spec?: string
  planQuantity?: number
  inputQuantity?: number
  finishQuantity?: number
  defectQuantity?: number
  reworkQuantity?: number
  orderStatus?: number
}

/** 条码实例（ProductTraceRespVO.Barcode） */
export interface TraceBarcode {
  id?: number
  barcodeValue?: string
  barcodeTypeId?: number
  barcodeMode?: number
  productId?: number
  materialId?: number
  batchNo?: string
  barcodeStatus?: number
  createTime?: string
}

/** 条码扫码使用记录（ProductTraceRespVO.BarcodeUse） */
export interface TraceBarcodeUse {
  id?: number
  barcodeId?: number
  processId?: number
  userId?: number
  equipmentId?: number
  useType?: number
  businessTime?: string
}

/** 工序履历（ProductTraceRespVO.ProcessHistory） */
export interface TraceProcessHistory {
  id?: number
  processId?: number
  processCode?: string
  processName?: string
  actionType?: number
  operatorId?: number
  actionReason?: string
  operateTime?: string
}

/** 报工发生、冲销和净额记录（ProductTraceRespVO.WorkReport） */
export interface TraceWorkReport {
  id?: number
  reportNo?: string
  recordType?: number
  sourceReportId?: number
  processId?: number
  occurrenceInputQuantity: number
  reversalInputQuantity: number
  netInputQuantity: number
  occurrenceGoodQuantity: number
  reversalGoodQuantity: number
  netGoodQuantity: number
  occurrenceDefectQuantity: number
  reversalDefectQuantity: number
  netDefectQuantity: number
  reverseReason?: string
  reportTime?: string
}

/** 工单物料需求（ProductTraceRespVO.Material） */
export interface TraceMaterial {
  materialId?: number
  materialCode?: string
  materialName?: string
  requireQuantity?: number
  issuedQuantity?: number
  materialBatchNo?: string
}

/** 产品批次完整追溯响应（ProductTraceRespVO） */
export interface ProductTraceRespVO {
  dataCompleteness?: string
  warnings: string[]
  task?: TraceTask
  workOrder?: TraceWorkOrder
  barcodes: TraceBarcode[]
  barcodeUses: TraceBarcodeUse[]
  processHistories: TraceProcessHistory[]
  workReports: TraceWorkReport[]
  materials: TraceMaterial[]
  packingDetails: TraceOptionalSourceItem[]
  qualityDefects: TraceOptionalSourceItem[]
  repairRecords: TraceOptionalSourceItem[]
  equipmentStatuses: TraceOptionalSourceItem[]
  andonExceptions: TraceOptionalSourceItem[]
}

// ---------- 电子看板 ----------

/** 电子看板快照（KanbanSnapshotServiceImpl） */
export interface KanbanSnapshot {
  snapshotTime?: string
  lastRefreshTime?: string
  /** FRESH / PARTIAL，后端后续可扩展其他状态 */
  dataStatus?: string
  sourceWarnings: string[]
  version?: number
  overview: RealtimeProductionOverview
}

// ---------- 产量报表接口 ----------

/** 产量汇总 */
export function getProductionOutputSummary(
  params: ReportQueryReq,
): Promise<ProductionReportSummary> {
  return get('/report/production_outputs/summary', params)
}

/** 产量明细分页 */
export function getProductionOutputDetails(
  params: ReportQueryReq & PageParam,
): Promise<PageResult<ProductionReportDetail>> {
  return get('/report/production_outputs/details', params)
}

/** 产量报表导出（blob 下载） */
export function exportProductionOutput(params: ReportQueryReq): Promise<void> {
  return download('/report/production_outputs/export', params, '产量报表')
}

// ---------- 车间时段报表接口 ----------

/** 车间时段汇总 */
export function getWorkshopPeriodSummary(
  params: ReportQueryReq,
): Promise<ProductionReportSummary> {
  return get('/report/workshop_periods/summary', params)
}

/** 车间时段明细分页 */
export function getWorkshopPeriodDetails(
  params: ReportQueryReq & PageParam,
): Promise<PageResult<ProductionReportDetail>> {
  return get('/report/workshop_periods/details', params)
}

/** 车间时段报表导出（blob 下载） */
export function exportWorkshopPeriod(params: ReportQueryReq): Promise<void> {
  return download('/report/workshop_periods/export', params, '车间时段报表')
}

// ---------- 实时生产接口 ----------

/** 实时生产总览 */
export function getRealtimeOverview(
  params: RealtimeReportQueryReq,
): Promise<RealtimeProductionOverview> {
  return get('/report/realtime_production/overview', params)
}

/** 当前在制任务列表 */
export function getRealtimeTasks(
  params: RealtimeReportQueryReq,
): Promise<RealtimeProductionTask[]> {
  return get('/report/realtime_production/tasks', params)
}

// ---------- 不良报表接口 ----------

/** 不良明细分页（view 区分来源明细 / 综合归并） */
export function getDefectPage(
  params: DefectPageParams & PageParam,
): Promise<PageResult<DefectReportDetail>> {
  return get('/report/defects/page', params)
}

/** 不良聚合汇总 */
export function getDefectSummary(params: ReportQueryReq): Promise<DefectReportSummary> {
  return get('/report/defects/summary', params)
}

/** 不良报表导出（blob 下载） */
export function exportDefect(params: ReportQueryReq): Promise<void> {
  return download('/report/defects/export', params, '不良报表')
}

// ---------- 产品追溯接口 ----------

/** 按批次/条码/工单/任务查询产品追溯 */
export function getProductTrace(params: ProductTraceQueryReq): Promise<ProductTraceRespVO> {
  return get('/report/traces/products', params)
}

/** 按条码值查询追溯 */
export function getBarcodeTrace(barcodeValue: string): Promise<ProductTraceRespVO> {
  return get(`/report/traces/barcodes/${encodeURIComponent(barcodeValue)}`)
}

// ---------- 电子看板接口 ----------

/** 产线看板快照 */
export function getLineKanban(lineId: number): Promise<KanbanSnapshot> {
  return get(`/report/kanban/lines/${lineId}`)
}

/** 车间看板快照 */
export function getWorkshopKanban(workshopId: number): Promise<KanbanSnapshot> {
  return get(`/report/kanban/workshops/${workshopId}`)
}

/** 中央看板快照 */
export function getCentralKanban(): Promise<KanbanSnapshot> {
  return get('/report/kanban/central')
}
