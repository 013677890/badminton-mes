package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备故障原理响应 VO，详情与分页列表共用。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
public class EquipmentFaultPrincipleRespVO {

    /** 故障原理主键 */
    private Long id;

    /** 故障编码 */
    private String faultCode;

    /** 故障名称 */
    private String faultName;

    /** 适用设备类别 id */
    private Long categoryId;

    /** 故障等级 */
    private String faultLevel;

    /** 故障描述 */
    private String faultDescription;

    /** 建议处理方案 */
    private String suggestedSolution;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注说明 */
    private String remark;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
