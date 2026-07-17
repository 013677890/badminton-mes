import { get } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 外部接口写入日志，对齐后端 IntegrationController（/api/integration/write_logs）。
 * 类级 @RequiresRoles：ADMIN、PMC。查询参数 interfaceType/sourceSystem/businessKey/writeStatus。
 */

export interface IntegrationWriteLog {
  id: number
  /** 见 constants/integration.ts INTERFACE_TYPE_TEXT */
  interfaceType: string
  sourceSystem: string
  /** 来源侧业务键 */
  businessKey: string
  /** 请求 JSON 快照（字符串，展示时需 try/catch 解析） */
  requestSnapshot: string | null
  /** 见 constants/integration.ts WRITE_STATUS_MAP */
  writeStatus: number
  /** MES 业务主键 */
  resultId: number | null
  /** MES 业务编号 */
  resultNo: string | null
  errorCode: string | null
  errorMessage: string | null
  createTime: string
  updateTime: string
}

export interface IntegrationWriteLogPageParams {
  interfaceType?: string
  sourceSystem?: string
  businessKey?: string
  writeStatus?: number
}

/** 分页查询外部接口写入日志 */
export function getWriteLogPage(
  params: IntegrationWriteLogPageParams & PageParam,
): Promise<PageResult<IntegrationWriteLog>> {
  return get('/integration/write_logs', params)
}
