package com.badminton.mes.module.report.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 产量、时段和不良报表的统一查询条件。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportQueryReqVO extends PageParam {

    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @Positive
    private Long workshopId;

    @Positive
    private Long lineId;

    @Positive
    private Long productId;

    @Positive
    private Long workOrderId;

    @Positive
    private Long taskId;

    @Positive
    private Long processId;

    @Positive
    private Long shiftId;

    @Size(max = 64)
    private String batchNo;

    private Integer status;
}
