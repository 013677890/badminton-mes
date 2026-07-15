package com.badminton.mes.module.barcode.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 条码规则 JPA Repository。
 *
 * <p>状态流转、信息修改和逻辑删除使用返回影响行数的 JPQL 更新，
 * 保留数据库层 CAS 语义。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeRuleRepository extends JpaRepository<BarcodeRuleEntity, Long>,
        JpaSpecificationExecutor<BarcodeRuleEntity> {

    /**
     * 按主键查询未删除的条码规则。
     *
     * @param id 规则主键
     * @return 规则实体
     */
    Optional<BarcodeRuleEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 判断未删除规则中是否存在引用指定条码类型的记录，类型删除前校验。
     *
     * @param barcodeTypeId 条码类型主键
     * @return true 存在引用，false 无引用
     */
    boolean existsByBarcodeTypeIdAndDeletedFalse(Long barcodeTypeId);

    /**
     * 判断未删除规则中是否已存在指定编码。
     *
     * @param ruleCode 规则编码
     * @return true 存在，false 不存在
     */
    boolean existsByRuleCodeAndDeletedFalse(String ruleCode);

    /**
     * 判断除指定主键外的未删除规则中是否已存在指定编码，修改时排除自身查重。
     *
     * @param ruleCode 规则编码
     * @param id       排除的规则主键
     * @return true 存在，false 不存在
     */
    boolean existsByRuleCodeAndIdNotAndDeletedFalse(String ruleCode, Long id);

    /**
     * 修改规则基础信息，deleted = false 条件构成 CAS，防止并发删除后误复活。
     * 规则修改只影响新生成条码，不影响历史条码(已冻结决策)。
     *
     * @param id               规则主键
     * @param ruleCode         规则编码
     * @param ruleName         规则名称
     * @param barcodeTypeId    适用条码类型 id
     * @param serialLength     流水号位数
     * @param serialResetCycle 流水号重置周期
     * @return 影响行数；0 表示规则不存在或已删除
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeRuleEntity rule
            SET rule.ruleCode = :ruleCode,
                rule.ruleName = :ruleName,
                rule.barcodeTypeId = :barcodeTypeId,
                rule.serialLength = :serialLength,
                rule.serialResetCycle = :serialResetCycle,
                rule.updateTime = CURRENT_TIMESTAMP
            WHERE rule.id = :id
              AND rule.deleted = false
            """)
    int updateInfo(@Param("id") Long id,
                   @Param("ruleCode") String ruleCode,
                   @Param("ruleName") String ruleName,
                   @Param("barcodeTypeId") Long barcodeTypeId,
                   @Param("serialLength") Integer serialLength,
                   @Param("serialResetCycle") Integer serialResetCycle);

    /**
     * 状态流转 CAS：仅当当前状态等于前置状态才更新，用于启用/停用。
     *
     * @param id         规则主键
     * @param fromStatus 前置状态
     * @param toStatus   目标状态
     * @return 影响行数；0 表示规则不存在、已删除或状态不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeRuleEntity rule
            SET rule.status = :toStatus,
                rule.updateTime = CURRENT_TIMESTAMP
            WHERE rule.id = :id
              AND rule.status = :fromStatus
              AND rule.deleted = false
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus);

    /**
     * 逻辑删除，deleted = false 条件构成 CAS，防止重复删除。
     *
     * @param id 规则主键
     * @return 影响行数；0 表示规则不存在或已删除
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeRuleEntity rule
            SET rule.deleted = true,
                rule.updateTime = CURRENT_TIMESTAMP
            WHERE rule.id = :id
              AND rule.deleted = false
            """)
    int logicDeleteById(@Param("id") Long id);
}
