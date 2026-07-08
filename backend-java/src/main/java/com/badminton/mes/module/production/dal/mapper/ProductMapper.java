package com.badminton.mes.module.production.dal.mapper;

import com.badminton.mes.module.production.dal.dataobject.ProductDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 产品 Mapper(生产订单模块内部使用)。
 *
 * <p>基础资料模块建设后应迁移到对应模块，生产订单模块改为依赖其
 * Service 契约而非直接访问对方 Mapper。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Mapper
public interface ProductMapper {

    /**
     * 按主键查询未删除的产品，用于创建工单时的存在性校验与冗余字段回填。
     *
     * @param id 产品主键
     * @return 产品数据；不存在或已逻辑删除时返回 null
     */
    ProductDO selectById(@Param("id") Long id);
}
