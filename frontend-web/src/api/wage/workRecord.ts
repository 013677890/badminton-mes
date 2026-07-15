import { get } from '@/utils/request'
import type { PageParam, PageResult } from '@/utils/request'

/**
 * 已审核报工计件快照接口，对齐后端 WageWorkRecordController（/api/wage/work_records）。
 * 快照由现场报工审核后经 /import 幂等推送（B 组现场模块负责），前端只读查询。
 * 查询限 ADMIN/WORKSHOP_MANAGER。
 */

export interface WageWorkRecord {
  id: number
  /** 来源生产报工主键 */
  sourceReportId: number
  employeeId: number
  /** 作业日期 yyyy-MM-dd */
  workDate: string
  workOrderId: number
  processId: number
  productId: number
  qualifiedQuantity: number
  defectQuantity: number
  sourceAuditTime: string
  createTime: string
}

export interface WageWorkRecordPageParams {
  employeeId?: number
  workOrderId?: number
  processId?: number
  productId?: number
  workDateBegin?: string
  workDateEnd?: string
}

export function getWorkRecordPage(
  params: WageWorkRecordPageParams & PageParam,
): Promise<PageResult<WageWorkRecord>> {
  return get('/wage/work_records/page', params)
}
