import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelWorkOrder,
  closeWorkOrder,
  deleteWorkOrder,
  finishWorkOrder,
  pauseWorkOrder,
  releaseWorkOrder,
  resumeWorkOrder,
} from '@/api/production/workOrder'

/**
 * 工单状态流转操作复用（列表行操作与详情页按钮共用）。
 * 确认框/原因输入统一在这里，调用方只提供完成后的刷新回调。
 * 后端为最终防线：状态不允许时返回业务错误，由拦截器提示。
 */
export function useWorkOrderActions(onDone: () => void | Promise<void>) {
  async function confirmThen(message: string, action: () => Promise<void>, successTip: string) {
    try {
      await ElMessageBox.confirm(message, '操作确认', { type: 'warning' })
    } catch {
      return
    }
    try {
      await action()
      ElMessage.success(successTip)
    } catch {
      // 失败提示由 request 拦截器弹出
    } finally {
      await onDone()
    }
  }

  async function promptReasonThen(
    title: string,
    action: (reason: string) => Promise<void>,
    successTip: string,
  ) {
    let reason: string
    try {
      const result = await ElMessageBox.prompt('请填写操作原因（必填）', title, {
        inputPattern: /\S+/,
        inputErrorMessage: '原因不能为空',
        inputPlaceholder: '不超过 255 字',
        type: 'warning',
      })
      reason = result.value.trim()
    } catch {
      return
    }
    try {
      await action(reason)
      ElMessage.success(successTip)
    } catch {
      // 同上
    } finally {
      await onDone()
    }
  }

  return {
    /** 已创建 → 已下达（按 BOM 生成物料需求） */
    release: (id: number) =>
      confirmThen('下达后将按生效 BOM 生成物料需求，且工单不可再编辑，确认下达？', () => releaseWorkOrder(id), '工单已下达'),
    /** 已下达/生产中 → 暂停 */
    pause: (id: number) => promptReasonThen('暂停工单', (reason) => pauseWorkOrder(id, reason), '工单已暂停'),
    /** 暂停 → 暂停前状态 */
    resume: (id: number) => confirmThen('确认恢复该工单？', () => resumeWorkOrder(id), '工单已恢复'),
    /** 已下达/生产中 → 已完工 */
    finish: (id: number) => confirmThen('确认完工该工单？', () => finishWorkOrder(id), '工单已完工'),
    /** 已完工 → 已关闭 */
    close: (id: number) => confirmThen('关闭后工单进入终态，确认关闭？', () => closeWorkOrder(id), '工单已关闭'),
    /** 已创建/已下达 → 已作废 */
    cancel: (id: number) => promptReasonThen('作废工单', (reason) => cancelWorkOrder(id, reason), '工单已作废'),
    /** 仅已创建可删（逻辑删除） */
    remove: (id: number) =>
      confirmThen('删除后列表不再展示该工单，确认删除？', () => deleteWorkOrder(id), '工单已删除'),
  }
}
