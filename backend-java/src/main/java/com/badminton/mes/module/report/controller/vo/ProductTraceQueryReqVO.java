package com.badminton.mes.module.report.controller.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 产品追溯入口条件，至少提供一个业务键。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Data
public class ProductTraceQueryReqVO {

    @Size(max = 64)
    private String batchCode;

    @Size(max = 128)
    private String barcodeValue;

    @Size(max = 32)
    private String workOrderNo;

    @Size(max = 32)
    private String taskNo;
}
