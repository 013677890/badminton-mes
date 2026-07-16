package com.badminton.mes.module.equipment.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备保养计划分页查询请求 VO。
 *
 * <p>继承 {@link PageParam} 复用统一页码和每页数量约束。本对象只表达可组合的查询条件；关键字的
 * LIKE 通配符转义、逻辑删除过滤以及越界页码归一化分别由查询规格和 Service 负责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentMaintenancePlanPageReqVO extends PageParam {

    /** 计划编码、名称或保养内容关键字，采用包含匹配，可空。 */
    @Size(max = 64, message = "搜索关键字长度不能超过 64")
    private String keyword;

    /** 设备台账主键，精确筛选该设备的计划，可空。 */
    @Positive(message = "设备必须为正整数")
    private Long equipmentId;

    /** 保养类型，可空；分别表示例行、预防性和专项保养。 */
    @Pattern(regexp = "^(ROUTINE|PREVENTIVE|SPECIAL)$",
             message = "保养类型必须为 ROUTINE、PREVENTIVE、SPECIAL 之一")
    private String maintenanceType;

    /** 计划启停状态，可空；0 表示停用，1 表示启用。 */
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;

    /** 下次保养时间闭区间的起点，可空。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceStartTime;

    /** 下次保养时间闭区间的终点，可空。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextMaintenanceEndTime;
}
