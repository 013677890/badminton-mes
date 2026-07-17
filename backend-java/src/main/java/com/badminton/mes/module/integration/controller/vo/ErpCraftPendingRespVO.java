package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * ERP 工艺待确认数据响应。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class ErpCraftPendingRespVO {

    /** ERP 工艺暂存记录主键。 */
    private Long id;

    /** ERP 来源系统。 */
    private String sourceSystem;

    /** ERP 工艺路线编码。 */
    private String erpRoutingCode;

    /** ERP 工艺路线名称 */
    private String erpRoutingName;

    /** ERP 工艺路线版本。 */
    private String erpRoutingVersion;

    /** 适用产品编码。 */
    private String productCode;

    /** 处理状态：0 待确认、1 已确认、2 异常或其他人工处理状态。 */
    private Integer status;

    /** 确认后生成的 MES 工艺路线主键，未确认时为空。 */
    private Long confirmedRouteId;

    /** 校验失败或驳回的稳定业务错误码。 */
    private String errorCode;

    /** 校验失败或驳回的处理原因。 */
    private String errorMessage;

    /** 暂存记录创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
