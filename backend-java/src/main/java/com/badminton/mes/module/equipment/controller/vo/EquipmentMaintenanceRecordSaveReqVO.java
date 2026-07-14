package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 设备保养记录创建/修改请求 VO。 */
@Data
public class EquipmentMaintenanceRecordSaveReqVO {

    @Size(max = 32, message = "保养任务编号长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).*$", message = "保养任务编号不能使用系统保留前缀")
    private String recordNo;

    @NotNull(message = "保养计划不能为空")
    @Positive(message = "保养计划必须为正整数")
    private Long planId;

    @NotNull(message = "计划执行时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    @Positive(message = "执行人必须为正整数")
    private Long executorUserId;

    @NotBlank(message = "实际保养内容不能为空")
    @Size(max = 500, message = "实际保养内容长度不能超过 500")
    private String maintenanceContent;

    @Pattern(regexp = "^(NORMAL|ABNORMAL)$", message = "保养结果必须为 NORMAL 或 ABNORMAL")
    private String maintenanceResult;

    @Pattern(regexp = "^(PENDING|IN_PROGRESS|COMPLETED|CANCELLED)$",
             message = "任务状态必须为 PENDING、IN_PROGRESS、COMPLETED、CANCELLED 之一")
    private String recordStatus;

    @Size(max = 500, message = "异常说明长度不能超过 500")
    private String abnormalDescription;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    @AssertTrue(message = "保养结果为 ABNORMAL 时必须填写异常说明")
    public boolean isAbnormalDescriptionValid() {
        return !"ABNORMAL".equals(maintenanceResult)
                || (abnormalDescription != null && !abnormalDescription.isBlank());
    }
}
