package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产工单分页查询请求 VO，继承 {@link PageParam} 获得分页参数与入参保护。
 *
 * <p>筛选条件与 WorkOrderMapper.xml 的 pageWhere 片段一一对应：
 * 工单号右模糊(INDEX-004 严禁左模糊)，车间+状态+交期可命中组合索引 idx_workshop_status。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderPageReqVO extends PageParam {

    /** 工单号前缀，右模糊匹配，可空 */
    @Size(max = 32, message = "工单号长度不能超过 32")
    private String workOrderNo;

    /** 车间 id，可空 */
    @Positive(message = "车间 id 必须为正数")
    private Long workshopId;

    /** 工单状态，可空，取值见 WorkOrderStatusEnum(0-6) */
    @Min(value = 0, message = "工单状态取值为 0-6")
    @Max(value = 6, message = "工单状态取值为 0-6")
    private Integer orderStatus;

    /** 计划完成时间筛选起点(含)，GET 参数格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTimeBegin;

    /** 计划完成时间筛选终点(含)，GET 参数格式 yyyy-MM-dd HH:mm:ss */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTimeEnd;
}
