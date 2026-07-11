package com.badminton.mes.module.production.dal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.ProductEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 产品 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 按主键查询未删除的产品。
     *
     * @param id 产品主键
     * @return 产品实体
     */
    Optional<ProductEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 批量查询启用且未删除产品。
     *
     * @param ids    产品主键集合
     * @param status 启用状态
     * @return 可用产品列表
     */
    List<ProductEntity> findByIdInAndStatusAndDeletedFalse(Collection<Long> ids, Integer status);

    /**
     * 按主键升序写锁产品，保证默认路线切换串行化并统一锁顺序。
     *
     * @param ids 产品主键集合
     * @return 已锁定产品列表
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT product FROM ProductEntity product
            WHERE product.id IN :ids
              AND product.deleted = false
            ORDER BY product.id ASC
            """)
    List<ProductEntity> findAllByIdInForUpdateOrderByIdAsc(@Param("ids") Collection<Long> ids);
}
