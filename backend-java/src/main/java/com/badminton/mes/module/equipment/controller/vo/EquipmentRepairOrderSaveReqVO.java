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
 * <p>注解负责必填、长度、正数和状态枚举等单字段约束。报修单号生成及唯一性、设备和故障原理
 * 有效性、用户可用性、状态迁移、维修时间先后关系以及设备状态联动由 Service 在事务内校验。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@Data
public class EquipmentRepairOrderSaveReqVO {

    /** 业务唯一的报修单号；可空，创建时由后端自动生成。 */
    @Size(max = 32, message = "报修单号长度不能超过 32")
    private String repairNo;

    /** 发生故障的设备台账主键，必须指向有效且可报修的设备。 */
    @NotNull(message = "报修设备不能为空")
    @Positive(message = "报修设备必须为正整数")
    private Long equipmentId;

    /** 匹配的故障原理主键，可空；非空时应适用于当前设备类别。 */
    @Positive(message = "故障原理必须为正整数")
    private Long faultPrincipleId;

    /** 现场观察到的故障现象和影响，不能为空，最大 500 个字符。 */
    @NotBlank(message = "故障描述不能为空")
    @Size(max = 500, message = "故障描述长度不能超过 500")
    private String faultDescription;

    /** 故障上报时间；可空，创建时由 Service 补充当前时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;

    /** 报修人用户主键；可空，创建时默认使用当前操作用户。 */
    @Positive(message = "报修人必须为正整数")
    private Long reportUserId;

    /** 被指派的维修人用户主键，可空；派工后须指向可用用户。 */
    @Positive(message = "维修人必须为正整数")
    private Long repairUserId;

    /** 实际维修开始时间，可空；进入维修状态时由业务规则约束或补充。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairStartTime;

    /** 实际维修结束时间，可空；不得早于维修开始时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairEndTime;

    /** 维修过程、处理措施及最终结果，可空，最大 500 个字符。 */
    @Size(max = 500, message = "维修结果长度不能超过 500")
    private String repairResult;

    /** 目标报修状态；可空并默认 REPORTED，合法迁移路径由 Service 状态机约束。 */
    @Pattern(regexp = "^(REPORTED|ASSIGNED|REPAIRING|FINISHED|CANCELLED)$",
             message = "报修状态必须为 REPORTED、ASSIGNED、REPAIRING、FINISHED、CANCELLED 之一")
    private String repairStatus;

    /** 报修任务的其他补充说明，可空，最大 255 个字符。 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
