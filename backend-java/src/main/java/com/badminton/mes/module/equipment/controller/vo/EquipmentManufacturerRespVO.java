package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备制造商响应 VO，制造商详情与分页列表共用。
 *
 * <p>只暴露前端需要的展示字段，逻辑删除标记等内部字段不出参；
 * JSON key 均为 lowerCamelCase，时间统一 yyyy-MM-dd HH:mm:ss。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentManufacturerRespVO {

    /** 制造商主键 */
    private Long id;

    /** 制造商编码 */
    private String manufacturerCode;

    /** 制造商名称 */
    private String manufacturerName;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 联系邮箱 */
    private String contactEmail;

    /** 地址 */
    private String address;

    /** 官网 */
    private String website;

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
