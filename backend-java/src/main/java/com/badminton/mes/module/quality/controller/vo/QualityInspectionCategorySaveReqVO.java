package com.badminton.mes.module.quality.controller.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 检验分类创建或修改请求。
 *
 * <p>编码和名称为必填业务标识；启用状态只接受 0/1，保留前缀用于逻辑删除后的编码避让，客户端不得使用。
 */
@Data
public class QualityInspectionCategorySaveReqVO {

    /** 分类业务编码；不能为空、最长 32 个字符，且不得以系统保留的 {@code __DELETED_} 前缀开头。 */
    @NotBlank(message = "检验分类编码不能为空")
    @Size(max = 32, message = "检验分类编码长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).+$", message = "检验分类编码不能使用系统保留前缀")
    private String categoryCode;

    /** 分类名称；不能为空且最长 128 个字符。 */
    @NotBlank(message = "检验分类名称不能为空")
    @Size(max = 128, message = "检验分类名称长度不能超过 128")
    private String categoryName;

    /** 启用状态：0 停用、1 启用；为空时由创建或修改业务规则确定默认行为。 */
    @Min(value = 0, message = "启用状态只能为 0 或 1")
    @Max(value = 1, message = "启用状态只能为 0 或 1")
    private Integer enabledStatus;

    /** 分类用途或维护备注，最长 255 个字符。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
