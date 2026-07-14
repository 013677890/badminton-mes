package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 派工单排产调整日志响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@Data
public class DispatchAdjustLogRespVO {

    /** 主键 */
    private Long id;

    /** 派工单 id */
    private Long dispatchOrderId;

    /** 记录类型：1 系统建议 2 人工创建 3 调整 4 审核 5 下发 6 取消 */
    private Integer adjustType;

    /** 调整前快照(JSON) */
    private String beforeSnapshot;

    /** 调整后快照(JSON) */
    private String afterSnapshot;

    /** 调整原因 */
    private String adjustReason;

    /** 操作人 */
    private Long operatorId;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
