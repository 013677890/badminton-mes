package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 车间基础资料响应。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Data
public class WorkshopRespVO {

    /** 车间主键 */
    private Long id;

    /** 车间编码 */
    private String workshopCode;

    /** 车间名称 */
    private String workshopName;

    /** 车间主管用户 id */
    private Long managerId;

    /** 车间主管姓名（回填，便于前端直接展示） */
    private String managerName;

    /** 启停状态 */
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
