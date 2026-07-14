package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDate;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 派工单分页查询请求 VO，继承 {@link PageParam} 获得分页参数与入参保护。
 *
 * <p>产线+日期+班次组合可命中 idx_line_date_shift。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DispatchPageReqVO extends PageParam {

    /** 来源工单 id，可空 */
    @Positive(message = "工单 id 必须为正数")
    private Long workOrderId;

    /** 产线 id，可空 */
    @Positive(message = "产线 id 必须为正数")
    private Long lineId;

    /** 班次 id，可空 */
    @Positive(message = "班次 id 必须为正数")
    private Long shiftId;

    /** 派工状态，可空，取值见 DispatchStatusEnum(0-5) */
    @Min(value = 0, message = "派工状态取值为 0-5")
    @Max(value = 5, message = "派工状态取值为 0-5")
    private Integer dispatchStatus;

    /** 排产日期筛选起点(含)，GET 参数格式 yyyy-MM-dd */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateBegin;

    /** 排产日期筛选终点(含)，GET 参数格式 yyyy-MM-dd */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateEnd;
}
