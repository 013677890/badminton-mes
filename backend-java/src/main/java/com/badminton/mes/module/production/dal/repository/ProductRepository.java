package com.badminton.mes.module.production.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.ProductEntity;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
