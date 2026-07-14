package com.badminton.mes.module.production.dal.repository;

import java.util.List;

import com.badminton.mes.module.production.dal.entity.BomDetailEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BOM 明细 JPA Repository，工单下达时按明细生成物料需求。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
public interface BomDetailRepository extends JpaRepository<BomDetailEntity, Long> {

    /**
     * 查询指定 BOM 的全部未删除明细。
     *
     * @param bomId BOM 主键
     * @return 明细列表，无数据时为空集合
     */
    List<BomDetailEntity> findByBomIdAndDeletedFalse(Long bomId);

    /** 判断物料是否被任意有效 BOM 明细引用。 */
    boolean existsByMaterialIdAndDeletedFalse(Long materialId);

    /** 判断物料是否被生效 BOM 引用。 */
    @org.springframework.data.jpa.repository.Query("""
            SELECT CASE WHEN COUNT(detail) > 0 THEN true ELSE false END
            FROM BomDetailEntity detail, BomEntity bom
            WHERE bom.id = detail.bomId
              AND detail.materialId = :materialId
              AND bom.bomStatus = :bomStatus
              AND bom.deleted = false
              AND detail.deleted = false
            """)
    boolean existsEffectiveBomByMaterialId(
            @org.springframework.data.repository.query.Param("materialId") Long materialId,
            @org.springframework.data.repository.query.Param("bomStatus") Integer bomStatus);
}
