package com.badminton.mes.module.barcode.dal.repository;

import java.util.List;

import com.badminton.mes.module.barcode.dal.entity.BarcodeRuleItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 条码规则组成明细 JPA Repository。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeRuleItemRepository extends JpaRepository<BarcodeRuleItemEntity, Long> {

    /**
     * 查询规则的未删除组成明细，按组成顺序升序。
     *
     * @param ruleId 规则主键
     * @return 组成明细列表
     */
    List<BarcodeRuleItemEntity> findByRuleIdAndDeletedFalseOrderBySeqAsc(Long ruleId);

    /**
     * 按规则逻辑删除全部组成明细，规则修改重写明细或规则删除时使用。
     *
     * @param ruleId 规则主键
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeRuleItemEntity item
            SET item.deleted = true,
                item.updateTime = CURRENT_TIMESTAMP
            WHERE item.ruleId = :ruleId
              AND item.deleted = false
            """)
    int logicDeleteByRuleId(@Param("ruleId") Long ruleId);
}
