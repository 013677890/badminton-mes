package com.badminton.mes.module.barcode.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.barcode.controller.vo.BarcodeTypePageReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTypeEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 条码类型动态查询条件。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeTypeSpecifications {

    /**
     * 构造分页筛选条件：编码右模糊、名称包含、状态相等，固定过滤已删除。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<BarcodeTypeEntity> page(BarcodeTypePageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getTypeCode())) {
                predicates.add(criteriaBuilder.like(root.get("typeCode"), reqVO.getTypeCode() + "%"));
            }
            if (StringUtils.hasText(reqVO.getTypeName())) {
                predicates.add(criteriaBuilder.like(root.get("typeName"), "%" + reqVO.getTypeName() + "%"));
            }
            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BarcodeTypeSpecifications() {
    }
}
