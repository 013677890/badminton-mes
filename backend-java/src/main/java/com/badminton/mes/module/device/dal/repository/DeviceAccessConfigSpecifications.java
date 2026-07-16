package com.badminton.mes.module.device.dal.repository;

import java.util.ArrayList;
import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigPageReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 设备接入配置动态查询条件。
 *
 * <p>所有条件使用 AND 组合；逻辑删除过滤始终生效，其余筛选项仅在请求提供有效值时追加。
 */
public final class DeviceAccessConfigSpecifications {

    /**
     * 构建设备接入配置分页条件。
     *
     * <p>固定排除逻辑删除记录；关键字同时模糊匹配配置编码、配置名称和采集点编码，且转义用户输入中的
     * LIKE 通配符；设备、工序、联调状态和启用状态均为可选精确匹配条件。排序和分页由调用方传入的
     * Pageable 负责，此处只描述过滤谓词。
     *
     * @param request 分页筛选请求，空文本和空标识不会生成对应条件
     * @return 可与 Spring Data 分页查询组合的动态 Specification
     */
    public static Specification<DeviceAccessConfigEntity> page(DeviceAccessConfigPageReqVO request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + escapeWildcards(request.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("configCode"), pattern, '\\'),
                        criteriaBuilder.like(root.get("configName"), pattern, '\\'),
                        criteriaBuilder.like(root.get("collectionPointCode"), pattern, '\\')));
            }
            if (request.getEquipmentId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("equipmentId"), request.getEquipmentId()));
            }
            if (request.getProcessId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("processId"), request.getProcessId()));
            }
            if (StringUtils.hasText(request.getCommissioningStatus())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("commissioningStatus"), request.getCommissioningStatus()));
            }
            if (request.getEnabledStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabledStatus"), request.getEnabledStatus()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 转义反斜杠、百分号和下划线，使关键字按普通字符参与包含匹配而非扩展匹配范围。 */
    private static String escapeWildcards(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** 动态条件工具类不允许实例化。 */
    private DeviceAccessConfigSpecifications() {
    }
}
