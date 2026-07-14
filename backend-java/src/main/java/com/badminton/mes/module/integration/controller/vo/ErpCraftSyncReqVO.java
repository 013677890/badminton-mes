package com.badminton.mes.module.integration.controller.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ERP 工艺数据同步触发请求。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class ErpCraftSyncReqVO {

    /** 来源系统，默认 ERP */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;
}
