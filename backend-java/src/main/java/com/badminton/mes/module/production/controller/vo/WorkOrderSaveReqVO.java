package com.badminton.mes.module.production.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 生产工单创建/修改请求 VO。
 *
 * <p>对外开放接口必须做入参校验(FLOW-013)，单字段规则用注解声明；
 * "计划完成时间不早于开始时间"等跨字段业务规则在 Service 层校验。
 * 产品名称、规格、单位等冗余字段不接收前端提交，由服务端按产品档案回填，
 * 防止与档案不一致。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
public class WorkOrderSaveReqVO {

    /** 工单号，可空；未传时由系统按"WO+日期+流水"生成。仅创建时生效，修改时忽略 */
    @Size(max = 32, message = "工单号长度不能超过 32")
    private String workOrderNo;

    /** 产品 id */
    @NotNull(message = "产品不能为空")
    @Positive(message = "产品 id 必须为正数")
    private Long productId;

    /** 生产批次号 */
    @Size(max = 64, message = "批次号长度不能超过 64")
    private String batchNo;

    /** BOM 版本 id，创建时可空，下达前必须维护 */
    @Positive(message = "BOM id 必须为正数")
    private Long bomId;

    /** 工艺路线 id，创建时可空，下达前必须维护 */
    @Positive(message = "工艺路线 id 必须为正数")
    private Long routingId;

    /** 客户 id，可空 */
    @Positive(message = "客户 id 必须为正数")
    private Long customerId;

    /** 目标车间 id */
    @NotNull(message = "车间不能为空")
    @Positive(message = "车间 id 必须为正数")
    private Long workshopId;

    /** 计划数量 */
    @NotNull(message = "计划数量不能为空")
    @Min(value = 1, message = "计划数量最小值为 1")
    private Integer planQuantity;

    /** 允许超产比例(%)，可空，数据库默认 0 */
    @DecimalMin(value = "0", message = "超产比例不能为负数")
    @Digits(integer = 3, fraction = 2, message = "超产比例整数最多 3 位、小数最多 2 位")
    private BigDecimal overRatio;

    /** 优先级：1 最高 - 9 最低，可空，数据库默认 5 */
    @Min(value = 1, message = "优先级最小值为 1")
    @Max(value = 9, message = "优先级最大值为 9")
    private Integer priority;

    /** 计划开始时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @NotNull(message = "计划开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStartTime;

    /** 计划完成时间(交期)，格式 yyyy-MM-dd HH:mm:ss，不得早于计划开始时间 */
    @NotNull(message = "计划完成时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEndTime;

    /** 变更原因：已下达工单修改计划数量或交期时必填，已创建状态修改可空 */
    @Size(max = 255, message = "变更原因长度不能超过 255")
    private String changeReason;
}
