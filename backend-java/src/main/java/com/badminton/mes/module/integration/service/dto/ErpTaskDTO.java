package com.badminton.mes.module.integration.service.dto;

import java.time.LocalDateTime;

/**
 * ERP 生产任务单数据传输对象，由 Mock 数据源返回，供同步命令服务消费。
 *
 * @param erpOrderNo    ERP 任务单号
 * @param productCode   产品编码
 * @param planQuantity  计划数量
 * @param planStartTime 计划开始时间
 * @param planEndTime   计划完成时间
 * @param workshopCode  目标车间编码
 * @param batchNo       生产批次号，可空
 * @author 张竹灏
 * @date 2026/07/13
 */
public record ErpTaskDTO(String erpOrderNo,
                         String productCode,
                         Integer planQuantity,
                         LocalDateTime planStartTime,
                         LocalDateTime planEndTime,
                         String workshopCode,
                         String batchNo) {
}
