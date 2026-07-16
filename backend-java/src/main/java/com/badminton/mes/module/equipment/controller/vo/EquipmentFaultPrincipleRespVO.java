package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备故障原理响应 VO，详情与分页列表共用。
 *
 * <p>返回故障分类、严重等级、适用设备类别及建议方案等业务展示字段，不暴露逻辑删除标记等持久化
 * 控制字段；审计时间统一序列化到秒。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
public class EquipmentFaultPrincipleRespVO {

    /** 设备故障原理主键。 */
    private Long id;

    /** 业务唯一的故障编码。 */
    private String faultCode;

    /** 面向报修人员展示和选择的故障名称。 */
    private String faultName;

    /** 适用设备类别主键；为空表示通用故障原理。 */
    private Long categoryId;

    /** 故障严重等级：LOW、MEDIUM、HIGH 或 CRITICAL。 */
    private String faultLevel;

    /** 典型故障现象、成因或判定依据。 */
    private String faultDescription;

    /** 标准排查步骤或建议处理方案。 */
    private String suggestedSolution;

    /** 选择列表中的展示顺序，数值越小越靠前。 */
    private Integer sortOrder;

    /** 适用限制、维护来源等补充说明。 */
    private String remark;

    /** 启停状态：1 表示启用，0 表示停用。 */
    private Integer status;

    /** 故障原理创建时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 故障原理最后更新时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
