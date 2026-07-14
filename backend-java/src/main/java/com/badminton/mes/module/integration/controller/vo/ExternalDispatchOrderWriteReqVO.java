package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 外部生产任务单（派工单）写入请求。
 *
 * <p>外部系统通过工单号、产线编码和班次编码引用 MES 已有主数据，
 * MES 解析后复用 {@code DispatchOrderService.createDispatch} 生成待审核派工单。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class ExternalDispatchOrderWriteReqVO {

    /** 来源系统编码 */
    @NotBlank(message = "来源系统不能为空")
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统只能包含字母、数字、下划线和连字符")
    private String sourceSystem;

    /** 外部任务单号，来源系统内唯一 */
    @NotBlank(message = "外部任务单号不能为空")
    @Size(max = 64, message = "外部任务单号长度不能超过 64")
    @Pattern(regexp = "^[A-Za-z0-9._/-]+$", message = "外部任务单号包含不支持的字符")
    private String externalDispatchOrderNo;

    /** MES 生产工单号 */
    @NotBlank(message = "生产工单号不能为空")
    @Size(max = 32, message = "生产工单号长度不能超过 32")
    private String workOrderNo;

    /** 产线编码 */
    @NotBlank(message = "产线编码不能为空")
    @Size(max = 32, message = "产线编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "产线编码只能包含字母、数字、下划线和连字符")
    private String lineCode;

    /** 班次编码 */
    @NotBlank(message = "班次编码不能为空")
    @Size(max = 32, message = "班次编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "班次编码只能包含字母、数字、下划线和连字符")
    private String shiftCode;

    /** 排产日期 */
    @NotNull(message = "排产日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    /** 计划数量 */
    @NotNull(message = "计划数量不能为空")
    @Min(value = 1, message = "计划数量最小值为 1")
    private Integer planQuantity;

    /** 计划开始时间 */
    @NotNull(message = "计划开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    @NotNull(message = "计划结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;
}
