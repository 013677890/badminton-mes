package com.badminton.mes.module.quality.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.quality.constants.QualityErrorCodeConstants;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionItemConvert;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionItemEntity;
import com.badminton.mes.module.quality.dal.redis.QualityCache;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionCategoryRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemSpecifications;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionPlanItemRepository;
import com.badminton.mes.module.quality.service.QualityInspectionItemService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 检验项目应用服务实现。
 *
 * <p>项目必须引用存在且启用的分类。规则校验把值类型与判定方式视为组合约束：数值型必须有单位和
 * 有序上下限，文本型禁止上下限，RANGE 只能配合数值型，STANDARD_VALUE 必须提供标准值。</p>
 *
 * <p>方案详情会从项目主数据冗余编码、名称、值类型和单位，因此项目更新需要在事务提交后级联失效
 * 所有引用方案的详情缓存；方案项自身的标准值、上下限和判定方式是版本快照，不会被本服务回写。</p>
 */
@Service
public class QualityInspectionItemServiceImpl implements QualityInspectionItemService {

    /** 值类型、判定方式及主数据状态约定。 */
    private static final String VALUE_TYPE_NUMERIC = "NUMERIC";
    private static final String JUDGMENT_METHOD_RANGE = "RANGE";
    private static final String JUDGMENT_METHOD_STANDARD_VALUE = "STANDARD_VALUE";
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DELETED_CODE_PREFIX = "__DELETED_";

    private final QualityInspectionItemRepository itemRepository;
    private final QualityInspectionCategoryRepository categoryRepository;
    private final QualityInspectionPlanItemRepository planItemRepository;
    private final QualityCache qualityCache;

