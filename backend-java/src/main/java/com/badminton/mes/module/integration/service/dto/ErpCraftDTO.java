package com.badminton.mes.module.integration.service.dto;

import java.util.List;

/**
 * ERP 工艺路线数据传输对象，由 Mock 数据源返回，供同步命令服务消费。
 *
 * @param erpRoutingCode    ERP 工艺路线编码
 * @param erpRoutingName    ERP 工艺路线名称
 * @param erpRoutingVersion ERP 工艺路线版本
 * @param productCode       适用产品编码
 * @param steps             工序步骤列表
 * @author 张竹灏
 * @date 2026/07/13
 */
public record ErpCraftDTO(String erpRoutingCode,
                          String erpRoutingName,
                          String erpRoutingVersion,
                          String productCode,
                          List<ErpCraftStepDTO> steps) {
}
