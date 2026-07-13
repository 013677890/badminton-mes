package com.badminton.mes.module.barcode.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.BarcodeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 条码主表 JPA Repository。
 *
 * <p>条码值全局唯一由 uk_barcode_value 兜底；作废等状态流转使用 CAS 更新，
 * 已使用条码不能作废在 UPDATE 条件内原子保证。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeRepository extends JpaRepository<BarcodeEntity, Long>,
        JpaSpecificationExecutor<BarcodeEntity> {

    /**
     * 判断应用规则是否已生成过条码，应用规则删除前校验。
     *
     * @param applyRuleId 应用规则主键
     * @return true 已生成条码，false 未生成
     */
    boolean existsByApplyRuleIdAndDeletedFalse(Long applyRuleId);

    /**
     * 按主键查询未删除的条码。
     *
     * @param id 条码主键
     * @return 条码实体
     */
    Optional<BarcodeEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按条码值查询未删除的条码，扫码解析入口(const 级唯一索引查询)。
     *
     * @param barcodeValue 条码值
     * @return 条码实体
     */
    Optional<BarcodeEntity> findByBarcodeValueAndDeletedFalse(String barcodeValue);

    /**
     * 判断未删除条码中是否已存在指定条码值，传入值/导入前查重。
     *
     * @param barcodeValue 条码值
     * @return true 存在，false 不存在
     */
    boolean existsByBarcodeValueAndDeletedFalse(String barcodeValue);

    /**
     * 条码状态流转 CAS：仅当当前状态等于前置状态才更新。
     * 作废时前置状态为未使用，"已使用不可作废"由本条件原子保证。
     *
     * @param id         条码主键
     * @param fromStatus 前置状态
     * @param toStatus   目标状态
     * @return 影响行数；0 表示条码不存在、已删除或状态不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeEntity barcode
            SET barcode.barcodeStatus = :toStatus,
                barcode.updateTime = CURRENT_TIMESTAMP
            WHERE barcode.id = :id
              AND barcode.barcodeStatus = :fromStatus
              AND barcode.deleted = false
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus);
}
