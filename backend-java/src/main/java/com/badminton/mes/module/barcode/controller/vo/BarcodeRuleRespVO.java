package com.badminton.mes.module.barcode.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 条码规则响应 VO，详情含组成明细。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeRuleRespVO {

    /** 主键 */
    private Long id;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 适用条码类型 id */
    private Long barcodeTypeId;

    /** 流水号位数 */
    private Integer serialLength;

    /** 流水号重置周期：1 按日 2 按月 3 不重置 */
    private Integer serialResetCycle;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 组成明细，按 seq 升序；分页列表不返回，详情返回 */
    private List<BarcodeRuleItemRespVO> items;

    /** 创建时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
