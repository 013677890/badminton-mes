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

/** 检验项目 Repository。 */
public interface QualityInspectionItemRepository extends JpaRepository<QualityInspectionItemEntity, Long>,
        JpaSpecificationExecutor<QualityInspectionItemEntity> {

    Optional<QualityInspectionItemEntity> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from QualityInspectionItemEntity item "
            + "where item.id = :id and item.deleted = false")
    Optional<QualityInspectionItemEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    boolean existsByItemCodeAndDeletedFalse(String itemCode);

    boolean existsByItemCodeAndIdNotAndDeletedFalse(String itemCode, Long id);

    boolean existsByItemCode(String itemCode);

    long countByCategoryIdAndDeletedFalse(Long categoryId);

    @Query("select item.id from QualityInspectionItemEntity item "
            + "where item.categoryId = :categoryId and item.deleted = false")
    List<Long> findIdsByCategoryIdAndDeletedFalse(@Param("categoryId") Long categoryId);
}
