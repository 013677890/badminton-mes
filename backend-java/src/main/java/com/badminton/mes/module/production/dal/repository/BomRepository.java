package com.badminton.mes.module.production.dal.repository;

import java.util.Optional;
import java.util.List;

import com.badminton.mes.module.production.dal.entity.BomEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * BOM 主表 JPA Repository，工单下达时校验 BOM 档案有效性。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface BomRepository extends JpaRepository<BomEntity, Long>,
        JpaSpecificationExecutor<BomEntity> {

    /**
     * 按主键查询未删除的 BOM。
     *
     * @param id BOM 主键
     * @return BOM 实体
     */
    Optional<BomEntity> findByIdAndDeletedFalse(Long id);

    /** 按主键写锁查询未删除 BOM。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bom FROM BomEntity bom WHERE bom.id = :id AND bom.deleted = false")
    Optional<BomEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /** 按主键升序锁定产品全部 BOM，串行化生效版本切换。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bom FROM BomEntity bom WHERE bom.productId = :productId "
            + "AND bom.deleted = false ORDER BY bom.id ASC")
    List<BomEntity> findByProductIdForUpdateOrderByIdAsc(@Param("productId") Long productId);

    /** 判断 BOM 编码是否被有效记录占用。 */
    boolean existsByBomCodeAndDeletedFalse(String bomCode);

    /** 判断 BOM 编码是否被其他有效记录占用。 */
    boolean existsByBomCodeAndIdNotAndDeletedFalse(String bomCode, Long id);

    /** 判断产品业务版本是否被有效记录占用。 */
    boolean existsByProductIdAndVersionAndDeletedFalse(Long productId, String version);

    /** 判断产品业务版本是否被其他有效记录占用。 */
    boolean existsByProductIdAndVersionAndIdNotAndDeletedFalse(Long productId, String version, Long id);

    /** 判断产品是否存在任意 BOM 引用。 */
    boolean existsByProductIdAndDeletedFalse(Long productId);

    /** 判断产品是否存在指定状态 BOM。 */
    boolean existsByProductIdAndBomStatusAndDeletedFalse(Long productId, Integer bomStatus);
}
