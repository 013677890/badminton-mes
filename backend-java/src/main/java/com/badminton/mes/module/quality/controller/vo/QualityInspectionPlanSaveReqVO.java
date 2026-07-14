package com.badminton.mes.module.quality.controller.vo;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 检验标准方案草稿创建/修改请求。 */
@Data
public class QualityInspectionPlanSaveReqVO {

    @NotBlank(message = "方案编码不能为空")
    @Size(max = 32, message = "方案编码长度不能超过 32")
    private String planCode;

    @NotBlank(message = "方案名称不能为空")
    @Size(max = 128, message = "方案名称长度不能超过 128")
    private String planName;

    @Positive(message = "适用产品必须为正整数")
    private Long productId;

    @Positive(message = "适用客户必须为正整数")
    private Long customerId;

    @NotBlank(message = "检验类型不能为空")
    @Pattern(regexp = "^(FIRST_ARTICLE|LAST_ARTICLE|PATROL|WAREHOUSE_IN|SHIPMENT)$",
             message = "检验类型不合法")
    private String inspectionType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    private Boolean defaultFlag;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    @Valid
    @NotEmpty(message = "检验方案至少包含一个检验项目")
    @Size(max = 100, message = "单个方案最多包含 100 个检验项目")
    private List<QualityInspectionPlanItemSaveReqVO> items;
}
