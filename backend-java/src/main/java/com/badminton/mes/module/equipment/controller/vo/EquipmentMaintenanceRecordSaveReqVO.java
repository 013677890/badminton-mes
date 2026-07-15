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

/**
 * 设备保养记录创建/修改请求 VO。
 *
 * <p>创建时 Service 强制将任务初始化为 PENDING，并清除不应由客户端提前写入的执行时间和结果；
 * 修改时 Service 再依据状态机合并可变字段。跨字段约束中，异常结果必须附带异常说明由
 * {@link #isAbnormalDescriptionValid()} 提前校验，其余时间先后关系和状态前置条件在事务内校验。
 */
@Data
public class EquipmentMaintenanceRecordSaveReqVO {

    /** 业务任务编号，可空并由后端生成；禁止使用逻辑删除记录的保留前缀。 */
    @Size(max = 32, message = "保养任务编号长度不能超过 32")
    @Pattern(regexp = "^(?!__DELETED_).*$", message = "保养任务编号不能使用系统保留前缀")
    private String recordNo;

    /** 关联保养计划主键；更新时不允许把既有任务迁移到其他计划。 */
    @NotNull(message = "保养计划不能为空")
    @Positive(message = "保养计划必须为正整数")
    private Long planId;

    /** 计划执行时间，是任务排程时间而非实际开始时间。 */
    @NotNull(message = "计划执行时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledTime;

    /** 实际开始时间，可空；进入执行中状态时可由 Service 自动补充。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 实际完成时间，可空；不得早于实际开始时间或晚于当前时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /** 实际执行人用户主键，可空；非空时必须指向启用用户。 */
    @Positive(message = "执行人必须为正整数")
    private Long executorUserId;

    /** 本次实际执行内容，作为历史审计记录保留。 */
    @NotBlank(message = "实际保养内容不能为空")
    @Size(max = 500, message = "实际保养内容长度不能超过 500")
    private String maintenanceContent;

    /** 保养结论，完成任务时必须为 NORMAL 或 ABNORMAL。 */
    @Pattern(regexp = "^(NORMAL|ABNORMAL)$", message = "保养结果必须为 NORMAL 或 ABNORMAL")
    private String maintenanceResult;

    /** 目标任务状态；合法迁移路径由 Service 状态机约束。 */
    @Pattern(regexp = "^(PENDING|IN_PROGRESS|COMPLETED|CANCELLED)$",
             message = "任务状态必须为 PENDING、IN_PROGRESS、COMPLETED、CANCELLED 之一")
    private String recordStatus;

    /** 异常现象或原因说明；结果为 ABNORMAL 时必填。 */
    @Size(max = 500, message = "异常说明长度不能超过 500")
    private String abnormalDescription;

    /** 本次任务补充说明，可空。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    /**
     * 校验异常结果与异常说明的联动关系。
     *
     * @return 非异常结果始终返回 true；异常结果仅在说明包含有效文本时返回 true
     */
    @AssertTrue(message = "保养结果为 ABNORMAL 时必须填写异常说明")
    public boolean isAbnormalDescriptionValid() {
        return !"ABNORMAL".equals(maintenanceResult)
                || (abnormalDescription != null && !abnormalDescription.isBlank());
    }
}
