package com.badminton.mes.module.integration.dal.repository;

import java.util.ArrayList;
import java.util.Locale;

import com.badminton.mes.module.integration.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.integration.dal.entity.DeviceCountExceptionEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备计数异常池动态查询条件。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public final class DeviceCountExceptionSpecifications {

    /**
     * 构造异常池分页条件。
     *
     * @param reqVO 查询参数
     * @return JPA Specification
     */
    public static Specification<DeviceCountExceptionEntity> page(
            DeviceCountExceptionPageReqVO reqVO) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getSourceSystem())) {
                predicates.add(criteriaBuilder.equal(root.get("sourceSystem"),
                        reqVO.getSourceSystem().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(reqVO.getEquipmentCode())) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentCode"),
                        reqVO.getEquipmentCode().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(reqVO.getExceptionType())) {
                predicates.add(criteriaBuilder.equal(root.get("exceptionType"),
                        reqVO.getExceptionType().trim().toUpperCase(Locale.ROOT)));
            }
            if (reqVO.getHandleStatus() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("handleStatus"), reqVO.getHandleStatus()));
            }
            if (reqVO.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createTime"), reqVO.getStartTime()));
            }
            if (reqVO.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createTime"), reqVO.getEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private DeviceCountExceptionSpecifications() {
    }
}
