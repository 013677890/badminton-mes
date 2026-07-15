package com.badminton.mes.module.barcode.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.BarcodePrintRecordEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 条码打印记录 JPA Repository。
 *
 * <p>逐次插入模型：打印序号在应用层按最新记录 +1 分配，
 * (barcode_id, print_count) 唯一索引兜底并发重打。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodePrintRecordRepository extends JpaRepository<BarcodePrintRecordEntity, Long> {

    /**
     * 查询条码最近一次打印记录(打印序号最大)。
     *
     * @param barcodeId 条码主键
     * @return 最近打印记录
     */
    Optional<BarcodePrintRecordEntity> findFirstByBarcodeIdAndDeletedFalseOrderByPrintCountDesc(
            Long barcodeId);
}
