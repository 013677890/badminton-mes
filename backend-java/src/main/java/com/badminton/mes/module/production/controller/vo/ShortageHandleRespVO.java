package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 欠料处理记录响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class ShortageHandleRespVO {

    /** 主键 */
    private Long id;

    /** 生产工单 id */
    private Long workOrderId;

    /** 欠料物料 id */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 处理方式：1 催采购 2 调拨 3 代用料 4 调整排产 */
    private Integer handleType;

    /** 责任人 id */
    private Long handlerId;

    /** 预计到料日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedArrivalDate;

    /** 处理说明 */
    private String handleRemark;

    /** 处理状态：0 处理中 1 已解决 */
    private Integer handleStatus;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
