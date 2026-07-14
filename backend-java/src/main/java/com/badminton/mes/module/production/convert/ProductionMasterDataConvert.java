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

/** 产品、物料与 BOM 对象转换器。 */
public final class ProductionMasterDataConvert {

    /** 创建产品实体。 */
    public static ProductEntity toProductEntity(ProductSaveReqVO reqVO) {
        ProductEntity entity = new ProductEntity();
        copyProduct(reqVO, entity);
        return entity;
    }

    /** 复制产品请求字段。 */
    public static void copyProduct(ProductSaveReqVO reqVO, ProductEntity entity) {
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
        MaterialEntity entity = new MaterialEntity();
        copyMaterial(reqVO, entity);
        return entity;
    }

    /** 复制物料请求字段。 */
    public static void copyMaterial(MaterialSaveReqVO reqVO, MaterialEntity entity) {
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
        BomRespVO result = toBomSummaryRespVO(bom, product);
        result.setDetails(details.stream().map(detail -> toBomDetailRespVO(
                detail, materialMap.get(detail.getMaterialId()))).toList());
        return result;
    }

    /** 转换不含明细的 BOM 列表响应。 */
    public static BomRespVO toBomSummaryRespVO(BomEntity bom, ProductEntity product) {
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
    }
}
