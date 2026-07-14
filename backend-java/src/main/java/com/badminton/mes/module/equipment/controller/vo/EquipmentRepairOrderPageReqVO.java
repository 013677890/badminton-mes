package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备报修任务分页查询请求 VO。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentRepairOrderPageReqVO extends PageParam {

    /** 报修单号、故障描述或维修结果，模糊匹配，可空 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 设备台账 id，可空 */
    @Positive(message = "设备必须为正整数")
    private Long equipmentId;

    /** 故障原理 id，可空 */
    @Positive(message = "故障原理必须为正整数")
    private Long faultPrincipleId;

    /** 报修状态，可空 */
    @Pattern(regexp = "^(REPORTED|ASSIGNED|REPAIRING|FINISHED|CANCELLED)$",
             message = "报修状态必须为 REPORTED、ASSIGNED、REPAIRING、FINISHED、CANCELLED 之一")
    private String repairStatus;

    /** 报修开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportStartTime;

    /** 报修结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportEndTime;
}
