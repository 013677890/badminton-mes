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

    /** 主键 */
    private Long id;

    /** 来源系统 */
    private String sourceSystem;

    /** ERP 工艺路线编码 */
    private String erpRoutingCode;

    /** ERP 工艺路线名称 */
    private String erpRoutingName;

    /** ERP 工艺路线版本 */
    private String erpRoutingVersion;

    /** 产品编码 */
    private String productCode;

    /** 状态：0 待确认 1 已确认 2 异常 */
    private Integer status;

    /** 确认后生成的工艺路线主键 */
    private Long confirmedRouteId;

    /** 异常错误码 */
    private String errorCode;

    /** 异常原因 */
    private String errorMessage;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
