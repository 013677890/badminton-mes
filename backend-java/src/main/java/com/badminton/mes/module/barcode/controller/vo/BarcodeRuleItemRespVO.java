package com.badminton.mes.module.barcode.controller.vo;

import lombok.Data;

/**
 * 条码规则组成项响应 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeRuleItemRespVO {

    /** 主键 */
    private Long id;

    /** 组成顺序 */
    private Integer seq;

    /** 组成类型：1 常量 2 日期 3 变量 4 流水号 */
    private Integer itemType;

    /** 常量值或变量名 */
    private String itemValue;

    /** 日期格式 */
    private String dateFormat;

    /** 该段长度 */
    private Integer itemLength;
}
