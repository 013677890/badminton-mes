package com.badminton.mes.module.integration.controller.vo;

import lombok.Data;

/**
 * 外部接口写入结果。
 *
 * <p>成功和重复结果携带已有 MES 业务定位；失败结果携带稳定错误码和说明，日志主键始终返回，
 * 便于调用方继续查询请求快照、处理状态及重试记录。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
public class IntegrationWriteResultRespVO {

    /** 对应接口审计日志主键。 */
    private Long logId;

    /** 处理状态：SUCCESS、FAILED 或 DUPLICATE。 */
    private String status;

    /** 成功或重复时对应的 MES 业务主键，失败且未创建业务数据时为空。 */
    private Long businessId;

    /** 成功或重复时对应的 MES 业务编号。 */
    private String businessNo;

    /** 失败时的稳定业务错误码。 */
    private String errorCode;

    /** 成功提示、重复说明或失败原因。 */
    private String message;
}
