package com.badminton.mes.module.wage.dal.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

/** 计件规则实体，对应 wage_piece_rate_rule。 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "wage_piece_rate_rule")
public class PieceRateRuleEntity {
    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 工序主键 */
    @Column(name = "process_id")
    private Long processId;
    /** 产品主键，null 表示通用规则 */
    @Column(name = "product_id")
    private Long productId;
    /** 计件单价，单位万分之一元 */
    @Column(name = "unit_price_basis")
    private Long unitPriceBasis;
    /** 不良扣减率基点，10000 表示 100% */
    @Column(name = "defect_deduction_rate")
    private Integer defectDeductionRate;
    /** 生效开始日期 */
    @Column(name = "effective_start")
    private LocalDate effectiveStart;
    /** 生效结束日期 */
    @Column(name = "effective_end")
    private LocalDate effectiveEnd;
    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
    private Integer status;
    /** 乐观锁版本 */
    @Version
    @Column(name = "version")
    private Integer version;
    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;
    /** 修改人 */
    @Column(name = "update_by")
    private Long updateBy;
    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
    /** 逻辑删除 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
