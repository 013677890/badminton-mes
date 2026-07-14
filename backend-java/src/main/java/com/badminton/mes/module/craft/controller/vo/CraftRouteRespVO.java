package com.badminton.mes.module.craft.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 工艺路线响应 VO。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Data
public class CraftRouteRespVO {

    /** 路线主键 */
    private Long id;

    /** 路线编码 */
    private String routingCode;

    /** 路线名称 */
    private String routingName;

    /** 业务版本 */
    private String routingVersion;

    /** 上一版本路线主键 */
    private Long previousRouteId;

    /** 来源：1 本地创建 2 ERP 读取确认 */
    private Integer sourceType;

    /** 状态：0 草稿 1 生效 2 停用 */
    private Integer routingStatus;

    /** 审核人 */
    private Long auditBy;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 乐观锁版本 */
    private Integer version;

    /** 适用产品列表 */
    private List<CraftRouteProductRespVO> products;

    /** 路线步骤列表 */
    private List<CraftRouteStepRespVO> steps;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
