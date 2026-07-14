package com.badminton.mes.module.production.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.MaterialPageReqVO;
import com.badminton.mes.module.production.controller.vo.MaterialRespVO;
import com.badminton.mes.module.production.controller.vo.MaterialSaveReqVO;
import com.badminton.mes.module.production.controller.vo.MaterialUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;

/** 物料主档服务。 */
public interface MaterialService {
    /**
     * 创建物料。
     *
     * @param reqVO 物料创建请求
     * @return 新物料主键
     */
    Long createMaterial(MaterialSaveReqVO reqVO);
    /**
     * 修改物料。
     *
     * @param id 物料主键
     * @param reqVO 物料修改请求
     */
    void updateMaterial(Long id, MaterialUpdateReqVO reqVO);
    /**
     * 删除物料。
     *
     * @param id 物料主键
     * @param version 客户端预期版本
     */
    void deleteMaterial(Long id, Integer version);
    /**
     * 启用或停用物料。
     *
     * @param id 物料主键
     * @param reqVO 状态变更请求
     */
    void updateMaterialStatus(Long id, ProductionStatusReqVO reqVO);
    /**
     * 查询物料详情。
     *
     * @param id 物料主键
     * @return 物料详情
     */
    MaterialRespVO getMaterial(Long id);
    /**
     * 分页查询物料。
     *
     * @param reqVO 分页筛选条件
     * @return 物料分页结果
     */
    PageResult<MaterialRespVO> getMaterialPage(MaterialPageReqVO reqVO);
}
