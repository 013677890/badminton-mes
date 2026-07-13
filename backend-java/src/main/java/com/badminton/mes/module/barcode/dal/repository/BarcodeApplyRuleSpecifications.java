package com.badminton.mes.module.barcode.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRulePageReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeApplyRuleEntity;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * 条码应用规则动态查询条件。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeApplyRuleSpecifications {

    /**
     * 构造分页筛选条件：对象类型/产品/物料/条码类型/来源/状态相等，固定过滤已删除。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<BarcodeApplyRuleEntity> page(BarcodeApplicationRulePageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (reqVO.getObjectType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("objectType"), reqVO.getObjectType()));
            }
            if (reqVO.getProductId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productId"), reqVO.getProductId()));
            }
            if (reqVO.getMaterialId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("materialId"), reqVO.getMaterialId()));
            }
            if (reqVO.getBarcodeTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("barcodeTypeId"), reqVO.getBarcodeTypeId()));
            }
            if (reqVO.getSourceType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceType"), reqVO.getSourceType()));
            }
            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BarcodeApplyRuleSpecifications() {
    }
}
