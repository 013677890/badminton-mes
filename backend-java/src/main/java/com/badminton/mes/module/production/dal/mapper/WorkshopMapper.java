package com.badminton.mes.module.production.dal.mapper;

import com.badminton.mes.module.production.dal.dataobject.WorkshopDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 车间 Mapper(生产订单模块内部使用)。
 *
 * <p>基础资料模块建设后应迁移到对应模块，生产订单模块改为依赖其
 * Service 契约而非直接访问对方 Mapper。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Mapper
public interface WorkshopMapper {

    /**
     * 按主键查询未删除的车间，用于创建工单时的存在性校验。
     *
     * @param id 车间主键
     * @return 车间数据；不存在或已逻辑删除时返回 null
     */
    WorkshopDO selectById(@Param("id") Long id);
}
