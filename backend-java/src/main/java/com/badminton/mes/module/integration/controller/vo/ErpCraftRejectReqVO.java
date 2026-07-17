package com.badminton.mes.module.integration.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ERP 工艺待确认驳回请求。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class ErpCraftRejectReqVO {

    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 512, message = "驳回原因长度不能超过 512")
    /** 工艺驳回原因，写入待确认记录错误说明并供后续审计。 */
    private String reason;
}
