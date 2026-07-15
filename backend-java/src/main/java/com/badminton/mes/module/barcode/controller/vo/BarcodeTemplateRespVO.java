package com.badminton.mes.module.barcode.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 条码模板响应 VO，详情含字段配置。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeTemplateRespVO {

    /** 主键(具体版本行) */
    private Long id;

    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 纸张宽度(mm) */
    private BigDecimal paperWidth;

    /** 纸张高度(mm) */
    private BigDecimal paperHeight;

    /** 模板版本 */
    private String version;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 字段配置；分页列表不返回，详情返回 */
    private List<BarcodeTemplateFieldRespVO> fields;

    /** 创建时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
