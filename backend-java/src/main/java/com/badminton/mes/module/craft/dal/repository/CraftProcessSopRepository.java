package com.badminton.mes.module.craft.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.craft.dal.entity.CraftProcessSopEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工序 SOP JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftProcessSopRepository extends JpaRepository<CraftProcessSopEntity, Long> {

    /**
     * 查询工序的 SOP 列表。
     *
     * @param processId 工序主键
     * @return SOP 列表
     */
    List<CraftProcessSopEntity> findByProcessIdAndDeletedFalseOrderByIdAsc(Long processId);

    /**
     * 查询工序下指定 SOP。
     *
     * @param id        SOP 主键
     * @param processId 工序主键
     * @return SOP 实体
     */
    Optional<CraftProcessSopEntity> findByIdAndProcessIdAndDeletedFalse(Long id, Long processId);

    /**
     * 判断同工序 SOP 编码是否重复。
     *
     * @param processId 工序主键
     * @param sopCode   SOP 编码
     * @return true 重复
     */
    boolean existsByProcessIdAndSopCodeAndDeletedFalse(Long processId, String sopCode);

    /**
     * 判断同工序 SOP 编码是否重复，排除指定记录。
     *
     * @param processId 工序主键
     * @param sopCode   SOP 编码
     * @param id        排除的 SOP 主键
     * @return true 重复
     */
    boolean existsByProcessIdAndSopCodeAndIdNotAndDeletedFalse(Long processId, String sopCode, Long id);

    /**
     * 判断工序是否仍存在未删除 SOP。
     *
     * @param processId 工序主键
     * @return true 表示存在未删除 SOP
     */
    boolean existsByProcessIdAndDeletedFalse(Long processId);
}
