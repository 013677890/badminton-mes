package com.badminton.mes.module.andon.dal.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.badminton.mes.module.andon.dal.entity.AndonEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * 现场安灯异常数据访问接口。
 *
 * <p>除常规逻辑删除过滤外，还提供状态流转所需的悲观写锁、类型变更时的级联事件 ID 查询，
 * 以及定时任务使用的未关闭超时候选查询。
 */
public interface AndonEventRepository extends JpaRepository<AndonEventEntity, Long>,
        JpaSpecificationExecutor<AndonEventEntity> {

    /** 按主键读取未逻辑删除的事件，供详情展示等只读场景使用。 */
    Optional<AndonEventEntity> findByIdAndDeletedFalse(Long id);

    /** 按业务单号读取活动事件，用于业务单号定位和重复校验后的回查。 */
    Optional<AndonEventEntity> findByEventNoAndDeletedFalse(String eventNo);

    /** 检查业务单号是否曾被占用，包含逻辑删除数据以避免复用历史事件编号。 */
    boolean existsByEventNo(String eventNo);

    /**
     * 查询指定安灯类型下所有未删除事件的主键。
     *
     * <p>仅投影 ID，供类型信息变更后批量级联失效事件聚合详情缓存，避免加载完整事件实体。
     */
    @Query("select event.id from AndonEventEntity event "
            + "where event.andonTypeId = :andonTypeId and event.deleted = false")
    List<Long> findIdsByAndonTypeIdAndDeletedFalse(@Param("andonTypeId") Long andonTypeId);

    /**
     * 在当前数据库事务中以悲观写锁读取活动事件。
     *
     * <p>状态迁移、转派和完成等并发写操作必须串行化，防止多个请求基于同一旧状态重复推进流程。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from AndonEventEntity event "
            + "where event.id = :id and event.deleted = false")
    Optional<AndonEventEntity> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * 查询截至指定时间可能需要响应超时或升级处理的活动事件。
     *
     * <p>候选范围排除已删除和已关闭事件，只要响应截止时间或升级截止时间任一到期即入选；
     * 具体是否需要更新超时状态或发送通知，仍由调度服务结合事件当前字段判定。
     */
    @Query("select event from AndonEventEntity event "
            + "where event.deleted = false "
            + "and event.eventStatus <> 'CLOSED' "
            + "and ((event.responseDeadline is not null and event.responseDeadline <= :now) "
            + "or (event.escalationDeadline is not null and event.escalationDeadline <= :now))")
    List<AndonEventEntity> findTimeoutCandidates(@Param("now") LocalDateTime now);
}
