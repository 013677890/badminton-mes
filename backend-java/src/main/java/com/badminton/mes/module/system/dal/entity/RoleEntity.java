package com.badminton.mes.module.system.dal.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 系统角色实体，对应表 sys_role。
 *
 * <p>六个内置角色由迁移种子数据固化，应用不提供角色增删改。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "sys_role")
public class RoleEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色编码 */
    @Column(name = "role_code")
    private String roleCode;

    /** 角色名称 */
    @Column(name = "role_name")
    private String roleName;

    /** 备注 */
    @Column(name = "remark")
    private String remark;

    /** 状态：1 启用 0 停用 */
    @Column(name = "status")
    private Integer status;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted")
    private Boolean deleted;
}
