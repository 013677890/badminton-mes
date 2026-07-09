package com.badminton.mes.module.production.dal.repository;

import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.BomEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BOM 主表 JPA Repository，工单下达时校验 BOM 档案有效性。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface BomRepository extends JpaRepository<BomEntity, Long> {

    /**
     * 按主键查询未删除的 BOM。
     *
     * @param id BOM 主键
     * @return BOM 实体
     */
    Optional<BomEntity> findByIdAndDeletedFalse(Long id);
}
