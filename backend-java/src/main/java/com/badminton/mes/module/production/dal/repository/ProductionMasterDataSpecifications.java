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

/**
 * 生产基础资料动态分页条件。
 *
 * <p>产品、物料和 BOM 共用前缀匹配、枚举等值匹配及逻辑删除边界；LIKE 输入统一转义，避免用户输入通配符改变查询范围。
 */
public final class ProductionMasterDataSpecifications {

    /** 构造产品分页条件。 */
    public static Specification<ProductEntity> productPage(ProductPageReqVO reqVO) {
        return (root, query, builder) -> {
            // 产品编码按大写前缀匹配，名称按原展示文本前缀匹配，其余字段采用等值条件。
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
            // 物料查询始终排除逻辑删除行，并把关键物料、状态和计量单位作为可选精确条件。
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
            // BOM 编码和版本先按统一大写口径过滤，产品及状态使用精确匹配。
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
        // 空值表示调用方未筛选该字段，不能生成与 NULL 比较的无效条件。
        if (value != null) {
            predicates.add(builder.equal(path, value));
        }
    }

    /** 转义 LIKE 通配符和转义符。 */
    private static String escapeLike(String value) {
        // 先转义反斜杠，再转义 % 和 _，防止查询参数被当作 SQL LIKE 通配符执行。
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private ProductionMasterDataSpecifications() {
        // 动态条件通过静态工厂提供，不允许创建工具类实例。
    }
}
