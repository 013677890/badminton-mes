package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 产品主档响应。 */
@Data
public class ProductRespVO {
    /** 产品主键 */
    private Long id;
    /** 产品编码 */
    private String productCode;
    /** 产品名称 */
    private String productName;
    /** 规格型号 */
    private String spec;
    /** 产品类型 */
    private Integer productType;
    /** 产品等级 */
    private String grade;
    /** 计量单位主键 */
    private Long unitId;
    /** 启停状态 */
    private Integer status;
    /** 乐观锁版本 */
    private Integer version;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
