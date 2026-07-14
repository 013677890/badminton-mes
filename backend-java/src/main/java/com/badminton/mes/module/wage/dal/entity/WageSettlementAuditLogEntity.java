package com.badminton.mes.module.wage.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** 计件工资结算审计日志实体。 */
@Data
@Entity
@DynamicInsert
@Table(name = "wage_settlement_audit_log")
public class WageSettlementAuditLogEntity {
    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 结算主键 */
    @Column(name = "settlement_id")
    private Long settlementId;
    /** 明细主键 */
    @Column(name = "detail_id")
    private Long detailId;
    /** 动作类型 */
    @Column(name = "action_type")
    private String actionType;
    /** 原状态 */
    @Column(name = "from_status")
    private Integer fromStatus;
    /** 目标状态 */
    @Column(name = "to_status")
    private Integer toStatus;
    /** 调整前金额 */
    @Column(name = "before_amount_basis")
    private Long beforeAmountBasis;
    /** 调整后金额 */
    @Column(name = "after_amount_basis")
    private Long afterAmountBasis;
    /** 操作原因 */
    @Column(name = "action_reason")
    private String actionReason;
    /** 操作人 */
    @Column(name = "operate_by")
    private Long operateBy;
    /** 操作时间 */
    @Column(name = "operate_time", insertable = false, updatable = false)
    private LocalDateTime operateTime;
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
