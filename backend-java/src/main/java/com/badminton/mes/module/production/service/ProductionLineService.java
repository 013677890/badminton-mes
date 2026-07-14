package com.badminton.mes.module.production.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.ProductionLinePageReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineRespVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;

/**
 * 产线基础资料服务。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
public interface ProductionLineService {

    /**
     * 创建产线。
     *
     * @param reqVO 创建请求
     * @return 新产线主键
     */
    Long createProductionLine(ProductionLineSaveReqVO reqVO);

    /**
     * 按客户端预期版本修改产线。
     *
     * @param id 产线主键
     * @param reqVO 修改请求
     */
    void updateProductionLine(Long id, ProductionLineUpdateReqVO reqVO);

    /**
     * 删除无业务引用的产线。
     *
     * @param id 产线主键
     * @param version 客户端预期版本
     */
    void deleteProductionLine(Long id, Integer version);

    /**
     * 启用或停用产线。
     *
     * @param id 产线主键
     * @param reqVO 状态变更请求
     */
    void updateProductionLineStatus(Long id, ProductionStatusReqVO reqVO);

    /**
     * 查询产线详情。
     *
     * @param id 产线主键
     * @return 产线详情
     */
    ProductionLineRespVO getProductionLine(Long id);

    /**
     * 分页查询产线。
     *
     * @param reqVO 分页筛选条件
     * @return 产线分页结果
     */
    PageResult<ProductionLineRespVO> getProductionLinePage(ProductionLinePageReqVO reqVO);
}
