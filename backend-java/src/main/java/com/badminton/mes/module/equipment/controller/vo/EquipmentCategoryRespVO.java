package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备类别响应 VO，类别详情与分页列表共用。
 *
 * <p>只暴露前端需要的展示字段，逻辑删除标记等内部字段不出参；
 * JSON key 均为 lowerCamelCase，时间统一 yyyy-MM-dd HH:mm:ss。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentCategoryRespVO {

    /** 类别主键 */
    private Long id;

    /** 类别编码 */
    private String categoryCode;

    /** 类别名称 */
    private String categoryName;

    /** 父级类别 id */
    private Long parentId;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注说明 */
    private String remark;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
