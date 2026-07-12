package com.badminton.mes.module.production.dal.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.production.dal.entity.FactoryCalendarEntity;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工厂日历 JPA Repository，派工校验工作日，只读。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public interface FactoryCalendarRepository extends JpaRepository<FactoryCalendarEntity, Long> {

    /**
     * 查询车间某日的日历记录；无记录按工作日处理(简化约定)。
     *
     * @param workshopId   车间主键
     * @param calendarDate 日期
     * @return 日历记录
     */
    Optional<FactoryCalendarEntity> findByWorkshopIdAndCalendarDateAndDeletedFalse(
            Long workshopId, LocalDate calendarDate);

    /**
     * 查询车间日期区间内的日历记录，排产建议过滤非工作日。
     *
     * @param workshopId 车间主键
     * @param startDate  起始日期(含)
     * @param endDate    结束日期(含)
     * @return 日历记录列表，无数据时为空集合
     */
    List<FactoryCalendarEntity> findByWorkshopIdAndCalendarDateBetweenAndDeletedFalse(
            Long workshopId, LocalDate startDate, LocalDate endDate);

    /**
     * 判断车间是否存在未删除日历记录。
     *
     * @param workshopId 车间主键
     * @return true 表示存在日历引用
     */
    boolean existsByWorkshopIdAndDeletedFalse(Long workshopId);
}
