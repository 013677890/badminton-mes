package com.badminton.mes.module.production.dal.repository;

import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.ShiftEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 班次 JPA Repository，派工时校验班次可用性，只读。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface ShiftRepository extends JpaRepository<ShiftEntity, Long> {

    /**
     * 按主键查询未删除班次。
     *
     * @param id 班次主键
     * @return 班次实体
     */
    Optional<ShiftEntity> findByIdAndDeletedFalse(Long id);

    /**
     * 查询全部启用班次，排产建议候选与班次产能均摊分母。
     *
     * @param status 状态(传启用)
     * @return 班次列表，无数据时为空集合
     */
    List<ShiftEntity> findByStatusAndDeletedFalseOrderByIdAsc(Integer status);

    /**
     * 按班次编码查询未删除班次，外部任务单写入时解析编码用。
     *
     * @param shiftCode 班次编码
     * @return 班次实体
     */
    Optional<ShiftEntity> findByShiftCodeAndDeletedFalse(String shiftCode);
}
