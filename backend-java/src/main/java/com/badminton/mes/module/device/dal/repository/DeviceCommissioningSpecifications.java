package com.badminton.mes.module.device.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCommissioningPageReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceCommissioningRecordEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备联调记录动态查询条件。
 *
 * <p>请求中存在的筛选项按 AND 组合，不提供的字段不产生谓词，也不额外排除任何历史联调记录。
 */
public final class DeviceCommissioningSpecifications {

    /**
     * 构建设备联调记录分页条件。
     *
     * <p>接入配置主键和综合测试结论采用精确匹配；开始时间使用大于等于，结束时间使用小于等于，
     * 因而时间区间两端均包含。排序及分页由调用方负责，此处仅动态生成过滤条件。
     *
     * @param request 分页筛选请求，空文本和空时间边界会被忽略
     * @return 可组合到联调记录分页查询的动态 Specification
     */
    public static Specification<DeviceCommissioningRecordEntity> page(DeviceCommissioningPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getAccessConfigId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("accessConfigId"), request.getAccessConfigId()));
            }
            if (StringUtils.hasText(request.getTestResult())) {
                predicates.add(criteriaBuilder.equal(root.get("testResult"), request.getTestResult()));
            }
            if (request.getTestStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("testTime"), request.getTestStartTime()));
            }
            if (request.getTestEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("testTime"), request.getTestEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 动态条件工具类不允许实例化。 */
    private DeviceCommissioningSpecifications() {
    }
}
