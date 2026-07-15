package com.badminton.mes.module.barcode.controller.vo;

import java.util.List;

import lombok.Data;

/**
 * 条码规则预览响应 VO。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeRulePreviewRespVO {

    /** 按样例上下文与流水号 1 试算的条码值 */
    private String barcodeValue;

    /** 试算条码总长度，上限 64 */
    private Integer totalLength;

    /** 单周期流水容量：10^流水位数 - 1 */
    private Long serialCapacity;

    /** 分段试算结果，按 seq 升序 */
    private List<Segment> segments;

    /**
     * 预览分段结果。
     *
     * @author 刘涵
     * @date 2026/07/12
     */
    @Data
    public static class Segment {

        /** 组成顺序 */
        private Integer seq;

        /** 组成类型：1 常量 2 日期 3 变量 4 流水号 */
        private Integer itemType;

        /** 该段试算内容 */
        private String content;
    }
}
