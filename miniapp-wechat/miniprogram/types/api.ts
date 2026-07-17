// 小程序接口类型只描述后端返回的数据形状，不包含页面状态或请求副作用。
export interface CommonResult<T> { code: string; message: string; userTip: string; data: T }
// 登录响应同时承载绑定引导信息和 MES 会话凭证。
export interface MiniAppLoginResponse { bindingRequired: boolean; bindTicket?: string; token?: string; userId?: number; userNo?: string; userName?: string; roleCodes: string[]; workshopId?: number; lineId?: number }
// 实时看板的指标和任务列表来自后端聚合快照。
export interface Overview { activeTaskCount: number; pausedTaskCount: number; abnormalBatchCount: number; planQuantity: number; inputQuantity: number; goodQuantity: number; defectQuantity: number; equipmentTotalCount: number; runningEquipmentCount: number; unavailableEquipmentCount: number; openAndonCount: number; criticalAndonCount: number; lastRefreshTime: string; dataStatus: string; warnings: string[] }
export interface Task { taskId: number; taskNo: string; productName: string; batchNo: string; planQuantity: number; finishQuantity: number; taskStatus: number; abnormal: boolean; updateTime: string }
export interface RealtimeDashboard { overview: Overview; tasks: Task[] }
export interface ProductionSummary { planQuantity: number; inputQuantity: number; goodQuantity: number; defectQuantity: number; reworkQuantity: number; finishQuantity: number; completionRate: number; defectRate: number; warnings: string[] }
// Trace 同时描述工单、工序、报工和维修记录，数组为空表示该环节暂无记录而非请求失败。
export interface Trace { dataCompleteness: string; warnings: string[]; task?: { taskNo: string; productName: string; batchNo: string }; processHistories: Array<{ processName: string; operateTime: string }>; workReports: Array<{ reportNo: string; reportTime: string; netGoodQuantity: number; netDefectQuantity: number }>; repairRecords: Array<{ summary: string; eventTime: string }> }