    /** 注入项目存储、分类引用、方案项反向引用和质量详情缓存。 */
    public QualityInspectionItemServiceImpl(QualityInspectionItemRepository itemRepository,
                                            QualityInspectionCategoryRepository categoryRepository,
                                            QualityInspectionPlanItemRepository planItemRepository,
                                            QualityCache qualityCache) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.planItemRepository = planItemRepository;
        this.qualityCache = qualityCache;
    }

    /** 创建前完整校验编码、分类和规则，保存时再以数据库约束兜底并发编码冲突。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createItem(QualityInspectionItemSaveReqVO request) {
        validateItemCodeUnique(request.getItemCode(), null);
        validateCategoryAvailable(request.getCategoryId());
        validateInspectionRule(request);
        QualityInspectionItemEntity item = QualityInspectionItemConvert.toEntity(request);
        item.setRequiredFlag(Boolean.TRUE.equals(request.getRequiredFlag()));
        item.setEnabledStatus(request.getEnabledStatus() == null ? ENABLED : request.getEnabledStatus());
        item.setCreateBy(getCurrentOperatorId());
        item.setDeleted(false);
        saveItem(item);
        return item.getId();
    }

    /** 更新先悲观锁定项目；未传必检或启用状态时保留原值，不把可选请求字段解释为清空。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long id, QualityInspectionItemSaveReqVO request) {
        QualityInspectionItemEntity item = getItemForUpdate(id);
        validateItemCodeUnique(request.getItemCode(), id);
        validateCategoryAvailable(request.getCategoryId());
        validateInspectionRule(request);
        Integer previousEnabledStatus = item.getEnabledStatus();
        Boolean previousRequiredFlag = item.getRequiredFlag();
        QualityInspectionItemConvert.copyEditableFields(request, item);
        if (request.getEnabledStatus() == null) {
            item.setEnabledStatus(previousEnabledStatus);
        }
        if (request.getRequiredFlag() == null) {
            item.setRequiredFlag(previousRequiredFlag);
        }
        saveItem(item);
        // 项目详情本身以及引用方案中的当前主数据冗余都必须在同一次提交后失效。
        evictItemCacheAfterCommit(id);
        qualityCache.evictDetailsAfterCommit(QualityRedisKeyConstants.INSPECTION_PLAN_RESOURCE,
                planItemRepository.findDistinctPlanIdsByInspectionItemId(id));
    }

    /**
     * 任何方案版本只要引用该项目就禁止删除，避免方案项及后续检验快照失去可追溯的主数据来源。
     * 通过替换编码、停用和逻辑删除释放原业务编码，同时保留历史主键。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        QualityInspectionItemEntity item = getItemForUpdate(id);
        if (planItemRepository.existsByInspectionItemId(id)) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_REFERENCED_BY_PLAN);
        }
        String deletedCode = DELETED_CODE_PREFIX + Long.toString(id, 36).toUpperCase();
        if (itemRepository.existsByItemCode(deletedCode)) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_CODE_DUPLICATE);
        }
        item.setItemCode(deletedCode);
        item.setEnabledStatus(DISABLED);
        item.setDeleted(true);
        saveItem(item);
        evictItemCacheAfterCommit(id);
    }

    /** 详情查询同时解析所属分类的当前主数据，并将组合结果作为项目详情缓存。 */
    @Override
    @Transactional(readOnly = true)
    public QualityInspectionItemRespVO getItem(Long id) {
        return qualityCache.getOrLoadDetail(QualityRedisKeyConstants.INSPECTION_ITEM_RESOURCE,
                id, QualityInspectionItemRespVO.class, () -> {
            QualityInspectionItemEntity item = getItemEntity(id);
            QualityInspectionCategoryEntity category = getCategoryEntity(item.getCategoryId());
            QualityInspectionItemRespVO response = QualityInspectionItemConvert.toRespVO(item, category);
            return response;
        });
    }

    /** 分页批量加载页面内所有分类，避免逐项目查询；缺失分类视为引用完整性错误。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<QualityInspectionItemRespVO> getItemPage(QualityInspectionItemPageReqVO request) {
        var specification = QualityInspectionItemSpecifications.page(request);
        long total = itemRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<QualityInspectionItemEntity> page = itemRepository.findAll(specification, pageRequest);
        Map<Long, QualityInspectionCategoryEntity> categoriesById = categoryRepository
                .findAllById(page.getContent().stream().map(QualityInspectionItemEntity::getCategoryId).collect(
                        Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(QualityInspectionCategoryEntity::getId, Function.identity()));
        List<QualityInspectionItemRespVO> list = page.getContent().stream()
                .map(item -> QualityInspectionItemConvert.toRespVO(item, requireCategory(
                        categoriesById, item.getCategoryId())))
                .toList();
        return PageResult.of(list, total, pageNo, pageSize);
    }

    /**
     * 校验值类型与判定方式的合法组合。
     *
     * <p>数值型无论采用何种判定方式都要求单位和上下限，且下限不得大于上限；非数值型不能残留上下限。
     * RANGE 进一步限定为数值型，STANDARD_VALUE 则要求可判定的非空标准值。</p>
     */
    private void validateInspectionRule(QualityInspectionItemSaveReqVO request) {
        boolean numericValue = VALUE_TYPE_NUMERIC.equals(request.getValueType());
        boolean hasBothLimits = request.getLowerLimit() != null && request.getUpperLimit() != null;
        boolean limitsOrdered = hasBothLimits && request.getLowerLimit().compareTo(request.getUpperLimit()) <= 0;
        if (numericValue && (!StringUtils.hasText(request.getUnit()) || !hasBothLimits || !limitsOrdered)) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_RULE_INVALID);
        }
        if (!numericValue && (request.getLowerLimit() != null || request.getUpperLimit() != null)) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_RULE_INVALID);
        }
        if (JUDGMENT_METHOD_RANGE.equals(request.getJudgmentMethod()) && !numericValue) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_RULE_INVALID);
        }
        if (JUDGMENT_METHOD_STANDARD_VALUE.equals(request.getJudgmentMethod())
                && !StringUtils.hasText(request.getStandardValue())) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_RULE_INVALID);
        }
    }

    /** 仅允许项目挂接到未删除且启用的分类，停用分类不能承接新增或更新引用。 */
    private void validateCategoryAvailable(Long categoryId) {
        QualityInspectionCategoryEntity category = getCategoryEntity(categoryId);
        if (!Integer.valueOf(ENABLED).equals(category.getEnabledStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS);
        }
    }

    /** 创建检查全部有效项目，更新时排除当前项目自身。 */
    private void validateItemCodeUnique(String itemCode, Long excludedId) {
        boolean exists = excludedId == null
                ? itemRepository.existsByItemCodeAndDeletedFalse(itemCode)
                : itemRepository.existsByItemCodeAndIdNotAndDeletedFalse(itemCode, excludedId);
        if (exists) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_CODE_DUPLICATE);
        }
    }

    /** 读取未删除项目，不获取写锁。 */
    private QualityInspectionItemEntity getItemEntity(Long id) {
        return itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.ITEM_NOT_EXISTS));
    }

    /** 悲观锁定项目，使引用检查、状态保留和持久化修改位于同一事务窗口。 */
    private QualityInspectionItemEntity getItemForUpdate(Long id) {
        return itemRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.ITEM_NOT_EXISTS));
    }

    /** 获取项目关联的未删除分类。 */
    private QualityInspectionCategoryEntity getCategoryEntity(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS));
    }

    /** 从批量结果中强制取得有效分类，避免用空冗余字段掩盖损坏的分类引用。 */
    private QualityInspectionCategoryEntity requireCategory(
            Map<Long, QualityInspectionCategoryEntity> categoriesById, Long categoryId) {
        QualityInspectionCategoryEntity category = categoriesById.get(categoryId);
        if (category == null || Boolean.TRUE.equals(category.getDeleted())) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    /** 立即刷盘并将数据库唯一约束异常统一映射为项目编码重复。 */
    private void saveItem(QualityInspectionItemEntity item) {
        try {
            itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_CODE_DUPLICATE);
        }
    }

    /** 注册事务提交后的项目详情缓存失效动作。 */
    private void evictItemCacheAfterCommit(Long id) {
        qualityCache.evictDetailAfterCommit(QualityRedisKeyConstants.INSPECTION_ITEM_RESOURCE, id);
    }

    /** 无登录上下文的系统调用回退到默认操作人。 */
    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
