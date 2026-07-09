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
     * @param userIds 角色筛选命中的用户 id 集合，null 表示未按角色筛选
     * @return JPA Specification
     */
    public static Specification<UserEntity> page(UserPageReqVO reqVO, Collection<Long> userIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
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
            if (userIds != null) {
                predicates.add(root.get("id").in(userIds));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UserSpecifications() {
    }
}
