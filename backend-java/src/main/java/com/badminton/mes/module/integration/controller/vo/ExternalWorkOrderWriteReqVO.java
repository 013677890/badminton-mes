package com.badminton.mes.module.integration.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 外部生产工单写入请求。
 *
 * @author 张竹灏
 * @date 2026/07/11
 */
@Data
public class ExternalWorkOrderWriteReqVO {

    /** 来源系统编码 */
    @NotBlank(message = "来源系统不能为空")
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统只能包含字母、数字、下划线和连字符")
    private String sourceSystem;

    /** 外部工单号，来源系统内唯一 */
    @NotBlank(message = "外部工单号不能为空")
    @Size(max = 64, message = "外部工单号长度不能超过 64")
    @Pattern(regexp = "^[A-Za-z0-9._/-]+$", message = "外部工单号包含不支持的字符")
    private String externalWorkOrderNo;

    /** 产品编码 */
    @NotBlank(message = "产品编码不能为空")
    @Size(max = 32, message = "产品编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "产品编码包含不支持的字符")
    private String productCode;

    /** 目标车间编码 */
    @NotBlank(message = "车间编码不能为空")
    @Size(max = 32, message = "车间编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "车间编码包含不支持的字符")
    private String workshopCode;

    /** 生产批次号 */
    @Size(max = 64, message = "批次号长度不能超过 64")
    private String batchNo;

    /** 生效 BOM 主键 */
    @NotNull(message = "BOM 不能为空")
    @Positive(message = "BOM id 必须为正数")
    private Long bomId;

    /** 已生效且绑定产品的工艺路线主键 */
    @NotNull(message = "工艺路线不能为空")
    @Positive(message = "工艺路线 id 必须为正数")
    private Long routingId;

    /** 客户主键 */
    @Positive(message = "客户 id 必须为正数")
    private Long customerId;

    /** 计划数量 */
    @NotNull(message = "计划数量不能为空")
    @Min(value = 1, message = "计划数量最小值为 1")
    private Integer planQuantity;

    /** 允许超产比例 */
    @DecimalMin(value = "0", message = "超产比例不能为负数")
    @Digits(integer = 3, fraction = 2, message = "超产比例整数最多 3 位、小数最多 2 位")
    private BigDecimal overRatio;

    /** 优先级：1 最高 - 9 最低 */
    @Min(value = 1, message = "优先级最小值为 1")
    @Max(value = 9, message = "优先级最大值为 9")
    private Integer priority;

    /** 计划开始时间 */
    @NotNull(message = "计划开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    /** 计划完成时间 */
    @NotNull(message = "计划完成时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;
}
