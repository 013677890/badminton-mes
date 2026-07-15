package com.badminton.mes.module.barcode.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.BarcodeSerialEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 条码流水号记录 JPA Repository。
 *
 * <p>MySQL 是流水事实兜底：Redis 发号成功且条码落库后同步推进
 * current_serial；(rule_id, serial_scope) 唯一索引保证维度不重复。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeSerialRepository extends JpaRepository<BarcodeSerialEntity, Long> {

    /**
     * 判断规则是否存在流水记录，存在即说明规则已被用于生成条码，删除前校验。
     *
     * @param ruleId 规则主键
     * @return true 存在流水记录，false 无
     */
    boolean existsByRuleIdAndDeletedFalse(Long ruleId);

    /**
     * 按规则与作用域查询流水记录，Redis Key 丢失时播种恢复。
     *
     * @param ruleId 规则主键
     * @param scope  流水作用域
     * @return 流水记录
     */
    Optional<BarcodeSerialEntity> findByRuleIdAndSerialScopeAndDeletedFalse(Long ruleId, String scope);

    /**
     * 推进流水记录到不小于本次流水号的值(只前进不后退，容忍乱序落库)。
     *
     * @param ruleId 规则主键
     * @param scope  流水作用域
     * @param serial 本次流水号
     * @return 影响行数；0 表示维度记录尚不存在，由调用方插入
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeSerialEntity serialRecord
            SET serialRecord.currentSerial = CASE WHEN serialRecord.currentSerial < :serial
                    THEN :serial ELSE serialRecord.currentSerial END,
                serialRecord.updateTime = CURRENT_TIMESTAMP
            WHERE serialRecord.ruleId = :ruleId
              AND serialRecord.serialScope = :scope
              AND serialRecord.deleted = false
            """)
    int advanceSerial(@Param("ruleId") Long ruleId,
                      @Param("scope") String scope,
                      @Param("serial") Long serial);
}
