package com.badminton.mes.module.wage.controller.vo;

import java.time.LocalDate;

import com.badminton.mes.common.core.PageParam;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 报工计件快照分页请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WageWorkRecordPageReqVO extends PageParam {
    /** 员工主键 */
    @Positive(message = "员工 id 必须为正数")
    private Long employeeId;
    /** 工单主键 */
    @Positive(message = "工单 id 必须为正数")
    private Long workOrderId;
    /** 工序主键 */
    @Positive(message = "工序 id 必须为正数")
    private Long processId;
    /** 产品主键 */
    @Positive(message = "产品 id 必须为正数")
    private Long productId;
    /** 作业开始日期 */
    private LocalDate workDateBegin;
    /** 作业结束日期 */
    private LocalDate workDateEnd;
}
