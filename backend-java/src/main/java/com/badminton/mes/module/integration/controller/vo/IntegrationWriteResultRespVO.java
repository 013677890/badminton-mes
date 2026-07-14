package com.badminton.mes.module.integration.controller.vo;

import lombok.Data;

/**
 * 外部接口写入结果。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
public class IntegrationWriteResultRespVO {

    /** 写入日志主键 */
    private Long logId;

    /** 处理状态：SUCCESS / FAILED / DUPLICATE */
    private String status;

    /** MES 业务主键 */
    private Long businessId;

    /** MES 业务编号 */
    private String businessNo;

    /** 失败错误码 */
    private String errorCode;

    /** 处理说明 */
    private String message;
}
