package com.badminton.mes.module.barcode.controller.vo;

import java.util.List;

import lombok.Data;

/**
 * 条码规则校验响应 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeRuleValidateRespVO {

    /** 配置是否合法 */
    private Boolean valid;

    /** 逐条错误说明，合法时为空集合(API-002) */
    private List<String> errors;
}
