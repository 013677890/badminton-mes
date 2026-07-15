package com.badminton.mes.module.report.service;

import com.badminton.mes.module.report.controller.vo.ProductTraceQueryReqVO;
import com.badminton.mes.module.report.controller.vo.ProductTraceRespVO;

/**
 * 产品批次与条码追溯服务。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public interface ProductTraceService {

    /** 按任一业务键查询产品主链路追溯。 */
    ProductTraceRespVO trace(ProductTraceQueryReqVO reqVO);

    /** 按条码值查询产品主链路追溯。 */
    ProductTraceRespVO traceByBarcode(String barcodeValue);
}
