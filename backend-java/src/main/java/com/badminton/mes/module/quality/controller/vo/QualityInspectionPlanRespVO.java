package com.badminton.mes.module.quality.controller.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 检验标准方案详情响应。
 *
 * <p>同时返回方案适用范围、版本生命周期、审核审计信息及方案项目规则；分页列表场景中项目明细可为空。
 */
@Data
public class QualityInspectionPlanRespVO {

    /** 方案版本数据库主键；每个版本均拥有独立主键。 */
    private Long id;

    /** 方案业务编码，同一版本链中的各版本共享该编码。 */
    private String planCode;

    /** 方案名称，用于维护界面和检验建单时识别标准。 */
    private String planName;

    /** 适用产品主键；为空表示不限定具体产品。 */
    private Long productId;

    /** 适用客户主键；为空表示不限定具体客户。 */
    private Long customerId;

    /** 检验类型：FIRST_ARTICLE 首件、LAST_ARTICLE 末件、PATROL 巡检、WAREHOUSE_IN 入库检、SHIPMENT 发货检。 */
    private String inspectionType;

    /** 同一方案编码下的递增版本号，初始版本为 1。 */
    private Integer versionNo;

    /** 方案状态：DRAFT 草稿、EFFECTIVE 生效、DISABLED 停用。 */
    private String planStatus;

    /** 计划生效日期；审核时未预填则取审核当日，仅表达业务日期而不含时分秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    /** 是否为相同产品、客户和检验类型适用范围内的默认生效方案。 */
    private Boolean defaultFlag;

    /** 版本建立原因、适用例外或其他质量管理说明。 */
    private String remark;

    /** 创建该方案版本的操作人主键。 */
    private Long createBy;

    /** 将草稿审核为生效方案的操作人主键；未审核时为空。 */
    private Long auditBy;

    /** 方案审核生效的服务器时间；未审核时为空，序列化精确到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 该方案版本的创建时间，按服务器时间格式化到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 该方案版本最近一次更新时间，按服务器时间格式化到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 方案内按执行顺序返回的检验项目规则明细；摘要查询中可不加载。 */
    private List<QualityInspectionPlanItemRespVO> items;
}
