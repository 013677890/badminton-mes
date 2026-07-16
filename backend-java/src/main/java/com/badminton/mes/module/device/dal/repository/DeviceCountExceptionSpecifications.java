package com.badminton.mes.module.device.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备计数异常动态查询条件。
 *
 * <p>请求中有效的筛选项按 AND 组合；不默认排除已处理异常，以便分页接口完整展示异常审计历史。
 */
public final class DeviceCountExceptionSpecifications {

    /**
     * 构建设备计数异常分页条件。
     *
     * <p>接入配置、设备和处理状态采用精确匹配；创建开始时间使用大于等于，结束时间使用小于等于，
     * 时间区间包含两端。空条件不会生成谓词，排序和分页由调用方负责。
     *
     * @param request 分页筛选请求，未提供的标识、状态或时间边界会被忽略
     * @return 可组合到异常记录分页查询的动态 Specification
     */
    public static Specification<DeviceCountExceptionEntity> page(DeviceCountExceptionPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getAccessConfigId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("accessConfigId"), request.getAccessConfigId()));
            }
            if (request.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), request.getEquipmentId()));
            }
            if (StringUtils.hasText(request.getProcessingStatus())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("processingStatus"), request.getProcessingStatus()));
            }
            if (request.getCreateStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createTime"), request.getCreateStartTime()));
            }
            if (request.getCreateEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createTime"), request.getCreateEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 动态条件工具类不允许实例化。 */
    private DeviceCountExceptionSpecifications() {
    }
}
