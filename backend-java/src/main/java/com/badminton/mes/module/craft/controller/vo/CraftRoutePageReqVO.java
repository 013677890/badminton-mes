package com.badminton.mes.module.craft.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工艺路线分页请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CraftRoutePageReqVO extends PageParam {

    /** 路线编码前缀 */
    @Size(max = 32, message = "路线编码长度不能超过 32")
    private String routingCode;

    /** 路线名称前缀 */
    @Size(max = 128, message = "路线名称长度不能超过 128")
    private String routingName;

    /** 业务版本 */
    @Size(max = 32, message = "路线版本长度不能超过 32")
    private String routingVersion;

    /** 来源：1 本地创建 2 ERP 读取确认 */
    @Min(value = 1, message = "路线来源取值为 1 或 2")
    @Max(value = 2, message = "路线来源取值为 1 或 2")
    private Integer sourceType;

    /** 状态：0 草稿 1 生效 2 停用 */
    @Min(value = 0, message = "路线状态取值为 0、1 或 2")
    @Max(value = 2, message = "路线状态取值为 0、1 或 2")
    private Integer routingStatus;
}
