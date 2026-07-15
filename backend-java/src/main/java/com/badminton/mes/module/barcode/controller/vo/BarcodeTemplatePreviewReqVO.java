package com.badminton.mes.module.barcode.controller.vo;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码模板预览请求 VO：按模板字段配置生成打印预览数据，不驱动真实打印机。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeTemplatePreviewReqVO {

    /** 模板主键(具体版本行) */
    @NotNull(message = "模板不能为空")
    @Positive(message = "模板 id 必须为正数")
    private Long templateId;

    /** 样例条码值，条码/二维码字段优先取该值 */
    @Size(max = 64, message = "样例条码值长度不能超过 64")
    private String sampleBarcodeValue;

    /** 样例数据，key 为字段数据来源，value 为展示内容，可空 */
    private Map<String, String> sampleData;
}
