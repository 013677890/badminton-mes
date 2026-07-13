package com.badminton.mes.module.craft.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 工序不良原因响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftProcessDefectReasonRespVO {

    /** 主键 */
    private Long id;

    /** 工序主键 */
    private Long processId;

    /** 不良原因编码 */
    private String reasonCode;

    /** 不良原因名称 */
    private String reasonName;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 乐观锁版本 */
    private Integer version;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
