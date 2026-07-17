package com.badminton.mes.module.barcode.controller.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 条码模板字段响应 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeTemplateFieldRespVO {

    /** 主键 */
    private Long id;

    /** 字段名称 */
    private String fieldName;

    /** 字段类型：1 文本 2 条码 3 二维码 */
    private Integer fieldType;

    /** 数据来源字段 */
    private String dataSource;

    /** X 位置(mm) */
    private BigDecimal posX;

    /** Y 位置(mm) */
    private BigDecimal posY;

    /** 字体大小 */
    private Integer fontSize;
}
