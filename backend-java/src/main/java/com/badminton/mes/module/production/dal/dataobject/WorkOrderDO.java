package com.badminton.mes.module.production.dal.dataobject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 生产工单数据对象，对应表 prod_work_order。
 *
 * <p>只承载数据库状态，不放业务逻辑；字段与列的映射关系统一在
 * WorkOrderMapper.xml 的 resultMap 中维护(ORM-003)。
 * 布尔属性不加 is 前缀，由 resultMap 完成 is_deleted 到 deleted 的映射(ORM-002)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
public class WorkOrderDO {

    /** 主键 */
    private Long id;

    /** 工单号(唯一；日期+流水，未传时由系统生成) */
    private String workOrderNo;

    /** 来源：1 手工 2 导入 3 ERP同步 4 API写入，见 WorkOrderSourceTypeEnum */
    private Integer sourceType;

    /** 外部来源单号(ERP/API；来源内唯一)，手工单为 null */
    private String sourceOrderNo;

    /** 产品 id */
    private Long productId;

    /** 产品名称(冗余自 base_product，避免列表联查) */
    private String productName;

    /** 规格型号(冗余自 base_product) */
    private String spec;

    /** 计量单位 id(冗余自 base_product) */
    private Long unitId;

    /** 生产批次号 */
    private String batchNo;

    /** BOM 版本 id，下达前必须维护 */
    private Long bomId;

    /** 工艺路线 id，下达前必须维护 */
    private Long routingId;

    /** 客户 id */
    private Long customerId;

    /** 目标车间 id */
    private Long workshopId;

    /** 计划数量 */
    private Integer planQuantity;

    /** 已派工数量 */
    private Integer dispatchedQuantity;

    /** 投入数量(报工汇总冗余) */
    private Integer inputQuantity;

    /** 完工数量(报工汇总冗余) */
    private Integer finishQuantity;

    /** 不良数量(报工汇总冗余) */
    private Integer defectQuantity;

    /** 返修数量(报工汇总冗余) */
    private Integer reworkQuantity;

    /** 允许超产比例(%)，精确小数使用 BigDecimal */
    private BigDecimal overRatio;

    /** 优先级：1 最高 - 9 最低 */
    private Integer priority;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划完成时间(交期) */
    private LocalDateTime planEndTime;

    /** 工单状态，见 WorkOrderStatusEnum */
    private Integer orderStatus;

    /** 齐套状态：0 未分析 1 齐套 2 部分齐套 3 欠料(齐套分析回写冗余) */
    private Integer kitStatus;

    /** 创建人用户 id */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记：true 已删除 false 未删除，映射列 is_deleted(TABLE-010) */
    private Boolean deleted;
}
