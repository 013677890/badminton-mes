package com.badminton.mes.module.craft.dal.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badminton.mes.module.craft.controller.vo.CraftProcessPageReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 工序档案动态查询条件。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public final class CraftProcessSpecifications {

    /**
     * 构造工序分页查询条件。
     *
     * @param reqVO 分页查询请求
     * @return JPA Specification
     */
    public static Specification<CraftProcessEntity> page(CraftProcessPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(reqVO.getProcessCode())) {
                String codePrefix = reqVO.getProcessCode().trim().toUpperCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.like(root.get("processCode"), codePrefix));
            }
            if (StringUtils.hasText(reqVO.getProcessName())) {
                predicates.add(criteriaBuilder.like(root.get("processName"), reqVO.getProcessName().trim() + "%"));
            }
            if (StringUtils.hasText(reqVO.getProcessType())) {
                String processType = reqVO.getProcessType().trim().toUpperCase(Locale.ROOT);
                predicates.add(criteriaBuilder.equal(root.get("processType"), processType));
            }
            if (reqVO.getKeyProcess() != null) {
                predicates.add(criteriaBuilder.equal(root.get("keyProcess"), reqVO.getKeyProcess()));
            }
            if (reqVO.getQualityRequired() != null) {
                predicates.add(criteriaBuilder.equal(root.get("qualityRequired"), reqVO.getQualityRequired()));
            }
            if (reqVO.getScanRequired() != null) {
                predicates.add(criteriaBuilder.equal(root.get("scanRequired"), reqVO.getScanRequired()));
            }
            if (reqVO.getPieceRateEnabled() != null) {
                predicates.add(criteriaBuilder.equal(root.get("pieceRateEnabled"), reqVO.getPieceRateEnabled()));
            }
            if (reqVO.getEquipmentCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("equipmentCategoryId"), reqVO.getEquipmentCategoryId()));
            }
            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private CraftProcessSpecifications() {
    }
}
