package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备类别响应 VO，类别详情与分页列表共用。
 *
 * <p>返回类别标识、父子层级、排序和启停状态等业务展示字段，不暴露逻辑删除标记等持久化控制字段；
 * 时间字段统一序列化到秒，供详情页和分页列表直接复用。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentCategoryRespVO {

    /** 设备类别主键。 */
    private Long id;

    /** 业务唯一的类别编码。 */
    private String categoryCode;

    /** 面向业务人员展示的类别名称。 */
    private String categoryName;

    /** 父类别主键；顶级类别为空。 */
    private Long parentId;

    /** 同级类别排序号，数值越小展示越靠前。 */
    private Integer sortOrder;

    /** 类别用途或管理约定的补充说明。 */
    private String remark;

    /** 启停状态：1 表示启用，0 表示停用。 */
    private Integer status;

    /** 类别创建时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 类别最后更新时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
