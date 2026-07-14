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

/** 检验项目 Service 实现。 */
@Service
public class QualityInspectionItemServiceImpl implements QualityInspectionItemService {

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

    public QualityInspectionItemServiceImpl(QualityInspectionItemRepository itemRepository,
                                            QualityInspectionCategoryRepository categoryRepository,
                                            QualityInspectionPlanItemRepository planItemRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.planItemRepository = planItemRepository;
    }

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
    }

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
    }

    @Override
    @Transactional(readOnly = true)
    public QualityInspectionItemRespVO getItem(Long id) {
        QualityInspectionItemEntity item = getItemEntity(id);
        QualityInspectionCategoryEntity category = getCategoryEntity(item.getCategoryId());
        return QualityInspectionItemConvert.toRespVO(item, category);
    }

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

    private void validateCategoryAvailable(Long categoryId) {
        QualityInspectionCategoryEntity category = getCategoryEntity(categoryId);
        if (!Integer.valueOf(ENABLED).equals(category.getEnabledStatus())) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS);
        }
    }

    private void validateItemCodeUnique(String itemCode, Long excludedId) {
        boolean exists = excludedId == null
                ? itemRepository.existsByItemCodeAndDeletedFalse(itemCode)
                : itemRepository.existsByItemCodeAndIdNotAndDeletedFalse(itemCode, excludedId);
        if (exists) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_CODE_DUPLICATE);
        }
    }

    private QualityInspectionItemEntity getItemEntity(Long id) {
        return itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.ITEM_NOT_EXISTS));
    }

    private QualityInspectionItemEntity getItemForUpdate(Long id) {
        return itemRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.ITEM_NOT_EXISTS));
    }

    private QualityInspectionCategoryEntity getCategoryEntity(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS));
    }

    private QualityInspectionCategoryEntity requireCategory(
            Map<Long, QualityInspectionCategoryEntity> categoriesById, Long categoryId) {
        QualityInspectionCategoryEntity category = categoriesById.get(categoryId);
        if (category == null || Boolean.TRUE.equals(category.getDeleted())) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private void saveItem(QualityInspectionItemEntity item) {
        try {
            itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.ITEM_CODE_DUPLICATE);
        }
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
