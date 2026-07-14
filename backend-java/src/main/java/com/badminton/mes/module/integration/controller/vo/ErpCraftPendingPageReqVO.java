package com.badminton.mes.module.integration.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ERP 工艺待确认分页查询请求。
 *
 * @author Codex
 * @date 2026/07/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpCraftPendingPageReqVO extends PageParam {

    private Integer status;

    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;

    @Size(max = 32, message = "路线编码长度不能超过 32")
    private String erpRoutingCode;
}
