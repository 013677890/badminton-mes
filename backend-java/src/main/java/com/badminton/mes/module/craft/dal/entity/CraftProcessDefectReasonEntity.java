package com.badminton.mes.module.craft.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 工序可选不良原因实体，对应表 craft_process_defect_reason。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "craft_process_defect_reason")
public class CraftProcessDefectReasonEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工序 id */
    @Column(name = "process_id")
    private Long processId;

    /** 不良原因编码 */
    @Column(name = "reason_code")
    private String reasonCode;

    /** 不良原因名称 */
    @Column(name = "reason_name")
    private String reasonName;

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

    /** 最后修改人 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
