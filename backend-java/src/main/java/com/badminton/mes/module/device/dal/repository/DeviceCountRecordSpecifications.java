package com.badminton.mes.module.device.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCountRecordPageReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备计数记录动态查询条件。
 *
 * <p>仅将请求中有效的可选条件加入查询，所有已加入谓词按 AND 组合，保留完整计数历史供追溯。
 */
public final class DeviceCountRecordSpecifications {

    /**
     * 构建设备计数记录分页条件。
     *
     * <p>接入配置、设备和任务匹配状态采用精确匹配；采集开始时间使用大于等于，结束时间使用小于等于，
     * 形成包含边界的采集时间区间。该条件不改变排序，分页与排序规则由调用方提供。
     *
     * @param request 分页筛选请求，未提供的标识、状态或时间边界不参与过滤
     * @return 可组合到计数记录分页查询的动态 Specification
     */
    public static Specification<DeviceCountRecordEntity> page(DeviceCountRecordPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getAccessConfigId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("accessConfigId"), request.getAccessConfigId()));
            }
            if (request.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), request.getEquipmentId()));
            }
            if (StringUtils.hasText(request.getMatchStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("matchStatus"), request.getMatchStatus()));
            }
            if (request.getCollectedStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("collectedAt"), request.getCollectedStartTime()));
            }
            if (request.getCollectedEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("collectedAt"), request.getCollectedEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 动态条件工具类不允许实例化。 */
    private DeviceCountRecordSpecifications() {
    }
}
