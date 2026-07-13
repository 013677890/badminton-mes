package com.badminton.mes.module.craft.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.craft.dal.entity.CraftProcessDefectReasonEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工序不良原因 JPA Repository。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftProcessDefectReasonRepository
        extends JpaRepository<CraftProcessDefectReasonEntity, Long> {

    /**
     * 查询工序的可选不良原因。
     *
     * @param processId 工序主键
     * @return 不良原因列表
     */
    List<CraftProcessDefectReasonEntity> findByProcessIdAndDeletedFalseOrderByIdAsc(Long processId);

    /**
     * 查询工序下指定不良原因。
     *
     * @param id        不良原因主键
     * @param processId 工序主键
     * @return 不良原因实体
     */
    Optional<CraftProcessDefectReasonEntity> findByIdAndProcessIdAndDeletedFalse(Long id, Long processId);

    /**
     * 判断同工序不良原因编码是否重复。
     *
     * @param processId 工序主键
     * @param reasonCode 不良原因编码
     * @return true 重复
     */
    boolean existsByProcessIdAndReasonCodeAndDeletedFalse(Long processId, String reasonCode);

    /**
     * 判断同工序不良原因编码是否重复，排除指定记录。
     *
     * @param processId 工序主键
     * @param reasonCode 不良原因编码
     * @param id 排除的不良原因主键
     * @return true 重复
     */
    boolean existsByProcessIdAndReasonCodeAndIdNotAndDeletedFalse(
            Long processId, String reasonCode, Long id);

    /**
     * 判断工序是否仍存在未删除不良原因。
     *
     * @param processId 工序主键
     * @return true 表示存在未删除不良原因
     */
    boolean existsByProcessIdAndDeletedFalse(Long processId);
}
