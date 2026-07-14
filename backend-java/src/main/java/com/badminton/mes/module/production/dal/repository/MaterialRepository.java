package com.badminton.mes.module.production.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.MaterialEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 物料 JPA Repository，工单物料需求展示时批量回查物料档案。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface MaterialRepository extends JpaRepository<MaterialEntity, Long>,
        JpaSpecificationExecutor<MaterialEntity> {

    /** 按主键查询未删除物料。 */
    Optional<MaterialEntity> findByIdAndDeletedFalse(Long id);

    /** 按主键写锁查询未删除物料。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT material FROM MaterialEntity material "
            + "WHERE material.id = :id AND material.deleted = false")
    Optional<MaterialEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /** 按主键升序锁定未删除物料。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT material FROM MaterialEntity material "
            + "WHERE material.id IN :ids AND material.deleted = false ORDER BY material.id ASC")
    List<MaterialEntity> findAllByIdInForUpdateOrderByIdAsc(@Param("ids") Collection<Long> ids);

    /** 判断有效物料编码是否存在。 */
    boolean existsByMaterialCodeAndDeletedFalse(String materialCode);

    /** 按物料编码查询未删除物料。 */
    Optional<MaterialEntity> findByMaterialCodeAndDeletedFalse(String materialCode);

    /** 判断有效物料编码是否存在，排除指定主键。 */
    boolean existsByMaterialCodeAndIdNotAndDeletedFalse(String materialCode, Long id);

    /**
     * 按主键集合批量查询未删除物料，用于物料需求明细的名称/编码回填。
     *
     * @param ids 物料主键集合，调用方保证非空且规模有限(单工单 BOM 明细数)
     * @return 物料列表
     */
    List<MaterialEntity> findByIdInAndDeletedFalse(Collection<Long> ids);
}
