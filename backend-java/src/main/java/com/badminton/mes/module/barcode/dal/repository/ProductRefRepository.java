package com.badminton.mes.module.barcode.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.ProductRefEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 产品只读引用 Repository，B 组按协作边界直接只读查询 A 组 base_product。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface ProductRefRepository extends JpaRepository<ProductRefEntity, Long> {

    /**
     * 按主键查询未删除的产品。
     *
     * @param id 产品主键
     * @return 产品只读引用
     */
    Optional<ProductRefEntity> findByIdAndDeletedFalse(Long id);
}
