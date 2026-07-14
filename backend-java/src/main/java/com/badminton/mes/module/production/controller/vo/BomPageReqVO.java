package com.badminton.mes.module.production.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** BOM 分页查询请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BomPageReqVO extends PageParam {
    /** BOM 编码前缀 */
    @Size(max = 32, message = "BOM 编码长度不能超过 32")
    private String bomCode;
    /** 产品主键 */
    @Positive(message = "产品 id 必须为正数")
    private Long productId;
    /** BOM 业务版本 */
    @Size(max = 16, message = "BOM 版本长度不能超过 16")
    private String version;
    /** BOM 状态 */
    @Min(value = 0, message = "BOM 状态不合法")
    @Max(value = 2, message = "BOM 状态不合法")
    private Integer bomStatus;
}
