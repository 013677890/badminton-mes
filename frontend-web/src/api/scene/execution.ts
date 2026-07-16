import { post, put } from '@/utils/request'

// ---------- 报工 ----------

/** 报工提交请求（人工报工与设备计数报工共用） */
export interface SceneWorkReportSubmitReq {
  /** 客户端幂等请求号 */
  requestNo: string
  /** 工序派工明细 id */
  dispatchDetailId: number
  /** 投入数量 */
  inputQuantity: number
  /** 良品数量 */
  goodQuantity: number
  /** 不良数量 */
  defectQuantity: number
  /** 返修数量 */
  reworkQuantity: number
  /** 报工扫码值（是否必填由 must_scan_report 参数决定） */
  barcodeValue?: string
  /** 业务报工时间 */
  reportTime: string
}

/** 报工冲销请求 */
export interface SceneWorkReportReverseReq {
  requestNo: string
  reason: string
}

/** 人工报工提交 -> POST /scene/work_reports/submit */
export function submitSceneWorkReport(data: SceneWorkReportSubmitReq): Promise<number> {
  return post('/scene/work_reports/submit', data)
}

/** 设备计数报工提交 -> POST /scene/work_reports/device_count */
export function submitDeviceCountReport(data: SceneWorkReportSubmitReq): Promise<number> {
  return post('/scene/work_reports/device_count', data)
}

/** 报工全额冲销 -> PUT /scene/work_reports/{id}/reverse */
export function reverseSceneWorkReport(
  id: number,
  data: SceneWorkReportReverseReq,
): Promise<number> {
  return put(`/scene/work_reports/${id}/reverse`, data)
}

// ---------- 完工单 ----------

/** 从任务创建完工单 -> POST /scene/completion_orders/create_from_task */
export function createCompletionFromTask(
  taskId: number,
  finishQuantity: number,
): Promise<number> {
  return post('/scene/completion_orders/create_from_task', { taskId, finishQuantity })
}

/** 完工单草稿/驳回单修改 -> PUT /scene/completion_orders/{id} */
export function updateSceneCompletionOrder(id: number, finishQuantity: number): Promise<boolean> {
  return put(`/scene/completion_orders/${id}`, { finishQuantity })
}

/** 完工单提交审核 -> PUT /scene/completion_orders/{id}/submit */
export function submitSceneCompletionOrder(id: number): Promise<boolean> {
  return put(`/scene/completion_orders/${id}/submit`)
}

/** 完工单审核（approved=true 通过 / false 驳回） -> PUT /scene/completion_orders/{id}/audit */
export function auditSceneCompletionOrder(
  id: number,
  approved: boolean,
  remark?: string,
): Promise<boolean> {
  return put(`/scene/completion_orders/${id}/audit`, { approved, remark })
}

/** 完工单人工同步至外部系统 -> POST /scene/completion_orders/{id}/sync */
export function syncSceneCompletionOrder(id: number): Promise<boolean> {
  return post(`/scene/completion_orders/${id}/sync`)
}
