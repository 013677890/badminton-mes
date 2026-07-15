package com.badminton.mes.module.barcode.controller.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * 条码模板预览响应 VO：标签布局与逐字段展示内容，第一阶段仅返回
 * 预览数据结构，不驱动打印设备(已冻结决策)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeTemplatePreviewRespVO {

    /** 模板主键(具体版本行) */
    private Long templateId;

    /** 模板编码 */
    private String templateCode;

    /** 模板版本 */
    private String version;

    /** 纸张宽度(mm) */
    private BigDecimal paperWidth;

    /** 纸张高度(mm) */
    private BigDecimal paperHeight;

    /** 逐字段预览内容 */
    private List<Field> fields;

    /**
     * 预览字段。
     *
     * @author 刘涵
     * @date 2026/07/12
     */
    @Data
    public static class Field {

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

        /** 按样例数据解析的展示内容，缺样例时为 null */
        private String sampleContent;
    }
}
