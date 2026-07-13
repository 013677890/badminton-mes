package com.badminton.mes.module.craft.controller.vo;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工序档案分页查询请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CraftProcessPageReqVO extends PageParam {

    /** 工序编码前缀 */
    @Size(max = 32, message = "工序编码长度不能超过 32")
    private String processCode;

    /** 工序名称前缀 */
    @Size(max = 64, message = "工序名称长度不能超过 64")
    private String processName;

    /** 工序类型编码 */
    @Size(max = 32, message = "工序类型长度不能超过 32")
    private String processType;

    /** 是否关键工序 */
    private Boolean keyProcess;

    /** 是否需要质检 */
    private Boolean qualityRequired;

    /** 是否需要扫码 */
    private Boolean scanRequired;

    /** 是否参与计件 */
    private Boolean pieceRateEnabled;

    /** 适用设备类别 id */
    @Positive(message = "设备类别 id 必须为正数")
    private Long equipmentCategoryId;

    /** 状态：1 启用 0 停用 */
    @Min(value = 0, message = "状态取值为 0 或 1")
    @Max(value = 1, message = "状态取值为 0 或 1")
    private Integer status;
}
