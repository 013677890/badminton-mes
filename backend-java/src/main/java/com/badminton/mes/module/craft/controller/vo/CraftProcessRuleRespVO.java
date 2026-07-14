package com.badminton.mes.module.craft.controller.vo;

import lombok.Data;

/**
 * 工序规则批量查询响应 VO。
 *
 * <p>面向现场作业、质量和计件工资等读取场景，仅返回执行所需的稳定规则字段。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class CraftProcessRuleRespVO {

    /** 工序主键 */
    private Long id;

    /** 工序编码 */
    private String processCode;

    /** 工序名称 */
    private String processName;

    /** 工序类型编码 */
    private String processType;

    /** 标准工时，单位秒 */
    private Integer standardTimeSeconds;

    /** 是否关键工序 */
    private Boolean keyProcess;

    /** 是否强制报工 */
    private Boolean reportRequired;

    /** 是否强制记录作业人员 */
    private Boolean personnelRequired;

    /** 是否需要质检 */
    private Boolean qualityRequired;

    /** 是否需要扫码 */
    private Boolean scanRequired;

    /** 是否参与计件 */
    private Boolean pieceRateEnabled;

    /** 适用设备类别主键 */
    private Long equipmentCategoryId;

    /** 检验方案主键 */
    private Long qualityPlanId;
}
