package com.badminton.mes.module.barcode.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.BarcodeApplyRuleEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 条码应用规则 JPA Repository。
 *
 * <p>"同对象同类型仅一条启用默认规则"由应用层预检 + 数据库生成列唯一索引
 * uk_active_default 共同保证；状态流转与逻辑删除使用 CAS 更新。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeApplyRuleRepository extends JpaRepository<BarcodeApplyRuleEntity, Long>,
        JpaSpecificationExecutor<BarcodeApplyRuleEntity> {

    /**
     * 按主键查询未删除的应用规则。
     *
     * @param id 应用规则主键
     * @return 应用规则实体
     */
    Optional<BarcodeApplyRuleEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 判断未删除应用规则中是否存在引用指定条码类型的记录，类型删除前校验。
     *
     * @param barcodeTypeId 条码类型主键
     * @return true 存在引用，false 无引用
     */
    boolean existsByBarcodeTypeIdAndDeletedFalse(Long barcodeTypeId);

    /**
     * 判断未删除应用规则中是否存在引用指定条码规则的记录，规则删除前校验。
     *
     * @param ruleId 条码规则主键
     * @return true 存在引用，false 无引用
     */
    boolean existsByRuleIdAndDeletedFalse(Long ruleId);

    /**
     * 判断未删除应用规则中是否存在引用指定模板的记录，模板修改升版本判定。
     *
     * @param templateId 模板主键(具体版本行)
     * @return true 存在引用，false 无引用
     */
    boolean existsByTemplateIdAndDeletedFalse(Long templateId);

    /**
     * 统计同对象同类型已存在的"启用 + 默认"规则数，创建/修改/启用前预检；
     * 并发窗口由生成列唯一索引 uk_active_default 兜底。
     *
     * @param objectType    对象类型
     * @param objectId      产品或物料 id
     * @param barcodeTypeId 条码类型 id
     * @param excludeId     排除的应用规则主键，创建时传 null
     * @param enabledStatus 启用状态值
     * @return 满足条件的记录数
     */
    @Query("""
            SELECT COUNT(applyRule) FROM BarcodeApplyRuleEntity applyRule
            WHERE applyRule.objectType = :objectType
              AND applyRule.barcodeTypeId = :barcodeTypeId
              AND applyRule.defaultFlag = true
              AND applyRule.status = :enabledStatus
              AND applyRule.deleted = false
              AND (applyRule.productId = :objectId OR applyRule.materialId = :objectId)
              AND (:excludeId IS NULL OR applyRule.id <> :excludeId)
            """)
    long countActiveDefault(@Param("objectType") Integer objectType,
                            @Param("objectId") Long objectId,
                            @Param("barcodeTypeId") Long barcodeTypeId,
                            @Param("excludeId") Long excludeId,
                            @Param("enabledStatus") Integer enabledStatus);

    /**
     * 查询生成条码时可用的启用应用规则选项，默认规则在前。
     *
     * @param objectType    对象类型，可空
     * @param productId     产品 id，可空
     * @param materialId    物料 id，可空
     * @param barcodeTypeId 条码类型 id，可空
     * @param enabledStatus 启用状态值
     * @return 启用应用规则列表
     */
    @Query("""
            SELECT applyRule FROM BarcodeApplyRuleEntity applyRule
            WHERE applyRule.status = :enabledStatus
              AND applyRule.deleted = false
              AND (:objectType IS NULL OR applyRule.objectType = :objectType)
              AND (:productId IS NULL OR applyRule.productId = :productId)
              AND (:materialId IS NULL OR applyRule.materialId = :materialId)
              AND (:barcodeTypeId IS NULL OR applyRule.barcodeTypeId = :barcodeTypeId)
            ORDER BY applyRule.defaultFlag DESC, applyRule.id ASC
            """)
    List<BarcodeApplyRuleEntity> findEnabledOptions(@Param("objectType") Integer objectType,
                                                    @Param("productId") Long productId,
                                                    @Param("materialId") Long materialId,
                                                    @Param("barcodeTypeId") Long barcodeTypeId,
                                                    @Param("enabledStatus") Integer enabledStatus);

    /**
     * 修改应用规则配置，deleted = false 条件构成 CAS，防止并发删除后误复活。
     *
     * @return 影响行数；0 表示应用规则不存在或已删除
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeApplyRuleEntity applyRule
            SET applyRule.objectType = :objectType,
                applyRule.productId = :productId,
                applyRule.materialId = :materialId,
                applyRule.barcodeTypeId = :barcodeTypeId,
                applyRule.barcodeMode = :barcodeMode,
                applyRule.ruleId = :ruleId,
                applyRule.templateId = :templateId,
                applyRule.sourceType = :sourceType,
                applyRule.defaultFlag = :defaultFlag,
                applyRule.updateTime = CURRENT_TIMESTAMP
            WHERE applyRule.id = :id
              AND applyRule.deleted = false
            """)
    int updateInfo(@Param("id") Long id,
                   @Param("objectType") Integer objectType,
                   @Param("productId") Long productId,
                   @Param("materialId") Long materialId,
                   @Param("barcodeTypeId") Long barcodeTypeId,
                   @Param("barcodeMode") Integer barcodeMode,
                   @Param("ruleId") Long ruleId,
                   @Param("templateId") Long templateId,
                   @Param("sourceType") Integer sourceType,
                   @Param("defaultFlag") Boolean defaultFlag);

    /**
     * 状态流转 CAS：仅当当前状态等于前置状态才更新，用于启用/停用。
     *
     * @param id         应用规则主键
     * @param fromStatus 前置状态
     * @param toStatus   目标状态
     * @return 影响行数；0 表示应用规则不存在、已删除或状态不满足
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeApplyRuleEntity applyRule
            SET applyRule.status = :toStatus,
                applyRule.updateTime = CURRENT_TIMESTAMP
            WHERE applyRule.id = :id
              AND applyRule.status = :fromStatus
              AND applyRule.deleted = false
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") Integer fromStatus,
                     @Param("toStatus") Integer toStatus);

    /**
     * 逻辑删除，deleted = false 条件构成 CAS，防止重复删除。
     *
     * @param id 应用规则主键
     * @return 影响行数；0 表示应用规则不存在或已删除
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BarcodeApplyRuleEntity applyRule
            SET applyRule.deleted = true,
                applyRule.updateTime = CURRENT_TIMESTAMP
            WHERE applyRule.id = :id
              AND applyRule.deleted = false
            """)
    int logicDeleteById(@Param("id") Long id);
}
