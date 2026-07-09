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
 * 系统用户实体，对应表 sys_user。
 *
 * <p>实体只承载数据库状态，不建立 JPA 级联关系，不调用 Service 或 Redis。
 * password 字段仅存 BCrypt 哈希，任何查询接口不得对外返回。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@Entity
@DynamicInsert
@Table(name = "sys_user")
public class UserEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工号 */
    @Column(name = "user_no")
    private String userNo;

    /** 姓名 */
    @Column(name = "user_name")
    private String userName;

    /** 密码(BCrypt 哈希) */
    @Column(name = "password")
    private String password;

    /** 手机号 */
    @Column(name = "mobile")
    private String mobile;

    /** 所属车间 id */
    @Column(name = "workshop_id")
    private Long workshopId;

    /** 所属产线 id */
    @Column(name = "line_id")
    private Long lineId;

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
