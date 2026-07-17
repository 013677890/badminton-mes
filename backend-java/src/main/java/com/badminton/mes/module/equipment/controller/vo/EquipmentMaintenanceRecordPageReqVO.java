package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备保养记录分页查询请求 VO。
 *
 * <p>提供保养任务的组合筛选条件并继承统一分页约束。所有条件均可空；查询规格始终附加未逻辑删除
 * 条件，避免历史删除记录混入正常任务列表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentMaintenanceRecordPageReqVO extends PageParam {

    /** 任务编号、实际保养内容或异常说明关键字，采用包含匹配。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 保养计划主键，用于精确筛选某计划产生的任务。 */
    @Positive(message = "保养计划必须为正整数")
    private Long planId;

    /** 设备台账主键，用于精确筛选某设备的保养历史。 */
    @Positive(message = "设备必须为正整数")
    private Long equipmentId;

    /** 任务状态：待处理、执行中、已完成或已取消。 */
    @Pattern(regexp = "^(PENDING|IN_PROGRESS|COMPLETED|CANCELLED)$",
             message = "任务状态必须为 PENDING、IN_PROGRESS、COMPLETED、CANCELLED 之一")
    private String recordStatus;

    /** 保养结论：正常或异常；未完成任务通常为空。 */
    @Pattern(regexp = "^(NORMAL|ABNORMAL)$", message = "保养结果必须为 NORMAL 或 ABNORMAL")
    private String maintenanceResult;

    /** 计划执行时间闭区间的起点，可空。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    /** 计划执行时间闭区间的终点，可空。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledEndTime;
}
