package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备计数异常池分页查询参数。
 *
 * <p>全部条件均可选，时间范围基于异常入池时间；处理状态区分待处理、已处理和已忽略，供
 * 异常池管理页面筛选人工待办和历史处理结果。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCountExceptionPageReqVO extends PageParam {

    /** 计数异常来源系统，按规范化编码精确筛选。 */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统包含不支持的字符")
    private String sourceSystem;

    /** 发生异常的设备编码，按规范化编码精确筛选。 */
    @Size(max = 32, message = "设备编码长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "设备编码包含不支持的字符")
    private String equipmentCode;

    /** 设备计数异常类型，按枚举值精确筛选。 */
    @Pattern(regexp = "^(DISPATCH_NOT_FOUND|DISPATCH_STATUS_INVALID|PROCESS_NOT_FOUND|"
            + "COUNT_NON_POSITIVE|COUNT_ROLLBACK)$",
            message = "异常类型不合法")
    private String exceptionType;

    /** 处理状态：0 待处理、1 已处理、2 已忽略。 */
    @Min(value = 0, message = "处理状态最小值为 0")
    @Max(value = 2, message = "处理状态最大值为 2")
    private Integer handleStatus;

    /** 异常创建时间闭区间起点，包含该时刻。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 异常创建时间闭区间终点，包含该时刻。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
