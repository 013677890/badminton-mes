package com.badminton.mes.module.equipment.service.impl;

import java.util.List;
import java.util.Set;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategoryRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentCategorySaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentCategoryConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentCategoryEntity;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategorySpecifications;
import com.badminton.mes.module.equipment.service.EquipmentCategoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备类别 Service 实现。
 *
 * <p>Service 负责编排业务校验、事务、数据库写入。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Service
public class EquipmentCategoryServiceImpl implements EquipmentCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentCategoryServiceImpl.class);

    /** TODO(角色C, 2026/07/09): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    private final EquipmentCategoryRepository categoryRepository;

    private final CraftProcessRepository processRepository;

    private final CraftRouteDetailRepository routeDetailRepository;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param categoryRepository 设备类别 Repository
     * @param processRepository  工序 Repository
     * @param routeDetailRepository 路线明细 Repository
     */
    public EquipmentCategoryServiceImpl(EquipmentCategoryRepository categoryRepository,
                                        CraftProcessRepository processRepository,
                                        CraftRouteDetailRepository routeDetailRepository) {
        this.categoryRepository = categoryRepository;
        this.processRepository = processRepository;
        this.routeDetailRepository = routeDetailRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentCategory(EquipmentCategorySaveReqVO reqVO) {
        validateCategoryCode(reqVO.getCategoryCode(), null);
        validateParentCategory(reqVO.getParentId());

        EquipmentCategoryEntity category = EquipmentCategoryConvert.toEntity(reqVO);
        category.setCreateBy(DEFAULT_OPERATOR_ID);

        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_category_code 兜底，转成业务错误提示
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_CODE_DUPLICATE);
        }

        logger.info("[创建设备类别] id: {}, categoryCode: {}", category.getId(), category.getCategoryCode());
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentCategory(Long id, EquipmentCategorySaveReqVO reqVO) {
        EquipmentCategoryEntity existing = validateCategoryExistsForUpdate(id);
        validateCategoryCode(reqVO.getCategoryCode(), id);
        validateParentCategory(reqVO.getParentId());

        // 检查循环引用
        validateNoCyclicReference(id, reqVO.getParentId());

        boolean disabling = CommonStatusEnum.ENABLED.getStatus().equals(existing.getStatus())
                && CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus());
        if (disabling) {
            validateNoActiveReferences(id);
        }

        existing.setCategoryCode(reqVO.getCategoryCode());
        existing.setCategoryName(reqVO.getCategoryName());
        existing.setParentId(reqVO.getParentId());
        existing.setSortOrder(reqVO.getSortOrder());
        existing.setRemark(reqVO.getRemark());
        existing.setStatus(reqVO.getStatus());

        categoryRepository.save(existing);
        logger.info("[修改设备类别] id: {}, categoryCode: {}", id, reqVO.getCategoryCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentCategory(Long id) {
        EquipmentCategoryEntity category = validateCategoryExistsForUpdate(id);

        // 检查是否存在下级分类
        long childCount = categoryRepository.countByParentIdAndDeletedFalse(id);
        if (childCount > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_HAS_CHILDREN);
        }

        validateNoActiveReferences(id);

        // TODO(角色C, 2026/07/09): 后续需要检查该类别下是否存在设备
        // 暂时注释，等设备台账模块完成后再打开
        // if (equipmentRepository.countByCategoryIdAndDeletedFalse(id) > 0) {
        //     throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_HAS_EQUIPMENT);
        // }

        // 删除时重命名编码，避免唯一索引冲突（允许撤销删除或数据追溯）
        String originalCode = category.getCategoryCode();
        String deletedCode = originalCode + "_DELETED_" + System.currentTimeMillis();
        category.setCategoryCode(deletedCode);
        category.setDeleted(true);
        categoryRepository.save(category);

        logger.info("[删除设备类别] id: {}, 原编码: {}, 重命名为: {}", id, originalCode, deletedCode);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentCategoryRespVO getEquipmentCategory(Long id) {
        EquipmentCategoryEntity category = validateCategoryExists(id);
        return EquipmentCategoryConvert.toRespVO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EquipmentCategoryRespVO> getEquipmentCategoryPage(EquipmentCategoryPageReqVO reqVO) {
        Specification<EquipmentCategoryEntity> specification = EquipmentCategorySpecifications.page(reqVO);

        // 先 count：总数为 0 直接返回空页，省一次列表查询
        long total = categoryRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        // 请求页码超过总页数时按最后一页返回
        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);

        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<EquipmentCategoryEntity> page = categoryRepository.findAll(specification, pageRequest);
        List<EquipmentCategoryEntity> list = page.getContent();

        return PageResult.of(EquipmentCategoryConvert.toRespVOList(list), total, pageNo, pageSize);
    }

    /**
     * 校验设备类别存在且未删除。
     *
     * @param id 类别主键
     * @return 类别实体
     */
    private EquipmentCategoryEntity validateCategoryExists(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_NOT_EXISTS));
    }

    /**
     * 以写锁查询设备类别，供修改和删除操作保护跨表引用校验。
     *
     * @param id 类别主键
     * @return 类别实体
     */
    private EquipmentCategoryEntity validateCategoryExistsForUpdate(Long id) {
        return categoryRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_NOT_EXISTS));
    }

    /**
     * 校验类别编码唯一性。
     *
     * @param categoryCode 类别编码
     * @param excludeId    排除的类别 id，创建时传 null
     */
    private void validateCategoryCode(String categoryCode, Long excludeId) {
        boolean exists = excludeId == null
                ? categoryRepository.existsByCategoryCodeAndDeletedFalse(categoryCode)
                : categoryRepository.existsByCategoryCodeAndIdNotAndDeletedFalse(categoryCode, excludeId);

        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_CODE_DUPLICATE);
        }
    }

    /**
     * 校验父级类别存在且未删除。
     *
     * @param parentId 父级类别 id，顶级为 null
     */
    private void validateParentCategory(Long parentId) {
        if (parentId == null) {
            return;
        }

        boolean exists = categoryRepository.findByIdAndDeletedFalse(parentId).isPresent();
        if (!exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.PARENT_CATEGORY_NOT_EXISTS);
        }
    }

    /**
     * 校验父级类别不会形成循环引用。
     *
     * <p>检查逻辑：
     * <ol>
     *   <li>直接自引用：不能将自己设为父级</li>
     *   <li>间接循环：新父级的祖先链中不能包含当前节点</li>
     * </ol>
     *
     * @param currentId 当前类别 ID
     * @param newParentId 新的父级类别 ID
     */
    private void validateNoCyclicReference(Long currentId, Long newParentId) {
        if (newParentId == null) {
            return;
        }

        // 检查直接自引用
        if (currentId.equals(newParentId)) {
            throw new ServiceException(EquipmentErrorCodeConstants.CATEGORY_CANNOT_BE_SELF_PARENT);
        }

        // 检查循环引用：新父级的祖先链中不能包含当前节点
        Set<Long> ancestorIds = categoryRepository.findAncestorIds(newParentId);
        if (ancestorIds.contains(currentId)) {
            throw new ServiceException(EquipmentErrorCodeConstants.CATEGORY_CYCLIC_REFERENCE);
        }
    }

    /**
     * 校验设备类别未被启用工序或生效路线引用。
     *
     * @param categoryId 设备类别主键
     */
    private void validateNoActiveReferences(Long categoryId) {
        boolean referenced = processRepository.existsByEquipmentCategoryIdAndStatusAndDeletedFalse(
                categoryId, CommonStatusEnum.ENABLED.getStatus());
        if (referenced) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_REFERENCED_BY_PROCESS);
        }
        boolean routeReferenced = routeDetailRepository.existsEffectiveRouteByEquipmentCategoryId(
                categoryId, CraftRouteStatusEnum.EFFECTIVE.getStatus());
        if (routeReferenced) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_REFERENCED_BY_ROUTE);
        }
    }
}
