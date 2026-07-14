package com.badminton.mes.module.craft.controller.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 工艺路线步骤保存请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftRouteStepSaveReqVO {

    /** 连续工序顺序号，从 1 开始 */
    @NotNull(message = "工序顺序不能为空")
    @Positive(message = "工序顺序必须大于 0")
    private Integer sequenceNo;

    /** 工序主键 */
    @NotNull(message = "工序不能为空")
    @Positive(message = "工序 id 必须为正数")
    private Long processId;

    /** 默认工位主键 */
    @Positive(message = "工位 id 必须为正数")
    private Long stationId;

    /** 设备类别要求；为空时继承工序设备类别 */
    @Positive(message = "设备类别 id 必须为正数")
    private Long equipmentCategoryId;

    /** 是否质检节点 */
    @NotNull(message = "请设置是否质检节点")
    private Boolean inspectNode;

    /** 工序 SOP 关联主键 */
    @Positive(message = "SOP id 必须为正数")
    private Long sopId;

    /** 检验方案主键 */
    @Positive(message = "检验方案 id 必须为正数")
    private Long qualityPlanId;
}
