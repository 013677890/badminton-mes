package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 欠料处理记录创建请求 VO。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class ShortageHandleSaveReqVO {

    /** 生产工单 id */
    @NotNull(message = "生产工单不能为空")
    @Positive(message = "生产工单 id 必须为正数")
    private Long workOrderId;

    /** 欠料物料 id */
    @NotNull(message = "欠料物料不能为空")
    @Positive(message = "物料 id 必须为正数")
    private Long materialId;

    /** 处理方式：1 催采购 2 调拨 3 代用料 4 调整排产 */
    @NotNull(message = "处理方式不能为空")
    @Min(value = 1, message = "处理方式取值 1-4")
    @Max(value = 4, message = "处理方式取值 1-4")
    private Integer handleType;

    /** 责任人(sys_user.id) */
    @NotNull(message = "责任人不能为空")
    @Positive(message = "责任人 id 必须为正数")
    private Long handlerId;

    /** 预计到料日期，可空 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedArrivalDate;

    /** 处理说明，可空 */
    @Size(max = 255, message = "处理说明长度不能超过 255")
    private String handleRemark;
}
