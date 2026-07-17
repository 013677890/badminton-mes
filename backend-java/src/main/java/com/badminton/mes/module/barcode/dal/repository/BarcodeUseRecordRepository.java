package com.badminton.mes.module.barcode.dal.repository;

import java.util.List;

import com.badminton.mes.module.barcode.dal.entity.BarcodeUseRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 条码使用记录 JPA Repository。
 *
 * <p>M1 仅查询；写入随 M2 现场执行扫码切片补充。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeUseRecordRepository extends JpaRepository<BarcodeUseRecordEntity, Long> {

    /**
     * 查询条码的扫码使用轨迹，按业务发生时间倒序(命中 idx_barcode_business_time)。
     *
     * @param barcodeId 条码主键
     * @return 使用记录列表
     */
    List<BarcodeUseRecordEntity> findByBarcodeIdAndDeletedFalseOrderByBusinessTimeDesc(Long barcodeId);
}
