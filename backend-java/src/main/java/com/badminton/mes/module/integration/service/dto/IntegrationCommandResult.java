package com.badminton.mes.module.integration.service.dto;

/**
 * 外部写入命令结果。
 *
 * @param businessId  MES 业务主键
 * @param businessNo  MES 业务编号
 * @param duplicate   是否为重复请求
 * @param logId       写入日志主键
 * @author 张竹灏
 * @date 2026/07/11
 */
public record IntegrationCommandResult(Long businessId, String businessNo,
                                       boolean duplicate, Long logId) {
}
