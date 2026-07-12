package com.badminton.mes.module.production.dal.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badminton.mes.module.production.controller.vo.BomPageReqVO;
import com.badminton.mes.module.production.controller.vo.MaterialPageReqVO;
import com.badminton.mes.module.production.controller.vo.ProductPageReqVO;
import com.badminton.mes.module.production.dal.entity.BomEntity;
import com.badminton.mes.module.production.dal.entity.MaterialEntity;
import com.badminton.mes.module.production.dal.entity.ProductEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/** 生产基础资料动态分页条件。 */
public final class ProductionMasterDataSpecifications {

    /** 构造产品分页条件。 */
    public static Specification<ProductEntity> productPage(ProductPageReqVO reqVO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getProductCode())) {
                String prefix = escapeLike(reqVO.getProductCode().trim().toUpperCase(Locale.ROOT)) + "%";
                predicates.add(builder.like(root.get("productCode"), prefix, '\\'));
            }
            if (StringUtils.hasText(reqVO.getProductName())) {
                predicates.add(builder.like(root.get("productName"),
                        escapeLike(reqVO.getProductName().trim()) + "%", '\\'));
            }
            addEqual(predicates, builder, root.get("productType"), reqVO.getProductType());
            addEqual(predicates, builder, root.get("unitId"), reqVO.getUnitId());
            addEqual(predicates, builder, root.get("status"), reqVO.getStatus());
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 构造物料分页条件。 */
    public static Specification<MaterialEntity> materialPage(MaterialPageReqVO reqVO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getMaterialCode())) {
                String prefix = escapeLike(reqVO.getMaterialCode().trim().toUpperCase(Locale.ROOT)) + "%";
                predicates.add(builder.like(root.get("materialCode"), prefix, '\\'));
            }
            if (StringUtils.hasText(reqVO.getMaterialName())) {
                predicates.add(builder.like(root.get("materialName"),
                        escapeLike(reqVO.getMaterialName().trim()) + "%", '\\'));
            }
            addEqual(predicates, builder, root.get("materialType"), reqVO.getMaterialType());
            addEqual(predicates, builder, root.get("unitId"), reqVO.getUnitId());
            addEqual(predicates, builder, root.get("keyMaterial"), reqVO.getKeyMaterial());
            addEqual(predicates, builder, root.get("status"), reqVO.getStatus());
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 构造 BOM 分页条件。 */
    public static Specification<BomEntity> bomPage(BomPageReqVO reqVO) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getBomCode())) {
                String prefix = escapeLike(reqVO.getBomCode().trim().toUpperCase(Locale.ROOT)) + "%";
                predicates.add(builder.like(root.get("bomCode"), prefix, '\\'));
            }
            addEqual(predicates, builder, root.get("productId"), reqVO.getProductId());
            if (StringUtils.hasText(reqVO.getVersion())) {
                predicates.add(builder.equal(root.get("version"), reqVO.getVersion().trim().toUpperCase(Locale.ROOT)));
            }
            addEqual(predicates, builder, root.get("bomStatus"), reqVO.getBomStatus());
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 添加非空等值条件。 */
    private static void addEqual(List<Predicate> predicates,
                                 jakarta.persistence.criteria.CriteriaBuilder builder,
                                 jakarta.persistence.criteria.Path<?> path, Object value) {
        if (value != null) {
            predicates.add(builder.equal(path, value));
        }
    }

    /** 转义 LIKE 通配符和转义符。 */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private ProductionMasterDataSpecifications() {
    }
}
