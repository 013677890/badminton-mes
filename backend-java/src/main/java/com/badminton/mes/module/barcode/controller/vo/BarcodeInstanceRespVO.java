package com.badminton.mes.module.barcode.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 条码实例响应 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeInstanceRespVO {

    /** 主键 */
    private Long id;

    /** 条码值 */
    private String barcodeValue;

    /** 条码类型 id */
    private Long barcodeTypeId;

    /** 条码模式：1 唯一码 2 批次码 */
    private Integer barcodeMode;

    /** 来源应用规则 id */
    private Long applyRuleId;

    /** 产品 id */
    private Long productId;

    /** 物料 id(材料码) */
    private Long materialId;

    /** 批次号 */
    private String batchNo;

    /** 关联生产工单 id */
    private Long workOrderId;

    /** 关联生产任务单 id */
    private Long taskId;

    /** 来源：1 规则生成 2 传入值 3 外部导入 */
    private Integer sourceType;

    /** 状态：0 未使用 1 已使用 2 已作废 */
    private Integer barcodeStatus;

    /** 创建人用户 id */
    private Long createBy;

    /** 创建时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
