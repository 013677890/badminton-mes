package com.badminton.mes.module.barcode.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.barcode.dal.entity.MaterialRefEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 物料只读引用 Repository，B 组按协作边界直接只读查询 A 组 base_material。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface MaterialRefRepository extends JpaRepository<MaterialRefEntity, Long> {

    /**
     * 按主键查询未删除的物料。
     *
     * @param id 物料主键
     * @return 物料只读引用
     */
    Optional<MaterialRefEntity> findByIdAndDeletedFalse(Long id);
}
