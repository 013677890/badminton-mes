package com.badminton.mes.module.quality.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 检验分类持久化仓库。
 *
 * <p>详情读取、编辑锁定和业务唯一性校验均以未逻辑删除数据为主；不带删除过滤的编码查询只用于
 * 校验逻辑删除后生成的占位编码，防止占位值再次碰撞数据库唯一约束。
 */
public interface QualityInspectionCategoryRepository
        extends JpaRepository<QualityInspectionCategoryEntity, Long>,
        JpaSpecificationExecutor<QualityInspectionCategoryEntity> {

    /** 按主键读取未删除分类，供详情展示和无须加锁的引用校验使用。 */
    Optional<QualityInspectionCategoryEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 按主键读取未删除分类并施加悲观写锁。
     *
     * <p>编辑、启停和删除事务通过该锁串行修改同一分类，避免并发状态覆盖或删除检查与实际写入脱节。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select category from QualityInspectionCategoryEntity category "
            + "where category.id = :id and category.deleted = false")
    Optional<QualityInspectionCategoryEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /** 校验未删除主数据中是否已有相同分类编码，用于创建时的业务唯一性检查。 */
    boolean existsByCategoryCodeAndDeletedFalse(String categoryCode);

    /** 更新分类时排除当前行，校验其他未删除分类是否占用目标编码。 */
    boolean existsByCategoryCodeAndIdNotAndDeletedFalse(String categoryCode, Long id);

    /**
     * 在包含逻辑删除行的全表范围检查编码。
     *
     * <p>仅用于为待删除行生成唯一占位编码，不能替代面向有效主数据的编码重复校验。
     */
    boolean existsByCategoryCode(String categoryCode);
}
