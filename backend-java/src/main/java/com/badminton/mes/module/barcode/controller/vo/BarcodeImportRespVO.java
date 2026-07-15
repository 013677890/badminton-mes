package com.badminton.mes.module.barcode.controller.vo;

import java.util.List;

import lombok.Data;

/**
 * 条码外部导入响应 VO：成功数、失败数与逐条失败原因。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeImportRespVO {

    /** 提交总条数 */
    private Integer totalCount;

    /** 导入成功条数 */
    private Integer successCount;

    /** 导入失败条数 */
    private Integer failCount;

    /** 逐条失败明细，全部成功时为空集合(API-002) */
    private List<Failure> failures;

    /**
     * 导入失败明细。
     *
     * @author 刘涵
     * @date 2026/07/12
     */
    @Data
    public static class Failure {

        /** 明细在提交数组中的下标，从 0 开始 */
        private Integer index;

        /** 条码值 */
        private String barcodeValue;

        /** 失败原因 */
        private String reason;
    }
}
