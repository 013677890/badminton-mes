package com.badminton.mes.module.integration.service.dto;

import com.badminton.mes.module.integration.dal.entity.ErpCraftPendingEntity;

/**
 * ERP 工艺同步单条处理结果。
 *
 * @param pending      待确认数据实体（重复时为 null）
 * @param duplicate    是否为重复请求
 * @param logId        同步日志主键
 * @param errorCode    异常错误码（校验失败时）
 * @param errorMessage 异常原因（校验失败时）
 * @author 张竹灏
 * @date 2026/07/13
 */
public record ErpCraftSyncResult(ErpCraftPendingEntity pending,
                                 boolean duplicate,
                                 Long logId,
                                 String errorCode,
                                 String errorMessage) {
}
