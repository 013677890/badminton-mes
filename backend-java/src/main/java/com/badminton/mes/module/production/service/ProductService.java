package com.badminton.mes.module.production.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.ProductPageReqVO;
import com.badminton.mes.module.production.controller.vo.ProductRespVO;
import com.badminton.mes.module.production.controller.vo.ProductSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ProductUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;

/** 产品主档服务。 */
public interface ProductService {
    /**
     * 创建产品。
     *
     * @param reqVO 产品创建请求
     * @return 新产品主键
     */
    Long createProduct(ProductSaveReqVO reqVO);
    /**
     * 修改产品。
     *
     * @param id 产品主键
     * @param reqVO 产品修改请求
     */
    void updateProduct(Long id, ProductUpdateReqVO reqVO);
    /**
     * 删除产品。
     *
     * @param id 产品主键
     * @param version 客户端预期版本
     */
    void deleteProduct(Long id, Integer version);
    /**
     * 启用或停用产品。
     *
     * @param id 产品主键
     * @param reqVO 状态变更请求
     */
    void updateProductStatus(Long id, ProductionStatusReqVO reqVO);
    /**
     * 查询产品详情。
     *
     * @param id 产品主键
     * @return 产品详情
     */
    ProductRespVO getProduct(Long id);
    /**
     * 分页查询产品。
     *
     * @param reqVO 分页筛选条件
     * @return 产品分页结果
     */
    PageResult<ProductRespVO> getProductPage(ProductPageReqVO reqVO);
}
