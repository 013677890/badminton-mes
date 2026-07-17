import { get, put } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 设备计数异常池，对齐后端 IntegrationController（/api/integration/device_counts/exceptions）。
 * 类级 @RequiresRoles：ADMIN、PMC。忽略/重试为写操作。
 */

/** 设备计数写入请求（重试时提交完整修正请求） */
export interface DeviceCountWriteReq {
  sourceSystem: string
  externalKey: string
  equipmentCode: string
  dispatchNo: string
  processCode: string
  /** yyyy-MM-dd HH:mm:ss */
  collectTime: string
  countValue: number
}

/** 外部接口写入结果（重试后返回） */
export interface IntegrationWriteResult {
  logId: number | null
  /** SUCCESS / FAILED / DUPLICATE */
  status: string
  businessId: number | null
  businessNo: string | null
  errorCode: string | null
  message: string | null
}

export interface DeviceCountException {
  id: number
  sourceSystem: string
  externalKey: string
  equipmentCode: string
  dispatchNo: string
  processCode: string
  collectTime: string
  countValue: number
  /** 原始请求 JSON 快照 */
  requestSnapshot: string | null
  /** 重试请求 JSON 快照 */
  retryRequestSnapshot: string | null
  /** 见 constants/integration.ts EXCEPTION_TYPE_TEXT */
  exceptionType: string
  errorCode: string | null
  errorMessage: string | null
  /** 见 constants/integration.ts DEVICE_EXCEPTION_HANDLE_STATUS_MAP */
  handleStatus: number
  handleBy: number | null
  handleTime: string | null
  handleRemark: string | null
  retryLogId: number | null
  retryRecordId: number | null
  createTime: string
  updateTime: string
}

export interface DeviceCountExceptionPageParams {
  sourceSystem?: string
  equipmentCode?: string
  exceptionType?: string
  handleStatus?: number
  /** yyyy-MM-dd HH:mm:ss */
  startTime?: string
  /** yyyy-MM-dd HH:mm:ss */
  endTime?: string
}

/** 分页查询设备计数异常池 */
export function getDeviceCountExceptionPage(
  params: DeviceCountExceptionPageParams & PageParam,
): Promise<PageResult<DeviceCountException>> {
  return get('/integration/device_counts/exceptions', params)
}

/** 忽略一条待处理设备计数异常，remark 选填 */
export function ignoreDeviceCountException(
  id: number,
  remark?: string,
): Promise<boolean> {
  return put(`/integration/device_counts/exceptions/${id}/ignore`, remark ? { remark } : {})
}

/** 使用修正后的请求重新处理设备计数异常，返回写入结果 */
export function retryDeviceCountException(
  id: number,
  data: DeviceCountWriteReq,
): Promise<IntegrationWriteResult> {
  return put(`/integration/device_counts/exceptions/${id}/retry`, data)
}
