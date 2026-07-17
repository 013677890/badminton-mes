package com.badminton.mes.module.barcode.service;

/**
 * 条码模块向 M2 现场执行模块暴露的稳定扫码契约。
 *
 * <p>调用方只传入当前生产上下文，本服务负责校验条码状态与业务对象匹配关系，
 * 并在同一事务中记录扫码使用事实，避免现场模块直接依赖条码表结构。
 *
 * @author 刘涵
 */
public interface BarcodeSceneService {

    /**
     * 校验条码是否可用于当前任务，并记录本次扫码使用行为。
     *
     * @param barcodeValue 条码值
     * @param taskId 当前生产任务 id
     * @param productId 当前任务产品 id
     * @param batchNo 当前生产批次号
     * @param processId 当前工序 id
     * @param userId 扫码人员 id
     * @param equipmentId 使用设备 id，可空
     * @param useType 使用类型
     * @return 已通过校验的条码最小业务快照
     */
    BarcodeSceneSnapshot validateAndRecordUse(String barcodeValue, Long taskId, Long productId, String batchNo,
                                              Long processId, Long userId, Long equipmentId, Integer useType);

    /**
     * 现场执行所需的条码最小快照，隔离条码持久化实体与跨模块调用方。
     *
     * @param barcodeId 条码主键
     * @param productId 条码关联产品 id
     * @param batchNo 条码关联批次号
     * @param taskId 条码关联任务 id
     */
    record BarcodeSceneSnapshot(Long barcodeId, Long productId, String batchNo, Long taskId) { }
}
