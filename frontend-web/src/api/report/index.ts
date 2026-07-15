import { download, get } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

export interface ReportQueryParams {
  startTime: string; endTime: string; workshopId?: number; lineId?: number; productId?: number
  workOrderId?: number; taskId?: number; processId?: number; shiftId?: number; batchNo?: string; status?: number
}
export interface ProductionSummary {
  planQuantity: number; inputQuantity: number; goodQuantity: number; defectQuantity: number
  reworkQuantity: number; finishQuantity: number; occurrenceInputQuantity: number; reversalInputQuantity: number
  occurrenceGoodQuantity: number; reversalGoodQuantity: number; occurrenceDefectQuantity: number
  reversalDefectQuantity: number; completionRate: number; defectRate: number; warnings: string[]
}
export interface ProductionDetail {
  reportId: number; reportNo: string; taskId: number; taskNo: string; workOrderNo: string
  productId: number; productName: string; batchNo: string; workshopId: number; workshopName: string
  lineId: number; lineName: string; processId: number; processName: string; recordType: number
  sourceReportId: number | null; occurrenceInputQuantity: number; reversalInputQuantity: number; netInputQuantity: number
  occurrenceGoodQuantity: number; reversalGoodQuantity: number; netGoodQuantity: number
  occurrenceDefectQuantity: number; reversalDefectQuantity: number; netDefectQuantity: number
  occurrenceReworkQuantity: number; reversalReworkQuantity: number; netReworkQuantity: number; reportTime: string
}
export interface DefectSummary {
  sceneDefectQuantity: number; qualityDefectQuantity: number; repairRecheckDefectQuantity: number
  comprehensiveDefectQuantity: number; sceneOccurrenceQuantity: number; sceneReversalQuantity: number
  sourceRecordCount: number; comprehensiveEventCount: number; mergedDuplicateCount: number
  reportInputQuantity: number; sceneDefectRate: number; comprehensiveDefectRate: number; warnings: string[]
}
export interface DefectDetail {
  sourceType: string; sourceId: number; sourceDetailId: number | null; defectGroupNo: string | null
  taskId: number; taskNo: string; workOrderNo: string; productId: number; productName: string
  batchNo: string; workshopId: number; lineId: number; processId: number; processName: string
  defectCode: string; defectName: string; occurrenceQuantity: number; reversalQuantity: number
  netQuantity: number; detectedTime: string
}
export interface RealtimeOverview {
  activeTaskCount: number; pausedTaskCount: number; abnormalBatchCount: number; planQuantity: number
  inputQuantity: number; goodQuantity: number; defectQuantity: number; equipmentTotalCount: number
  runningEquipmentCount: number; unavailableEquipmentCount: number; openAndonCount: number
  criticalAndonCount: number; lastRefreshTime: string; dataStatus: string; warnings: string[]
}
export interface RealtimeTask {
  taskId: number; taskNo: string; workOrderNo: string; productId: number; productName: string
  batchNo: string; workshopId: number; workshopName: string; lineId: number; lineName: string
  planQuantity: number; inputQuantity: number; goodQuantity: number; defectQuantity: number
  finishQuantity: number; taskStatus: number; abnormal: boolean; actualStartTime: string | null; updateTime: string
}
export interface KanbanSnapshot {
  snapshotTime: string
  lastRefreshTime: string
  dataStatus: string
  sourceWarnings: string[]
  version: number
  overview: RealtimeOverview
}
export interface ProductTrace {
  dataCompleteness: string; warnings: string[]; task: Record<string, any> | null; workOrder: Record<string, any> | null
  barcodes: Array<Record<string, any>>; barcodeUses: Array<Record<string, any>>
  processHistories: Array<Record<string, any>>; workReports: Array<Record<string, any>>
  materials: Array<Record<string, any>>; packingDetails: Array<Record<string, any>>
  qualityDefects: Array<Record<string, any>>; repairRecords: Array<Record<string, any>>
  equipmentStatuses: Array<Record<string, any>>; andonExceptions: Array<Record<string, any>>
}

export function getProductionSummary(params: ReportQueryParams): Promise<ProductionSummary> { return get('/report/production_outputs/summary', params) }
export function getProductionDetails(params: ReportQueryParams & PageParam): Promise<PageResult<ProductionDetail>> { return get('/report/production_outputs/details', params) }
export function getWorkshopPeriodSummary(params: ReportQueryParams): Promise<ProductionSummary> { return get('/report/workshop_periods/summary', params) }
export function getWorkshopPeriodDetails(params: ReportQueryParams & PageParam): Promise<PageResult<ProductionDetail>> { return get('/report/workshop_periods/details', params) }
export function exportProductionReport(kind: 'production_outputs' | 'workshop_periods', params: ReportQueryParams) { return download(`/report/${kind}/export`, params) }
export function getDefectSummary(params: ReportQueryParams): Promise<DefectSummary> { return get('/report/defects/summary', params) }
export function getDefectPage(params: ReportQueryParams & PageParam & { view?: 'SOURCE' | 'COMPREHENSIVE' }): Promise<PageResult<DefectDetail>> { return get('/report/defects/page', params) }
export function exportDefectReport(params: ReportQueryParams) { return download('/report/defects/export', params) }
export function getRealtimeOverview(params?: { workshopId?: number; lineId?: number; productId?: number }): Promise<RealtimeOverview> { return get('/report/realtime_production/overview', params) }
export function getRealtimeTasks(params?: { workshopId?: number; lineId?: number; productId?: number }): Promise<RealtimeTask[]> { return get('/report/realtime_production/tasks', params) }
export function traceProduct(params: { batchCode?: string; barcodeValue?: string; workOrderNo?: string; taskNo?: string }): Promise<ProductTrace> { return get('/report/traces/products', params) }
export function traceBarcode(barcodeValue: string): Promise<ProductTrace> { return get(`/report/traces/barcodes/${encodeURIComponent(barcodeValue)}`) }
export function getKanbanSnapshot(scope: 'central' | 'line' | 'workshop', id?: number): Promise<KanbanSnapshot> {
  if (scope === 'central') return get('/report/kanban/central')
  return get(`/report/kanban/${scope === 'line' ? 'lines' : 'workshops'}/${id}`)
}
