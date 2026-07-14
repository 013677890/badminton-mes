package com.badminton.mes.module.craft.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 工序档案响应 VO。
 *
 * <p>关键工序按需求强制记录报工和人员信息，因此响应中显式返回
 * reportRequired 与 personnelRequired，两者都由 keyProcess 派生。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftProcessRespVO {

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

    /** 关键工序是否强制报工 */
    private Boolean reportRequired;

    /** 关键工序是否强制记录人员 */
    private Boolean personnelRequired;

    /** 是否需要质检 */
    private Boolean qualityRequired;

    /** 是否需要扫码 */
    private Boolean scanRequired;

    /** 是否参与计件 */
    private Boolean pieceRateEnabled;

    /** 适用设备类别 id */
    private Long equipmentCategoryId;

    /** 检验方案 id */
    private Long qualityPlanId;

    /** 备注 */
    private String remark;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 乐观锁版本 */
    private Integer version;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
