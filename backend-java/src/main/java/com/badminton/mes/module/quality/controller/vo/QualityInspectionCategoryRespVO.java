package com.badminton.mes.module.quality.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 检验分类详情响应。
 *
 * <p>返回分类业务标识、启停状态以及数据库维护的审计时间，供分类详情和分页列表统一展示。
 */
@Data
public class QualityInspectionCategoryRespVO {

    /** 检验分类数据库主键。 */
    private Long id;

    /** 检验分类业务编码，在未删除分类中保持唯一。 */
    private String categoryCode;

    /** 检验分类名称。 */
    private String categoryName;

    /** 启用状态：0 停用、1 启用。 */
    private Integer enabledStatus;

    /** 分类用途、范围或维护说明。 */
    private String remark;

    /** 分类创建时间，按服务器时间格式化到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 分类最近一次更新时间，按服务器时间格式化到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
