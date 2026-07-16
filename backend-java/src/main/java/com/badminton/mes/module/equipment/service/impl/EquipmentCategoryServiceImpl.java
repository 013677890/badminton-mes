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
import com.badminton.mes.module.equipment.dal.redis.EquipmentCache;
import com.badminton.mes.module.equipment.dal.redis.EquipmentRedisKeyConstants;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategoryRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentCategorySpecifications;
import com.badminton.mes.module.equipment.dal.repository.EquipmentFaultPrincipleRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
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
 * <p>维护设备类别树，并在事务内协调编码唯一性、父子层级、设备与故障原理引用以及工艺模块引用。
 * 类别停用和逻辑删除均受生效工艺数据保护；修改、删除后仅在事务提交成功时失效详情缓存，避免
 * 数据库回滚而缓存提前清除。更新和删除先锁定当前类别，使层级及跨表引用校验基于稳定版本。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Service
public class EquipmentCategoryServiceImpl implements EquipmentCategoryService {

    /** 记录类别主数据关键写操作。 */
    private static final Logger logger = LoggerFactory.getLogger(EquipmentCategoryServiceImpl.class);

    /** TODO(角色C, 2026/07/09): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 类别仓储，提供树结构查询、悲观锁读取及编码唯一性检查。 */
    private final EquipmentCategoryRepository categoryRepository;

    /** 台账仓储，用于阻止删除仍被设备引用的类别。 */
    private final EquipmentLedgerRepository ledgerRepository;

    /** 故障原理仓储，用于保护故障知识所依赖的适用类别。 */
    private final EquipmentFaultPrincipleRepository faultPrincipleRepository;

    /** 工序仓储，用于保护启用工序引用。 */
    private final CraftProcessRepository processRepository;

    /** 工艺路线明细仓储，用于保护生效路线引用。 */
    private final CraftRouteDetailRepository routeDetailRepository;

    /** 类别详情缓存协调器，写事务提交后负责失效。 */
    private final EquipmentCache equipmentCache;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param categoryRepository 设备类别 Repository
     * @param ledgerRepository 设备台账 Repository
     * @param faultPrincipleRepository 设备故障原理 Repository
     * @param processRepository 工序 Repository
     * @param routeDetailRepository 路线明细 Repository
     * @param equipmentCache 设备缓存组件
     */
    public EquipmentCategoryServiceImpl(
            EquipmentCategoryRepository categoryRepository,
            EquipmentLedgerRepository ledgerRepository,
            EquipmentFaultPrincipleRepository faultPrincipleRepository,
            CraftProcessRepository processRepository,
            CraftRouteDetailRepository routeDetailRepository,
            EquipmentCache equipmentCache) {
        this.categoryRepository = categoryRepository;
        this.ledgerRepository = ledgerRepository;
        this.faultPrincipleRepository = faultPrincipleRepository;
        this.processRepository = processRepository;
        this.routeDetailRepository = routeDetailRepository;
        this.equipmentCache = equipmentCache;
    }

