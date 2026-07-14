package com.badminton.mes.module.integration.controller.vo;

import java.time.LocalDateTime;

import com.badminton.mes.common.core.PageParam;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产完工单读取日志分页参数。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompletionReadLogPageReqVO extends PageParam {

    /** 读取来源系统 */
    @Size(max = 32, message = "来源系统长度不能超过 32")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "来源系统包含不支持的字符")
    private String sourceSystem;

    /** 完工单号 */
    @Size(max = 32, message = "完工单号长度不能超过 32")
    private String completionNo;

    /** 读取起始时间，包含 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 读取结束时间，包含 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
