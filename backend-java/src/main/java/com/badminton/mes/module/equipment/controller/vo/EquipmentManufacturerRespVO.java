package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 设备制造商响应 VO，制造商详情与分页列表共用。
 *
 * <p>返回制造商身份、联系方式和启停状态等业务展示字段，不暴露逻辑删除标记等持久化控制字段；
 * 时间字段统一序列化到秒，供详情页与分页列表直接复用。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Data
public class EquipmentManufacturerRespVO {

    /** 设备制造商主键。 */
    private Long id;

    /** 业务唯一的制造商编码。 */
    private String manufacturerCode;

    /** 制造商正式名称。 */
    private String manufacturerName;

    /** 制造商业务联系人。 */
    private String contactPerson;

    /** 联系电话，未维护时为空。 */
    private String contactPhone;

    /** 联系邮箱，未维护时为空。 */
    private String contactEmail;

    /** 办公地址或通信地址。 */
    private String address;

    /** 制造商官方网站地址。 */
    private String website;

    /** 合作关系、供货范围等补充说明。 */
    private String remark;

    /** 启停状态：1 表示启用，0 表示停用。 */
    private Integer status;

    /** 制造商记录创建时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 制造商记录最后更新时间，由数据库维护。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
