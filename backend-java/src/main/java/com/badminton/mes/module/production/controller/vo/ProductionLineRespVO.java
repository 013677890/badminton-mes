package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 产线基础资料响应。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Data
public class ProductionLineRespVO {

    /** 产线主键 */
    private Long id;

    /** 产线编码 */
    private String lineCode;

    /** 产线名称 */
    private String lineName;

    /** 所属车间 id */
    private Long workshopId;

    /** 所属车间编码 */
    private String workshopCode;

    /** 所属车间名称 */
    private String workshopName;

    /** 标准日产能 */
    private Integer standardCapacity;

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
