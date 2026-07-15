package com.badminton.mes.module.barcode.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 条码解析响应 VO：条码事实 + 业务对象上下文，供现场扫码识别产品批次与任务。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeParseRespVO {

    /** 条码主键 */
    private Long id;

    /** 条码值 */
    private String barcodeValue;

    /** 条码类型 id */
    private Long barcodeTypeId;

    /** 条码类型编码 */
    private String barcodeTypeCode;

    /** 条码类型名称 */
    private String barcodeTypeName;

    /** 条码模式：1 唯一码 2 批次码 */
    private Integer barcodeMode;

    /** 批次号 */
    private String batchNo;

    /** 产品 id */
    private Long productId;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 物料 id(材料码) */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 关联生产工单 id */
    private Long workOrderId;

    /** 关联生产任务单 id */
    private Long taskId;

    /** 来源：1 规则生成 2 传入值 3 外部导入 */
    private Integer sourceType;

    /** 状态：0 未使用 1 已使用 2 已作废 */
    private Integer barcodeStatus;

    /** 生成时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
