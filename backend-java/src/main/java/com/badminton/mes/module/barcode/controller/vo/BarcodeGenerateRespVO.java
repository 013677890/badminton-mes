package com.badminton.mes.module.barcode.controller.vo;

import lombok.Data;

/**
 * 批次码生成响应 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeGenerateRespVO {

    /** 条码主键 */
    private Long id;

    /** 生成的条码值 */
    private String barcodeValue;

    /** 条码类型 id */
    private Long barcodeTypeId;

    /** 条码模式：1 唯一码 2 批次码 */
    private Integer barcodeMode;

    /** 批次号 */
    private String batchNo;

    /** 来源：1 规则生成 2 传入值 3 外部导入 */
    private Integer sourceType;

    /** 状态：0 未使用 1 已使用 2 已作废 */
    private Integer barcodeStatus;
}
