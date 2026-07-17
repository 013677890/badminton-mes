package com.badminton.mes.module.integration.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ERP 工艺待确认分页查询请求。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpCraftPendingPageReqVO extends PageParam {

    /** 暂存处理状态：待确认、已确认、异常或驳回等状态值。 */
    private Integer status;

    /** ERP 来源系统，按规范化编码精确筛选。 */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    private String sourceSystem;

    @Size(max = 32, message = "路线编码长度不能超过 32")
    /** ERP 路线编码，按去空白后的业务编码精确筛选。 */
    private String erpRoutingCode;
}
