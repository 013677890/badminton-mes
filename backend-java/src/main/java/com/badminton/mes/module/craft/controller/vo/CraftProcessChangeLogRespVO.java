package com.badminton.mes.module.craft.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 工序变更日志响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftProcessChangeLogRespVO {

    /** 日志主键 */
    private Long id;

    /** 工序主键 */
    private Long processId;

    /** 变更类型：1 创建 2 修改 3 状态变更 4 删除 5 SOP 变更 6 不良原因变更 */
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
