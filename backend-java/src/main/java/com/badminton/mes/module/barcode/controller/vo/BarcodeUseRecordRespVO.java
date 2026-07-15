package com.badminton.mes.module.barcode.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 条码使用记录响应 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeUseRecordRespVO {

    /** 主键 */
    private Long id;

    /** 条码 id */
    private Long barcodeId;

    /** 生产任务单 id */
    private Long taskId;

    /** 工序 id */
    private Long processId;

    /** 扫码人员用户 id */
    private Long userId;

    /** 设备 id */
    private Long equipmentId;

    /** 使用类型：1 工序开工扫码 2 工序完工扫码 3 报工扫码 4 其他 */
    private Integer useType;

    /** 业务发生时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime businessTime;

    /** 记录时间，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
