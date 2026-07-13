package com.badminton.mes.module.production.dal.entity;

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
 * BOM 主表实体，对应表 base_bom。
 *
 * <p>BOM 维护属工艺/基础资料范围，本实体仅映射工单下达校验需要读取的列。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "base_bom")
public class BomEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** BOM 编码 */
    @Column(name = "bom_code")
    private String bomCode;

    /** 产品 id */
    @Column(name = "product_id")
    private Long productId;

    /** BOM 版本 */
    @Column(name = "version")
    private String version;

    /** 状态：0 草稿 1 生效 2 停用 */
    @Column(name = "bom_status")
    private Integer bomStatus;

    /** 创建人 */
    @Column(name = "create_by")
    private Long createBy;

    /** 最后修改人 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 乐观锁版本，与 BOM 业务版本分离 */
    @Version
    @Column(name = "lock_version")
    private Integer lockVersion;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
