package com.badminton.mes.module.barcode.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.barcode.controller.vo.BarcodeTemplatePageReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeTemplateEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 条码模板动态查询条件。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeTemplateSpecifications {

    /**
     * 构造分页筛选条件：编码右模糊、名称包含、状态相等，固定过滤已删除。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<BarcodeTemplateEntity> page(BarcodeTemplatePageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getTemplateCode())) {
                predicates.add(criteriaBuilder.like(root.get("templateCode"),
                        reqVO.getTemplateCode() + "%"));
            }
            if (StringUtils.hasText(reqVO.getTemplateName())) {
                predicates.add(criteriaBuilder.like(root.get("templateName"),
                        "%" + reqVO.getTemplateName() + "%"));
            }
            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BarcodeTemplateSpecifications() {
    }
}
