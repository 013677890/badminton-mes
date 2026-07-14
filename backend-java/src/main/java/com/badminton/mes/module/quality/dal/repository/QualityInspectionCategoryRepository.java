package com.badminton.mes.module.quality.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 检验分类 Repository。 */
public interface QualityInspectionCategoryRepository
        extends JpaRepository<QualityInspectionCategoryEntity, Long>,
        JpaSpecificationExecutor<QualityInspectionCategoryEntity> {

    Optional<QualityInspectionCategoryEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select category from QualityInspectionCategoryEntity category "
            + "where category.id = :id and category.deleted = false")
    Optional<QualityInspectionCategoryEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    boolean existsByCategoryCodeAndDeletedFalse(String categoryCode);

    boolean existsByCategoryCodeAndIdNotAndDeletedFalse(String categoryCode, Long id);

    boolean existsByCategoryCode(String categoryCode);
}
