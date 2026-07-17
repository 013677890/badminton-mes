package com.badminton.mes.module.integration.controller.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单条物料库存与在途快照。
 *
 * <p>四类数量均为非负值，syncTime 是外部事实时间而非 MES 接收时间；数据库 upsert 会以该时间
 * 判断新旧，保证网络重试或乱序消息不会覆盖较新的库存。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Data
public class MaterialStockItemReqVO {

    /** MES 物料业务编码，服务层据此解析有效物料主档。 */
    @NotBlank(message = "物料编码不能为空")
    @Size(max = 32, message = "物料编码长度不能超过 32")
    private String materialCode;

    @NotNull(message = "可用数量不能为空")
    @DecimalMin(value = "0", message = "可用数量不能小于 0")
    /** 当前可直接使用的库存数量。 */
    private BigDecimal availableQuantity;

    /** 已被订单或其他业务锁定、暂不能自由使用的数量。 */
    @NotNull(message = "锁定数量不能为空")
    @DecimalMin(value = "0", message = "锁定数量不能小于 0")
    private BigDecimal lockedQuantity;

    @NotNull(message = "在检数量不能为空")
    @DecimalMin(value = "0", message = "在检数量不能小于 0")
    /** 正在质检、暂不能投料的数量。 */
    private BigDecimal checkingQuantity;

    /** 已发运或采购在途、尚未入库的数量。 */
    @NotNull(message = "在途数量不能为空")
    @DecimalMin(value = "0", message = "在途数量不能小于 0")
    private BigDecimal transitQuantity;

    @NotNull(message = "同步时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** WMS/ERP 生成该快照的业务时间，用于新旧快照比较。 */
    private LocalDateTime syncTime;
}
