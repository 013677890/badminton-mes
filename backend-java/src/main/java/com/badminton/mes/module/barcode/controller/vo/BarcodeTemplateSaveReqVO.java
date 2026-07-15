package com.badminton.mes.module.barcode.controller.vo;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码模板创建/修改请求 VO，携带完整字段配置。
 *
 * <p>版本由系统管理不接收提交；修改时模板编码被忽略(编码是版本族标识，
 * 与生产工单修改忽略工单号同一处理方式)。"必须包含条码或二维码字段"
 * 由 Service 校验。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeTemplateSaveReqVO {

    /** 模板编码，同编码多版本共用；仅创建时生效，修改时忽略 */
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 32, message = "模板编码长度不能超过 32")
    private String templateCode;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 64, message = "模板名称长度不能超过 64")
    private String templateName;

    /** 纸张宽度(mm) */
    @NotNull(message = "纸张宽度不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "纸张宽度必须大于 0")
    @Digits(integer = 4, fraction = 2, message = "纸张宽度整数最多 4 位、小数最多 2 位")
    private BigDecimal paperWidth;

    /** 纸张高度(mm) */
    @NotNull(message = "纸张高度不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "纸张高度必须大于 0")
    @Digits(integer = 4, fraction = 2, message = "纸张高度整数最多 4 位、小数最多 2 位")
    private BigDecimal paperHeight;

    /** 字段配置，必须包含至少一个条码或二维码字段 */
    @NotEmpty(message = "模板字段不能为空")
    @Valid
    private List<BarcodeTemplateFieldSaveReqVO> fields;
}
