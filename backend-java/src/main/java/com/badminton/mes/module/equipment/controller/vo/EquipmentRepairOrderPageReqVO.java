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
 * <p>所有筛选条件均可空，并可与 {@link PageParam} 的分页参数组合使用。关键字采用模糊匹配，
 * 设备、故障原理和报修状态采用精确匹配，报修时间按起止边界组合查询。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentRepairOrderPageReqVO extends PageParam {

    /** 报修单号、故障描述或维修结果关键字，采用模糊匹配，可空。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 设备台账主键，精确筛选该设备的报修历史；非空时必须为正数。 */
    @Positive(message = "设备必须为正整数")
    private Long equipmentId;

    /** 故障原理主键，精确筛选采用该故障分类的任务；非空时必须为正数。 */
    @Positive(message = "故障原理必须为正整数")
    private Long faultPrincipleId;

    /** 报修任务状态，可按已上报、已派工、维修中、已完成或已取消精确筛选。 */
    @Pattern(regexp = "^(REPORTED|ASSIGNED|REPAIRING|FINISHED|CANCELLED)$",
             message = "报修状态必须为 REPORTED、ASSIGNED、REPAIRING、FINISHED、CANCELLED 之一")
    private String repairStatus;

    /** 报修时间查询区间的起点，可空，按秒解析。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportStartTime;

    /** 报修时间查询区间的终点，可空，按秒解析。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportEndTime;
}
