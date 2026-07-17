package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码解析请求 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeParseReqVO {

    /** 待解析的条码值 */
    @NotBlank(message = "条码值不能为空")
    @Size(max = 64, message = "条码值长度不能超过 64")
    private String barcodeValue;
}
