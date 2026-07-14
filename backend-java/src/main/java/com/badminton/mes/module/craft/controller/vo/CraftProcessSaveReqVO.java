package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工序档案创建/修改请求 VO。
 *
 * <p>工序编码限定为 ASCII 字母、数字、下划线和连字符，Service 统一转大写；
 * “需要质检时必须有检验方案”属于跨字段规则，由 Service 校验。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftProcessSaveReqVO {

    /** 工序编码，唯一 */
    @NotBlank(message = "工序编码不能为空")
    @Size(max = 32, message = "工序编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "工序编码只能包含字母、数字、下划线和连字符")
    private String processCode;

    /** 工序名称 */
    @NotBlank(message = "工序名称不能为空")
    @Size(max = 64, message = "工序名称长度不能超过 64")
    private String processName;

    /** 工序类型编码，例如 PREPARATION、PROCESSING、INSPECTION */
    @NotBlank(message = "工序类型不能为空")
    @Size(max = 32, message = "工序类型长度不能超过 32")
    private String processType;

    /** 标准工时，单位秒 */
    @NotNull(message = "标准工时不能为空")
    @Positive(message = "标准工时必须大于 0")
    @Max(value = 86400, message = "标准工时不能超过 86400 秒")
    private Integer standardTimeSeconds;

    /** 是否关键工序 */
    @NotNull(message = "请设置是否关键工序")
    private Boolean keyProcess;

    /** 是否需要质检 */
    @NotNull(message = "请设置是否需要质检")
    private Boolean qualityRequired;

    /** 是否需要扫码 */
    @NotNull(message = "请设置是否需要扫码")
    private Boolean scanRequired;

    /** 是否参与计件 */
    @NotNull(message = "请设置是否参与计件")
    private Boolean pieceRateEnabled;

    /** 适用设备类别 id */
    @Positive(message = "设备类别 id 必须为正数")
    private Long equipmentCategoryId;

    /** 检验方案 id，需要质检时必填 */
    @Positive(message = "检验方案 id 必须为正数")
    private Long qualityPlanId;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /** 变更原因，修改时记入变更日志 */
    @Size(max = 255, message = "变更原因长度不能超过 255")
    private String changeReason;
}
