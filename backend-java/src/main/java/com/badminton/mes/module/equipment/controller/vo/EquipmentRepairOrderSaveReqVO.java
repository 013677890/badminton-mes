package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备报修任务创建/修改请求 VO。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
public class EquipmentRepairOrderSaveReqVO {

    /** 报修单号，可空，后端自动生成 */
    @Size(max = 32, message = "报修单号长度不能超过 32")
    private String repairNo;

    /** 设备台账 id */
    @NotNull(message = "报修设备不能为空")
    @Positive(message = "报修设备必须为正整数")
    private Long equipmentId;

    /** 故障原理 id，可空 */
    @Positive(message = "故障原理必须为正整数")
    private Long faultPrincipleId;

    /** 故障描述 */
    @NotBlank(message = "故障描述不能为空")
    @Size(max = 500, message = "故障描述长度不能超过 500")
    private String faultDescription;

    /** 报修时间，可空，默认当前时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;

    /** 报修人用户 id，可空，默认当前操作人 */
    @Positive(message = "报修人必须为正整数")
    private Long reportUserId;

    /** 维修人用户 id，可空 */
    @Positive(message = "维修人必须为正整数")
    private Long repairUserId;

    /** 维修开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairStartTime;

    /** 维修结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairEndTime;

    /** 维修结果 */
    @Size(max = 500, message = "维修结果长度不能超过 500")
    private String repairResult;

    /** 报修状态，可空，默认 REPORTED */
    @Pattern(regexp = "^(REPORTED|ASSIGNED|REPAIRING|FINISHED|CANCELLED)$",
             message = "报修状态必须为 REPORTED、ASSIGNED、REPAIRING、FINISHED、CANCELLED 之一")
    private String repairStatus;

    /** 备注说明 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
