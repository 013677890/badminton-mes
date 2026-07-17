package com.badminton.mes.module.integration.dal.entity;

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
 * 计量单位实体，对应 base_unit。
 *
 * <p>单位编码是外部 upsert 的业务键，名称和小数精度被产品与工单引用。精度一旦被产品使用，
 * 写入服务禁止随意修改，以免历史数量的展示和换算规则发生变化；版本字段负责补充乐观并发控制。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "base_unit")
public class UnitEntity {

    /** 单位自增主键，作为产品和工单关联的稳定标识。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 规范化后的单位业务编码，受有效数据唯一约束保护。 */
    @Column(name = "unit_code")
    private String unitCode;

    /** 面向业务人员展示的单位名称。 */
    @Column(name = "unit_name")
    private String unitName;

    /** 数量允许的小数位数，已被产品引用时不能直接改变。 */
    @Column(name = "decimal_precision")
    private Integer decimalPrecision;

    /** 状态：1 启用、0 停用；生产业务引用前必须确认启用。 */
    @Column(name = "status")
    private Integer status;

    /** JPA 乐观锁版本，用于检测非锁定更新的并发覆盖。 */
    @Version
    @Column(name = "version")
    private Integer version;

    /** 单位主档创建人系统用户主键。 */
    @Column(name = "create_by")
    private Long createBy;

    /** 单位主档最后修改人系统用户主键。 */
    @Column(name = "update_by")
    private Long updateBy;

    /** 单位主档创建时间，由数据库维护。 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 单位主档最后更新时间，由数据库维护。 */
    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，正常单位查询和编码校验均排除已删除数据。 */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
