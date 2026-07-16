package com.badminton.mes.module.quality.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 质量检验单详情响应。
 *
 * <p>返回检验来源、采用的方案版本快照、单据生命周期、质量结论和放行状态；详情场景同时包含逐项结果。
 */
@Data
public class QualityInspectionRecordRespVO {

    /** 检验单数据库主键，也是全部项目结果的聚合标识。 */
    private Long id;

    /** 全局检验单号，用于现场流转、检索及外部单据关联。 */
    private String inspectionNo;

    /** 检验类型：FIRST_ARTICLE 首件、LAST_ARTICLE 末件、PATROL 巡检、WAREHOUSE_IN 入库检、SHIPMENT 发货检。 */
    private String inspectionType;

    /** 创建检验单时采用的具体方案版本主键。 */
    private Long planId;

    /** 建单时固化的方案业务编码快照，保证历史记录可追溯。 */
    private String planCode;

    /** 建单时固化的方案版本号快照。 */
    private Integer planVersion;

    /** 生产类检验关联的生产工单主键。 */
    private Long workOrderId;

    /** 生产类检验关联的生产任务主键；未关联具体任务时可为空。 */
    private Long productionTaskId;

    /** 非工单来源或扩展来源单据的主键。 */
    private Long sourceDocumentId;

    /** 来源单据号快照，便于脱离来源模块展示和追溯。 */
    private String sourceDocumentNo;

    /** 被检产品主键，需与方案适用范围及生产工单一致。 */
    private Long productId;

    /** 客户主键，用于匹配客户专用方案；不限定客户时可为空。 */
    private Long customerId;

    /** 生产线主键；由创建请求提供，用于展示本次生产类检验的现场范围。 */
    private Long productionLineId;

    /** 工序主键；用于标识本次生产类检验对应的具体工序。 */
    private Long processId;

    /** 被检产品或生产对象的批次号。 */
    private String batchNo;

    /** 检验单总体抽样数量，与各项目的项目级抽样要求配合使用。 */
    private Integer sampleQuantity;

    /** 单据状态：DRAFT 草稿可保存结果，SUBMITTED 已提交且不可继续编辑。 */
    private String recordStatus;

    /** 最终结论：PASS 合格、CONCESSION 让步接收、REWORK 返工、SCRAP 报废。 */
    private String conclusion;

    /** 后续业务放行状态：PENDING 待放行、RELEASED 已放行、BLOCKED 已阻断。 */
    private String releaseStatus;

    /** 不良分组号；有失败结果时用于关联同一批不良处置。 */
    private String defectGroupNo;

    /** 本次检验确认的不良数量；无失败项目时为零或空。 */
    private Integer defectQuantity;

    /** 存在失败项目时填写的不符合事实及偏差说明。 */
    private String nonconformanceDescription;

    /** 非合格结论对应的返工、报废或让步评审等处置意见。 */
    private String disposition;

    /** 最终提交该检验单的检验员主键；草稿阶段为空。 */
    private Long inspectorId;

    /** 检验完成并提交的服务器时间；草稿阶段为空，序列化精确到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inspectedAt;

    /** 检验单创建时间，按服务器时间格式化到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 检验单最近一次更新时间，按服务器时间格式化到秒。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 按执行顺序返回的项目结果快照；分页摘要场景中可不加载。 */
    private List<QualityInspectionResultRespVO> results;
}
