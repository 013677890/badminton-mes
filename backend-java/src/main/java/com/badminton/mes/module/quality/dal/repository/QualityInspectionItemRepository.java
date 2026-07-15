package com.badminton.mes.module.quality.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 检验项目主数据持久化仓库。
 *
 * <p>常规读取与编码唯一性判断忽略逻辑删除行；分类引用统计和主键列表分别服务于删除约束检查与
 * 分类变更后的项目详情缓存级联失效。
 */
public interface QualityInspectionItemRepository extends JpaRepository<QualityInspectionItemEntity, Long>,
        JpaSpecificationExecutor<QualityInspectionItemEntity> {

    /** 按主键读取未删除项目，供详情展示及方案配置时的有效引用校验使用。 */
    Optional<QualityInspectionItemEntity> findByIdAndDeletedFalse(Long id);

    /** 对未删除项目施加悲观写锁，串行处理编辑、启停和逻辑删除。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from QualityInspectionItemEntity item "
            + "where item.id = :id and item.deleted = false")
    Optional<QualityInspectionItemEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /** 创建项目时，在有效主数据范围校验项目编码是否重复。 */
    boolean existsByItemCodeAndDeletedFalse(String itemCode);

    /** 更新项目时排除当前行，在其余未删除项目中校验目标编码。 */
    boolean existsByItemCodeAndIdNotAndDeletedFalse(String itemCode, Long id);

    /** 包含逻辑删除行检查占位编码是否碰撞，仅用于生成逻辑删除后的唯一编码。 */
    boolean existsByItemCode(String itemCode);

    /** 统计分类下未删除项目数量，用于阻止删除仍被有效项目引用的分类。 */
    long countByCategoryIdAndDeletedFalse(Long categoryId);

    /**
     * 查询分类下全部未删除项目主键。
     *
     * <p>分类名称或状态变化会影响项目详情的组合展示，调用方据此批量级联失效相关项目缓存。
     */
    @Query("select item.id from QualityInspectionItemEntity item "
            + "where item.categoryId = :categoryId and item.deleted = false")
    List<Long> findIdsByCategoryIdAndDeletedFalse(@Param("categoryId") Long categoryId);
}
