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

    /** ERP 来源系统，未填写时门面使用 ERP 默认标识。 */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;
}
