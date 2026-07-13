package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 物料主档响应。 */
@Data
public class MaterialRespVO {
    /** 物料主键 */
    private Long id;
    /** 物料编码 */
    private String materialCode;
    /** 物料名称 */
    private String materialName;
    /** 规格型号 */
    private String spec;
    /** 物料类型 */
    private Integer materialType;
    /** 计量单位主键 */
    private Long unitId;
    /** 是否关键物料 */
    private Boolean keyMaterial;
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
