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
 * 用户角色关系实体，对应表 sys_user_role。
 *
 * <p>调整用户角色采用"逻辑删除旧关系 + 新增新关系"，保留授权轨迹。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "sys_user_role")
public class UserRoleEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 id */
    @Column(name = "user_id")
    private Long userId;

    /** 角色 id */
    @Column(name = "role_id")
    private Long roleId;

    /** 创建时间 */
    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", insertable = false)
    private LocalDateTime updateTime;

    /** 逻辑删除标记，映射列 is_deleted */
    @Column(name = "is_deleted", columnDefinition = "tinyint unsigned")
    private Boolean deleted;
}
