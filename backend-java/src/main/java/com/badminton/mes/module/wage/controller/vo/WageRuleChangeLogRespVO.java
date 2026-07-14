package com.badminton.mes.module.wage.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** 计件规则变更日志响应。 */
@Data
public class WageRuleChangeLogRespVO {
    /** 主键 */
    private Long id;
    /** 规则主键 */
    private Long ruleId;
    /** 变更类型 */
    private String changeType;
    /** 变更前快照 */
    private String beforeSnapshot;
    /** 变更后快照 */
    private String afterSnapshot;
    /** 变更原因 */
    private String changeReason;
    /** 操作人 */
    private Long operateBy;
    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;
}
