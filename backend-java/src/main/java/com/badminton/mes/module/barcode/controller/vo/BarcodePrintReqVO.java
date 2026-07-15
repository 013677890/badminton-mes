package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码打印请求 VO。
 *
 * <p>第一阶段不对接真实打印机，打印=插入记录并返回预览数据(已冻结决策)；
 * 重复打印(序号 > 1)必须填写原因。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodePrintReqVO {

    /** 打印原因；重复打印时必填 */
    @Size(max = 255, message = "打印原因长度不能超过 255")
    private String reason;
}
