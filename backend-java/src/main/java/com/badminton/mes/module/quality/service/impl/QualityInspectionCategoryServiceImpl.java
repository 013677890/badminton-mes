package com.badminton.mes.module.quality.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.quality.constants.QualityErrorCodeConstants;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionCategoryConvert;
import com.badminton.mes.module.quality.dal.entity.QualityInspectionCategoryEntity;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionCategoryRepository;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionCategorySpecifications;
import com.badminton.mes.module.quality.dal.repository.QualityInspectionItemRepository;
import com.badminton.mes.module.quality.service.QualityInspectionCategoryService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 检验分类 Service 实现。 */
@Service
public class QualityInspectionCategoryServiceImpl implements QualityInspectionCategoryService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DELETED_CODE_PREFIX = "__DELETED_";

    private final QualityInspectionCategoryRepository categoryRepository;
    private final QualityInspectionItemRepository itemRepository;

    public QualityInspectionCategoryServiceImpl(QualityInspectionCategoryRepository categoryRepository,
                                                QualityInspectionItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(QualityInspectionCategorySaveReqVO request) {
        validateCategoryCodeUnique(request.getCategoryCode(), null);
        QualityInspectionCategoryEntity category = QualityInspectionCategoryConvert.toEntity(request);
        category.setEnabledStatus(request.getEnabledStatus() == null ? ENABLED : request.getEnabledStatus());
        category.setCreateBy(getCurrentOperatorId());
        category.setDeleted(false);
        saveCategory(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, QualityInspectionCategorySaveReqVO request) {
        QualityInspectionCategoryEntity category = getCategoryForUpdate(id);
        validateCategoryCodeUnique(request.getCategoryCode(), id);
        Integer previousEnabledStatus = category.getEnabledStatus();
        QualityInspectionCategoryConvert.copyEditableFields(request, category);
        if (request.getEnabledStatus() == null) {
            category.setEnabledStatus(previousEnabledStatus);
        }
        saveCategory(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        QualityInspectionCategoryEntity category = getCategoryForUpdate(id);
        if (itemRepository.countByCategoryIdAndDeletedFalse(id) > 0) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_HAS_ITEMS);
        }
        String deletedCode = DELETED_CODE_PREFIX + Long.toString(id, 36).toUpperCase();
        if (categoryRepository.existsByCategoryCode(deletedCode)) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_CODE_DUPLICATE);
        }
        category.setCategoryCode(deletedCode);
        category.setEnabledStatus(DISABLED);
        category.setDeleted(true);
        saveCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public QualityInspectionCategoryRespVO getCategory(Long id) {
        return QualityInspectionCategoryConvert.toRespVO(getCategoryEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QualityInspectionCategoryRespVO> getCategoryPage(
            QualityInspectionCategoryPageReqVO request) {
        var specification = QualityInspectionCategorySpecifications.page(request);
        long total = categoryRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<QualityInspectionCategoryEntity> page = categoryRepository.findAll(specification, pageRequest);
        List<QualityInspectionCategoryRespVO> list =
                QualityInspectionCategoryConvert.toRespVOList(page.getContent());
        return PageResult.of(list, total, pageNo, pageSize);
    }

    private QualityInspectionCategoryEntity getCategoryEntity(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS));
    }

    private QualityInspectionCategoryEntity getCategoryForUpdate(Long id) {
        return categoryRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS));
    }

    private void validateCategoryCodeUnique(String categoryCode, Long excludedId) {
        boolean exists = excludedId == null
                ? categoryRepository.existsByCategoryCodeAndDeletedFalse(categoryCode)
                : categoryRepository.existsByCategoryCodeAndIdNotAndDeletedFalse(categoryCode, excludedId);
        if (exists) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_CODE_DUPLICATE);
        }
    }

    private void saveCategory(QualityInspectionCategoryEntity category) {
        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_CODE_DUPLICATE);
        }
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
