package com.badminton.mes.module.craft.controller.vo;

import lombok.Data;

/**
 * 工艺路线步骤响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftRouteStepRespVO {

    /** 明细主键 */
    private Long id;

    /** 工序顺序 */
    private Integer sequenceNo;

    /** 工序主键 */
    private Long processId;

    /** 工序编码 */
    private String processCode;

    /** 工序名称 */
    private String processName;

    /** 默认工位主键 */
    private Long stationId;

    /** 设备类别要求 */
    private Long equipmentCategoryId;

    /** 是否关键工序 */
    private Boolean keyProcess;

    /** 是否质检节点 */
    private Boolean inspectNode;

    /** 是否需要扫码 */
    private Boolean scanRequired;

    /** 是否参与计件 */
    private Boolean pieceRateEnabled;

    /** SOP 关联主键 */
    private Long sopId;

    /** 检验方案主键 */
    private Long qualityPlanId;
}
