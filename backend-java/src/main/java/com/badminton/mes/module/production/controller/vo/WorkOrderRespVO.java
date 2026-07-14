package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 生产工单响应 VO，工单详情与分页列表共用。
 *
 * <p>只暴露前端需要的展示字段，逻辑删除标记等内部字段不出参；
 * JSON key 均为 lowerCamelCase(API-004)，时间统一 yyyy-MM-dd HH:mm:ss(API-013)。
 * 本表主键为自增序列，量级远小于 JS 安全整数上限，可直接返回 Long；
 * 若改用雪花等 16 位以上 id，必须改为 String 返回(API-006)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
public class WorkOrderRespVO {

    /** 工单主键 */
    private Long id;

    /** 工单号 */
    private String workOrderNo;

    /** 来源：1 手工 2 导入 3 ERP同步 4 API写入，见 WorkOrderSourceTypeEnum */
    private Integer sourceType;

    /** 外部来源系统，手工单为 null */
    private String sourceSystem;

    /** 外部来源单号，手工单为 null */
    private String sourceOrderNo;

    /** 产品 id */
    private Long productId;

    /** 产品名称 */
    private String productName;

    /** 规格型号 */
    private String spec;

    /** 计量单位 id */
    private Long unitId;

    /** 生产批次号 */
    private String batchNo;

    /** BOM 版本 id */
    private Long bomId;

    /** 工艺路线 id */
    private Long routingId;

    /** 客户 id */
    private Long customerId;

    /** 目标车间 id */
    private Long workshopId;

    /** 计划数量 */
    private Integer planQuantity;

    /** 已派工数量 */
    private Integer dispatchedQuantity;

    /** 投入数量 */
    private Integer inputQuantity;

    /** 完工数量 */
    private Integer finishQuantity;

    /** 不良数量 */
    private Integer defectQuantity;

    /** 返修数量 */
    private Integer reworkQuantity;

    /** 允许超产比例(%) */
    private BigDecimal overRatio;

    /** 优先级：1 最高 - 9 最低 */
    private Integer priority;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    /** 计划完成时间(交期) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;

    /** 工单状态，见 WorkOrderStatusEnum */
    private Integer orderStatus;

    /** 齐套状态：0 未分析 1 齐套 2 部分齐套 3 欠料 */
    private Integer kitStatus;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
