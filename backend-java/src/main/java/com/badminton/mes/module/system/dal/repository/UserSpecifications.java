package com.badminton.mes.module.system.dal.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badminton.mes.module.system.controller.vo.UserPageReqVO;
import com.badminton.mes.module.system.dal.entity.UserEntity;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

/**
 * 系统用户动态查询条件。
 *
 * <p>角色筛选不在本类做关联查询：Service 先按角色反查用户 id 集合，
 * 再通过 {@code userIds} 传入，保持实体间无 JPA 级联的项目约定。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public final class UserSpecifications {

    /**
     * 构造分页筛选条件。
     *
     * @param reqVO   分页请求
     * @param includedUserIds 必须包含的用户 id 集合，null 表示不限制
     * @param excludedUserIds 必须排除的用户 id 集合，null 表示不限制
     * @return JPA Specification
     */
    public static Specification<UserEntity> page(UserPageReqVO reqVO,
                                                  Collection<Long> includedUserIds,
                                                  Collection<Long> excludedUserIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(reqVO.getKeyword())) {
                String keyword = reqVO.getKeyword() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("userNo"), keyword),
                        criteriaBuilder.like(root.get("userName"), keyword)));
            }
            if (StringUtils.hasText(reqVO.getUserNo())) {
                predicates.add(criteriaBuilder.like(root.get("userNo"), reqVO.getUserNo() + "%"));
            }
            if (StringUtils.hasText(reqVO.getUserName())) {
                predicates.add(criteriaBuilder.like(root.get("userName"), reqVO.getUserName() + "%"));
            }
            if (reqVO.getWorkshopId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("workshopId"), reqVO.getWorkshopId()));
            }
            if (reqVO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), reqVO.getStatus()));
            }
            if (includedUserIds != null) {
                predicates.add(root.get("id").in(includedUserIds));
            }
            if (excludedUserIds != null && !excludedUserIds.isEmpty()) {
                predicates.add(criteriaBuilder.not(root.get("id").in(excludedUserIds)));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UserSpecifications() {
    }
}
