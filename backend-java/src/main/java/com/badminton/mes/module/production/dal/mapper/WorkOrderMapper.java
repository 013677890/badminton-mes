package com.badminton.mes.module.production.dal.mapper;

import java.util.List;

import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.dal.dataobject.WorkOrderDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 生产工单 Mapper，只负责 prod_work_order 表的数据库访问，SQL 见 WorkOrderMapper.xml。
 *
 * <p>DAO 层与 Service 层同应用部署，参数已由上层校验，本层不再重复校验(FLOW-014)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Mapper
public interface WorkOrderMapper {

    /**
     * 插入生产工单，插入成功后自增主键回填到 entity.id。
     *
     * @param entity 工单数据对象
     * @return 影响行数
     */
    int insert(WorkOrderDO entity);

    /**
     * 按主键查询未删除的工单。
     *
     * @param id 工单主键
     * @return 工单数据；不存在或已逻辑删除时返回 null
     */
    WorkOrderDO selectById(@Param("id") Long id);

    /**
     * 按工单号查询未删除的工单，用于创建时的唯一性校验。
     *
     * @param workOrderNo 工单号
     * @return 工单数据；不存在时返回 null
     */
    WorkOrderDO selectByWorkOrderNo(@Param("workOrderNo") String workOrderNo);

    /**
     * 统计满足分页条件的工单总数。
     *
     * @param reqVO 分页筛选条件
     * @return 总记录数
     */
    long selectPageCount(@Param("reqVO") WorkOrderPageReqVO reqVO);

    /**
     * 查询工单分页列表，按 id 倒序保证分页顺序确定。
     *
     * @param reqVO  分页筛选条件
     * @param offset 偏移量，(pageNo - 1) * pageSize
     * @param limit  取行数，即 pageSize
     * @return 当前页工单列表；无数据时返回空集合
     */
    List<WorkOrderDO> selectPageList(@Param("reqVO") WorkOrderPageReqVO reqVO,
                                     @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按主键更新工单计划信息，只更新 entity 中非 null 的目标字段(ORM-008)。
     *
     * <p>SQL 带 {@code order_status = 0(已创建)} 条件，与状态校验形成 CAS，
     * 防止"先查后改"间隙内工单被并发下达后计划仍被覆盖。
     *
     * @param entity 工单数据对象，id 必填
     * @return 影响行数；为 0 说明工单不存在、已删除或状态已变化
     */
    int updateById(WorkOrderDO entity);

    /**
     * 下达工单：已创建 → 已下达 的 CAS 状态机更新。
     *
     * <p>SQL 同时要求 bom_id 与 routing_id 非空(未维护 BOM/工艺路线不允许下达)；
     * 影响行数为 0 时由 Service 查明具体原因后给出精确提示。
     *
     * @param id 工单主键
     * @return 影响行数；1 下达成功，0 条件不满足
     */
    int updateToReleased(@Param("id") Long id);

    /**
     * 逻辑删除工单(TABLE-010 不允许物理删除)，仅"已创建"状态允许删除。
     *
     * <p>注意：逻辑删除后 uk_work_order_no 唯一索引仍占用该工单号，
     * 业务上单号不复用，属预期行为。
     *
     * @param id 工单主键
     * @return 影响行数；为 0 说明工单不存在或状态不允许删除
     */
    int deleteById(@Param("id") Long id);
}
