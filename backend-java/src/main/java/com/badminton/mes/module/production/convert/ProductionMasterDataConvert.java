package com.badminton.mes.module.production.convert;

import java.util.List;
import java.util.Map;

import com.badminton.mes.module.production.controller.vo.BomDetailRespVO;
import com.badminton.mes.module.production.controller.vo.BomRespVO;
import com.badminton.mes.module.production.controller.vo.MaterialRespVO;
import com.badminton.mes.module.production.controller.vo.MaterialSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ProductRespVO;
import com.badminton.mes.module.production.controller.vo.ProductSaveReqVO;
import com.badminton.mes.module.production.dal.entity.BomDetailEntity;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;

/**
 * 产品、物料与 BOM 对象转换器。
 *
 * <p>转换器只做显式字段搬运，不负责格式校验、数据库查询或状态判断；这些规则由 Service 和支撑组件集中处理。
 */
public final class ProductionMasterDataConvert {

    /** 创建产品实体。 */
    public static ProductEntity toProductEntity(ProductSaveReqVO reqVO) {
        // 创建实体仅复制请求字段，审计字段和数据库默认字段由 Service/JPA 负责设置。
        ProductEntity entity = new ProductEntity();
        copyProduct(reqVO, entity);
        return entity;
    }

    /** 复制产品请求字段。 */
    public static void copyProduct(ProductSaveReqVO reqVO, ProductEntity entity) {
        // 显式覆盖产品业务字段，不触碰主键、版本号、创建时间和逻辑删除标记。
        entity.setProductCode(reqVO.getProductCode());
        entity.setProductName(reqVO.getProductName());
        entity.setSpec(reqVO.getSpec());
        entity.setProductType(reqVO.getProductType());
        entity.setGrade(reqVO.getGrade());
        entity.setUnitId(reqVO.getUnitId());
        entity.setStatus(reqVO.getStatus());
    }

    /** 转换产品响应。 */
    public static ProductRespVO toProductRespVO(ProductEntity entity) {
        // 响应只暴露主档展示字段和并发版本，不返回内部 JPA 状态。
        ProductRespVO result = new ProductRespVO();
        result.setId(entity.getId());
        result.setProductCode(entity.getProductCode());
        result.setProductName(entity.getProductName());
        result.setSpec(entity.getSpec());
        result.setProductType(entity.getProductType());
        result.setGrade(entity.getGrade());
        result.setUnitId(entity.getUnitId());
        result.setStatus(entity.getStatus());
        result.setVersion(entity.getVersion());
        result.setCreateTime(entity.getCreateTime());
        result.setUpdateTime(entity.getUpdateTime());
        return result;
    }

    /** 创建物料实体。 */
    public static MaterialEntity toMaterialEntity(MaterialSaveReqVO reqVO) {
        // 创建物料实体由 Service 先完成请求规范化后调用，转换器不重复修改输入值。
        MaterialEntity entity = new MaterialEntity();
        copyMaterial(reqVO, entity);
        return entity;
    }

    /** 复制物料请求字段。 */
    public static void copyMaterial(MaterialSaveReqVO reqVO, MaterialEntity entity) {
        // 更新只覆盖物料主档业务字段，保留数据库主键和乐观锁字段。
        entity.setMaterialCode(reqVO.getMaterialCode());
        entity.setMaterialName(reqVO.getMaterialName());
        entity.setSpec(reqVO.getSpec());
        entity.setMaterialType(reqVO.getMaterialType());
        entity.setUnitId(reqVO.getUnitId());
        entity.setKeyMaterial(reqVO.getKeyMaterial());
        entity.setStatus(reqVO.getStatus());
    }

    /** 转换物料响应。 */
    public static MaterialRespVO toMaterialRespVO(MaterialEntity entity) {
        // 物料详情直接映射主档字段，引用关系和状态含义由调用方负责校验。
        MaterialRespVO result = new MaterialRespVO();
        result.setId(entity.getId());
        result.setMaterialCode(entity.getMaterialCode());
        result.setMaterialName(entity.getMaterialName());
        result.setSpec(entity.getSpec());
        result.setMaterialType(entity.getMaterialType());
        result.setUnitId(entity.getUnitId());
        result.setKeyMaterial(entity.getKeyMaterial());
        result.setStatus(entity.getStatus());
        result.setVersion(entity.getVersion());
        result.setCreateTime(entity.getCreateTime());
        result.setUpdateTime(entity.getUpdateTime());
        return result;
    }

    /** 转换 BOM 聚合响应。 */
    public static BomRespVO toBomRespVO(BomEntity bom, ProductEntity product,
                                         List<BomDetailEntity> details,
                                         Map<Long, MaterialEntity> materialMap) {
        // 先组装主表摘要，再逐条回填物料展示信息；缺失的历史档案保留空名称而不丢弃明细。
        BomRespVO result = toBomSummaryRespVO(bom, product);
        result.setDetails(details.stream().map(detail -> toBomDetailRespVO(
                detail, materialMap.get(detail.getMaterialId()))).toList());
        return result;
    }

    /** 转换不含明细的 BOM 列表响应。 */
    public static BomRespVO toBomSummaryRespVO(BomEntity bom, ProductEntity product) {
        // 列表场景只组装主表摘要，不加载 BOM 明细，避免分页查询放大数据量。
        BomRespVO result = new BomRespVO();
        result.setId(bom.getId());
        result.setBomCode(bom.getBomCode());
        result.setProductId(bom.getProductId());
        if (product != null) {
            result.setProductCode(product.getProductCode());
            result.setProductName(product.getProductName());
        }
        result.setVersion(bom.getVersion());
        result.setBomStatus(bom.getBomStatus());
        result.setLockVersion(bom.getLockVersion());
        result.setDetails(List.of());
        result.setCreateTime(bom.getCreateTime());
        result.setUpdateTime(bom.getUpdateTime());
        return result;
    }

    /** 转换 BOM 明细响应。 */
    private static BomDetailRespVO toBomDetailRespVO(BomDetailEntity detail, MaterialEntity material) {
        // 明细数量和损耗率来自 BOM 快照，物料编码和名称来自可选的历史主档回填。
        BomDetailRespVO result = new BomDetailRespVO();
        result.setId(detail.getId());
        result.setMaterialId(detail.getMaterialId());
        if (material != null) {
            result.setMaterialCode(material.getMaterialCode());
            result.setMaterialName(material.getMaterialName());
        }
        result.setQuantity(detail.getQuantity());
        result.setLossRate(detail.getLossRate());
        return result;
    }

    private ProductionMasterDataConvert() {
        // 工具类不允许实例化。
    }
}
