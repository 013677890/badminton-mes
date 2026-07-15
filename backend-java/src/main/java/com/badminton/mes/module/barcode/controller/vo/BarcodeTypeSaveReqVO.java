package com.badminton.mes.module.barcode.controller.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 条码类型创建/修改请求 VO。
 *
 * <p>状态不接收前端提交，新建默认启用，启停由独立动作接口流转；
 * 编码唯一性等业务规则在 Service 层校验(FLOW-013 入参校验)。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@Data
public class BarcodeTypeSaveReqVO {

    /** 类型编码，全局唯一 */
    @NotBlank(message = "类型编码不能为空")
    @Size(max = 32, message = "类型编码长度不能超过 32")
    private String typeCode;

    /** 类型名称，如产品码/内外箱码/中箱码/栈板码/材料码 */
    @NotBlank(message = "类型名称不能为空")
    @Size(max = 64, message = "类型名称长度不能超过 64")
    private String typeName;

    /** 适用对象说明，可空 */
    @Size(max = 64, message = "适用对象说明长度不能超过 64")
    private String applyObject;
}
