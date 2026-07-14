package com.badminton.mes.module.craft.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 工艺路线变更日志响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftRouteChangeLogRespVO {

    /** 日志主键 */
    private Long id;

    /** 路线主键 */
    private Long routeId;

    /** 变更类型 */
    private Integer changeType;

    /** 变更前快照 */
    private String beforeSnapshot;

    /** 变更后快照 */
    private String afterSnapshot;

    /** 变更原因 */
    private String changeReason;

    /** 操作人 */
    private Long operatorId;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
