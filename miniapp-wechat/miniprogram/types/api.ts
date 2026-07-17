// 小程序接口类型只描述后端返回的数据形状，不包含页面状态或请求副作用。
export interface CommonResult<T> { code: string; message: string; userTip: string; data: T }
// 登录响应同时承载绑定引导信息和 MES 会话凭证。
export interface MiniAppLoginResponse { bindingRequired: boolean; bindTicket?: string; token?: string; userId?: number; userNo?: string; userName?: string; mobile?: string; roleCodes: string[]; roleNames?: string[]; workshopId?: number; lineId?: number; wechatBound?: boolean; wechatBindingTime?: string; wechatLastLoginTime?: string }
// 账号档案与登录响应分离，刷新个人资料时不重复返回令牌和绑定引导字段。
export interface AuthProfile { userId: number; userNo: string; userName: string; mobile?: string; workshopId?: number; lineId?: number; roleCodes: string[]; roleNames: string[]; wechatBound: boolean; wechatBindingTime?: string; wechatLastLoginTime?: string }
export interface RegistrationRole { id: number; roleCode: string; roleName: string; remark?: string; status: number }
export interface RegisterRequest { userNo: string; userName: string; password: string; roleId: number }
export interface PageResult<T> { list: T[]; total: number; pageNo: number; pageSize: number }
export interface SystemUser { id: number; userNo: string; userName: string; mobile?: string; workshopId?: number; lineId?: number; status: number; roleIds: number[]; roleCodes: string[]; roleNames: string[]; wechatBound: boolean; createTime?: string }
export interface SystemRole { id: number; roleCode: string; roleName: string; remark?: string; status: number }
export interface WorkshopOption { id: number; workshopCode: string; workshopName: string; status: number }
export interface ProductionLineOption { id: number; lineCode: string; lineName: string; workshopId: number; workshopName?: string; status: number }
export interface UserAssignmentRequest { roleIds: number[]; workshopId?: number; lineId?: number }
export interface WechatBindingCode { ticket: string; expiresAt: string; codeImageBase64: string; status: 'PENDING' | 'BOUND' | 'EXPIRED' }
export interface WechatBindingPreview { userName: string; maskedUserNo: string; expiresAt: string }
export interface WechatBindingStatus { status: 'PENDING' | 'BOUND' | 'EXPIRED' }
// 实时看板的指标和任务列表来自后端聚合快照。
export interface Overview { activeTaskCount: number; pausedTaskCount: number; abnormalBatchCount: number; planQuantity: number; inputQuantity: number; goodQuantity: number; defectQuantity: number; equipmentTotalCount: number; runningEquipmentCount: number; unavailableEquipmentCount: number; openAndonCount: number; criticalAndonCount: number; lastRefreshTime: string; dataStatus: string; warnings: string[] }
export interface Task { taskId: number; taskNo: string; productName: string; batchNo: string; planQuantity: number; finishQuantity: number; taskStatus: number; abnormal: boolean; updateTime: string }
export interface RealtimeDashboard { overview: Overview; tasks: Task[] }
export interface ProductionSummary { planQuantity: number; inputQuantity: number; goodQuantity: number; defectQuantity: number; reworkQuantity: number; finishQuantity: number; completionRate: number; defectRate: number; warnings: string[] }
// Trace 同时描述工单、工序、报工和维修记录，数组为空表示该环节暂无记录而非请求失败。
export interface Trace { dataCompleteness: string; warnings: string[]; task?: { taskNo: string; productName: string; batchNo: string }; processHistories: Array<{ processName: string; operateTime: string }>; workReports: Array<{ reportNo: string; reportTime: string; netGoodQuantity: number; netDefectQuantity: number }>; repairRecords: Array<{ summary: string; eventTime: string }> }
