package com.badminton.mes.module.barcode.service;

/** 条码模块向 M2 scene 暴露的稳定扫码契约。 @author 刘涵 */
public interface BarcodeSceneService {
    BarcodeSceneSnapshot validateAndRecordUse(String barcodeValue, Long taskId, Long productId, String batchNo,
                                              Long processId, Long userId, Long equipmentId, Integer useType);
    record BarcodeSceneSnapshot(Long barcodeId, Long productId, String batchNo, Long taskId) { }
}
