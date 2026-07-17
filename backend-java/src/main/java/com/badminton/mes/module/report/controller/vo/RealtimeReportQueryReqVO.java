package com.badminton.mes.module.report.controller.vo;

import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 实时生产查询条件。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
@Data
public class RealtimeReportQueryReqVO {

    @Positive
    private Long workshopId;

    @Positive
    private Long lineId;

    @Positive
    private Long productId;
}
