package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;

/**
 * 设备类别 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏，
 * 也避免反射拷贝的性能损耗与浅拷贝陷阱。这里只表达类别节点自身的字段边界，不遍历父子树，
 * 也不校验循环引用或跨模块引用；层级一致性必须在 Service 的事务与锁保护下完成。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentCategoryConvert {

    /**
     * 保存请求 VO 转实体，创建与修改共用。
     *
     * <p>只搬运类别编码、名称、父级、排序及状态等节点字段。主键、审计信息和逻辑删除标记不接受
     * 客户端赋值，父级有效性及环检测也不在转换阶段执行。
     *
     * @param reqVO 保存请求 VO
     * @return 设备类别实体
     */
    public static EquipmentCategoryEntity toEntity(EquipmentCategorySaveReqVO reqVO) {
        EquipmentCategoryEntity category = new EquipmentCategoryEntity();
        category.setCategoryCode(reqVO.getCategoryCode());
        category.setCategoryName(reqVO.getCategoryName());
        category.setParentId(reqVO.getParentId());
        category.setSortOrder(reqVO.getSortOrder());
        category.setRemark(reqVO.getRemark());
        category.setStatus(reqVO.getStatus());
        return category;
    }

    /**
     * 实体转响应 VO。
     *
     * <p>输出单个类别节点的持久化快照，包括父级主键而不递归展开树，避免列表和缓存响应因层级
     * 深度产生隐式查询；调用方可据此自行组装树结构。
     *
     * @param category 设备类别实体
     * @return 响应 VO
     */
    public static EquipmentCategoryRespVO toRespVO(EquipmentCategoryEntity category) {
        EquipmentCategoryRespVO respVO = new EquipmentCategoryRespVO();
        respVO.setId(category.getId());
        respVO.setCategoryCode(category.getCategoryCode());
        respVO.setCategoryName(category.getCategoryName());
        respVO.setParentId(category.getParentId());
        respVO.setSortOrder(category.getSortOrder());
        respVO.setRemark(category.getRemark());
        respVO.setStatus(category.getStatus());
        respVO.setCreateTime(category.getCreateTime());
        respVO.setUpdateTime(category.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转响应 VO 列表。
     *
     * @param list 设备类别实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<EquipmentCategoryRespVO> toRespVOList(List<EquipmentCategoryEntity> list) {
        return list.stream().map(EquipmentCategoryConvert::toRespVO).toList();
    }

    /** 工具类仅提供无状态字段转换，禁止实例化。 */
    private EquipmentCategoryConvert() {
    }
}
