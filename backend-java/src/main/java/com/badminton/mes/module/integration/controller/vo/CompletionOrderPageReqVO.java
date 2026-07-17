package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 已审核生产完工单分页读取参数。
 *
 * <p>来源系统是必填项，因为每次返回完工单都要写逐条读取日志；其余条件用于限定审核时间、
 * 完工单号或生产工单号，Service 会先校验时间区间再查询。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompletionOrderPageReqVO extends PageParam {

    /** 读取来源系统，用于逐条读取日志和审计分区。 */
    @NotBlank(message = "来源系统不能为空")
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统包含不支持的字符")
    private String sourceSystem;

    /** 审核时间闭区间起点，包含该时刻。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 审核时间闭区间终点，包含该时刻。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 完工单号 */
    @Size(max = 32, message = "完工单号长度不能超过 32")
    private String completionNo;

    /** 生产工单号 */
    @Size(max = 32, message = "生产工单号长度不能超过 32")
    private String workOrderNo;
}
