package com.badminton.mes.module.barcode.controller.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 条码打印响应 VO：打印记录事实 + 预览数据。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodePrintRespVO {

    /** 打印记录主键 */
    private Long printRecordId;

    /** 条码主键 */
    private Long barcodeId;

    /** 条码值 */
    private String barcodeValue;

    /** 打印模板 id(具体版本行) */
    private Long templateId;

    /** 打印时模板版本快照 */
    private String templateVersion;

    /** 本次打印序号(同一条码从 1 递增) */
    private Integer printCount;

    /** 打印时间，格式 yyyy-MM-dd HH:mm:ss(API-013) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime printTime;

    /** 标签预览数据，与持久化快照一致 */
    private BarcodeTemplatePreviewRespVO preview;
}
