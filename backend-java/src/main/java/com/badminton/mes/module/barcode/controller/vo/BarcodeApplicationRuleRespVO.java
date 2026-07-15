package com.badminton.mes.module.barcode.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 条码应用规则响应 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeApplicationRuleRespVO {

    /** 主键 */
    private Long id;

    /** 对象类型：1 产品 2 物料 */
    private Integer objectType;

    /** 适用产品 id */
    private Long productId;

    /** 适用物料 id */
    private Long materialId;

    /** 条码类型 id */
    private Long barcodeTypeId;

    /** 条码模式：1 唯一码 2 批次码 */
    private Integer barcodeMode;

    /** 条码规则 id */
    private Long ruleId;

    /** 标签模板 id(具体版本行) */
    private Long templateId;

    /** 条码来源：1 规则生成 2 传入值生成 3 外部导入 */
    private Integer sourceType;

    /** 是否默认规则 */
    private Boolean defaultFlag;

    /** 规则版本 */
    private String version;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 创建时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
