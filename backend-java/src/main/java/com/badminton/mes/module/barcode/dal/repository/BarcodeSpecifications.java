package com.badminton.mes.module.barcode.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.barcode.controller.vo.BarcodeInstancePageReqVO;
import com.badminton.mes.module.barcode.dal.entity.BarcodeEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 条码主表动态查询条件。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public final class BarcodeSpecifications {

    /**
     * 构造分页筛选条件：条码值右模糊(可走唯一索引前缀)、批次右模糊、
     * 其余维度相等，固定过滤已删除。
     *
     * @param reqVO 分页请求
     * @return JPA Specification
     */
    public static Specification<BarcodeEntity> page(BarcodeInstancePageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getBarcodeValue())) {
                predicates.add(criteriaBuilder.like(root.get("barcodeValue"),
                        reqVO.getBarcodeValue() + "%"));
            }
            if (StringUtils.hasText(reqVO.getBatchNo())) {
                predicates.add(criteriaBuilder.like(root.get("batchNo"), reqVO.getBatchNo() + "%"));
            }
            if (reqVO.getBarcodeTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("barcodeTypeId"), reqVO.getBarcodeTypeId()));
            }
            if (reqVO.getWorkOrderId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("workOrderId"), reqVO.getWorkOrderId()));
            }
            if (reqVO.getTaskId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("taskId"), reqVO.getTaskId()));
            }
            if (reqVO.getSourceType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceType"), reqVO.getSourceType()));
            }
            if (reqVO.getBarcodeStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("barcodeStatus"), reqVO.getBarcodeStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BarcodeSpecifications() {
    }
}
