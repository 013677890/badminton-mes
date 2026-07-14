package com.badminton.mes.module.craft.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 工序 SOP 响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftProcessSopRespVO {

    /** 主键 */
    private Long id;

    /** 工序主键 */
    private Long processId;

    /** SOP 编码 */
    private String sopCode;

    /** SOP 名称 */
    private String sopName;

    /** SOP 版本 */
    private String sopVersion;

    /** SOP 文件地址 */
    private String fileUrl;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    /** 乐观锁版本 */
    private Integer version;

    /** 停用时是否需要重新绑定 */
    private Boolean rebindRequired;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
