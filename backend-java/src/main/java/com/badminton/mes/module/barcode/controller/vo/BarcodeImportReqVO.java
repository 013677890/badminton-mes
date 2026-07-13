package com.badminton.mes.module.barcode.controller.vo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码外部导入请求 VO。
 *
 * <p>M1 待确认事项②口径(用户 2026-07-12 确认)：JSON 数组同步导入，
 * 前端解析文件后提交，单次最多 500 条，超限整单拒绝；逐条校验格式、
 * 长度与重复性，响应返回逐条失败原因。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeImportReqVO {

    /** 单次导入上限 */
    public static final int MAX_ITEMS = 500;

    /** 条码应用规则 id，来源必须为外部导入 */
    @NotNull(message = "应用规则不能为空")
    @Positive(message = "应用规则 id 必须为正数")
    private Long applyRuleId;

    /** 导入明细，单次最多 500 条 */
    @NotEmpty(message = "导入明细不能为空")
    @Size(max = MAX_ITEMS, message = "单次导入不能超过 500 条")
    @Valid
    private List<Item> items;

    /**
     * 导入明细项。
     *
     * @author 刘涵
     * @date 2026/07/12
     */
    @Data
    public static class Item {

        /** 外部条码值 */
        @NotBlank(message = "条码值不能为空")
        @Size(max = 64, message = "条码值长度不能超过 64")
        private String barcodeValue;

        /** 批次号，可空；批次码模式下缺省取条码值 */
        @Size(max = 64, message = "批次号长度不能超过 64")
        private String batchNo;

        /** 关联生产工单 id，可空；提供时校验存在性与车间数据范围 */
        @Positive(message = "工单 id 必须为正数")
        private Long workOrderId;

        /** 关联生产任务单 id，可空(逻辑引用，M2 落地任务表后补充校验) */
        @Positive(message = "任务 id 必须为正数")
        private Long taskId;
    }
}
