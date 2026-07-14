package com.badminton.mes.module.craft.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工艺路线聚合创建请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftRouteSaveReqVO {

    /** 路线编码 */
    @NotBlank(message = "路线编码不能为空")
    @Size(max = 32, message = "路线编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "路线编码只能包含字母、数字、下划线和连字符")
    private String routingCode;

    /** 路线名称 */
    @NotBlank(message = "路线名称不能为空")
    @Size(max = 128, message = "路线名称长度不能超过 128")
    private String routingName;

    /** 业务版本 */
    @NotBlank(message = "路线版本不能为空")
    @Size(max = 32, message = "路线版本长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "路线版本只能包含字母、数字、点、下划线和连字符")
    private String routingVersion;

    /** 来源：1 本地创建 2 ERP 读取确认 */
    @NotNull(message = "路线来源不能为空")
    @Min(value = 1, message = "路线来源取值为 1 或 2")
    @Max(value = 2, message = "路线来源取值为 1 或 2")
    private Integer sourceType;

    /** 适用产品主键列表 */
    @NotEmpty(message = "路线至少绑定一个产品")
    @Size(max = 100, message = "单条路线最多绑定 100 个产品")
    private List<@NotNull(message = "产品 id 不能为空") @Positive(message = "产品 id 必须为正数") Long> productIds;

    /** 路线步骤，按 sequenceNo 排序 */
    @NotEmpty(message = "路线至少包含一个工序")
    @Size(max = 100, message = "单条路线最多包含 100 个工序")
    private List<@NotNull(message = "路线步骤不能为空") @Valid CraftRouteStepSaveReqVO> steps;

    /** 变更原因 */
    @Size(max = 255, message = "变更原因长度不能超过 255")
    private String changeReason;
}