    /**
     * 创建类别并校验编码及可选父级。
     *
     * <p>应用层查重便于快速返回业务异常，{@code saveAndFlush} 触发数据库唯一索引作为并发兜底；
     * 创建人和缺省启用状态由服务统一补齐。
     *
     * @param reqVO 类别创建数据
     * @return 新类别主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentCategory(EquipmentCategorySaveReqVO reqVO) {
        validateCategoryCode(reqVO.getCategoryCode(), null);
        validateParentCategory(reqVO.getParentId());

        EquipmentCategoryEntity category = EquipmentCategoryConvert.toEntity(reqVO);
        category.setCreateBy(DEFAULT_OPERATOR_ID);
        // 设置状态默认值为启用
        if (category.getStatus() == null) {
            category.setStatus(1);
        }

        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_category_code 兜底
            // 精确匹配唯一索引冲突，避免误判其他约束（如外键）
            if (e.getMessage() != null && e.getMessage().contains("uk_category_code")) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_CODE_DUPLICATE);
            }
            // 其他数据库约束冲突不应吞噬，向上抛出
            throw e;
        }

        logger.info("[创建设备类别] id: {}, categoryCode: {}", category.getId(), category.getCategoryCode());
        return category.getId();
    }

    /**
     * 更新类别树节点，并保护层级闭环和生效工艺引用。
     *
     * <p>当前类别先以悲观写锁读取；新父级既不能是自身，也不能位于当前节点的后代链上。类别从
     * 启用切换为停用时，还必须确认没有启用工序或生效路线依赖该能力分类。
     *
     * @param id 类别主键
     * @param reqVO 类别更新数据
     */
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
            validateNoActiveCraftReferences(id);
        }

        existing.setCategoryCode(reqVO.getCategoryCode());
        existing.setCategoryName(reqVO.getCategoryName());
        existing.setParentId(reqVO.getParentId());
        existing.setSortOrder(reqVO.getSortOrder());
        existing.setRemark(reqVO.getRemark());
        // 修改时若状态为 null，保持原值不变
        if (reqVO.getStatus() != null) {
            existing.setStatus(reqVO.getStatus());
        }

        categoryRepository.save(existing);
        evictCategoryCacheAfterCommit(id);
        logger.info("[修改设备类别] id: {}, categoryCode: {}", id, reqVO.getCategoryCode());
    }

    /**
     * 在全部引用保护通过后逻辑删除类别。
     *
     * <p>子类别、设备、故障原理以及生效工艺任一引用存在时均拒绝删除。删除采用改写编码加删除标记
     * 的方式保留审计记录并释放原唯一键，详情缓存延迟至事务提交后失效。
     *
     * @param id 类别主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentCategory(Long id) {
        EquipmentCategoryEntity category = validateCategoryExistsForUpdate(id);

        // 检查是否存在下级分类
        long childCount = categoryRepository.countByParentIdAndDeletedFalse(id);
        if (childCount > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_HAS_CHILDREN);
        }

        long equipmentCount = ledgerRepository.countByCategoryIdAndDeletedFalse(id);
        if (equipmentCount > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_HAS_EQUIPMENT);
        }

        long faultPrincipleCount = faultPrincipleRepository.countByCategoryIdAndDeletedFalse(id);
        if (faultPrincipleCount > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_HAS_FAULT_PRINCIPLE);
        }

        validateNoActiveCraftReferences(id);

        // 删除时重命名编码，避免唯一索引冲突（允许撤销删除或数据追溯）
        String originalCode = category.getCategoryCode();
        String deletedCode = originalCode + "_DELETED_" + System.currentTimeMillis();
        category.setCategoryCode(deletedCode);
        category.setDeleted(true);
        categoryRepository.save(category);
        evictCategoryCacheAfterCommit(id);

        logger.info("[删除设备类别] id: {}", id);
    }

    /**
     * 读取类别详情，缓存未命中时加载未删除实体并生成响应快照。
     *
     * @param id 类别主键
     * @return 类别详情快照
     */
    @Override
    @Transactional(readOnly = true)
    public EquipmentCategoryRespVO getEquipmentCategory(Long id) {
        return equipmentCache.getOrLoadDetail(EquipmentRedisKeyConstants.CATEGORY_RESOURCE,
                id, EquipmentCategoryRespVO.class, () -> {
            EquipmentCategoryRespVO response = EquipmentCategoryConvert.toRespVO(validateCategoryExists(id));
            return response;
        });
    }

    /**
     * 分页查询类别，按排序号和主键提供稳定顺序。
     *
     * <p>无匹配数据时跳过列表查询；请求页码超出范围时收敛到最后一页。
     *
     * @param reqVO 分页及筛选条件
     * @return 类别分页快照
     */
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
     * <p>锁定当前节点可避免同一类别被并发修改或删除，使后续层级与引用判断对应同一实体版本。
     *
     * @param id 类别主键
     * @return 类别实体
     */
    private EquipmentCategoryEntity validateCategoryExistsForUpdate(Long id) {
        return categoryRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_NOT_EXISTS));
    }

    /**
     * 校验有效类别编码唯一性；并发事务的最终冲突由数据库唯一索引处理。
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
     * 祖先集合由仓储一次查询得到，既避免逐层访问，也能明确识别将节点挂到自身后代下的情况。
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
     * <p>仅保护当前仍具业务效力的引用，允许历史停用数据保留外键快照而不阻塞主数据治理。
     *
     * @param categoryId 设备类别主键
     */
    private void validateNoActiveCraftReferences(Long categoryId) {
        boolean processReferenced = processRepository.existsByEquipmentCategoryIdAndStatusAndDeletedFalse(
                categoryId, CommonStatusEnum.ENABLED.getStatus());
        if (processReferenced) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_REFERENCED_BY_PROCESS);
        }

        boolean routeReferenced = routeDetailRepository.existsEffectiveRouteByEquipmentCategoryId(
                categoryId, CraftRouteStatusEnum.EFFECTIVE.getStatus());
        if (routeReferenced) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_CATEGORY_REFERENCED_BY_ROUTE);
        }
    }

    /**
     * 登记类别详情缓存在当前事务成功提交后失效。
     *
     * @param id 类别主键
     */
    private void evictCategoryCacheAfterCommit(Long id) {
        equipmentCache.evictDetailAfterCommit(EquipmentRedisKeyConstants.CATEGORY_RESOURCE, id);
    }
}
