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
import com.badminton.mes.module.quality.dal.redis.QualityCache;
import com.badminton.mes.module.quality.dal.redis.QualityRedisKeyConstants;
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

/**
 * 检验分类应用服务实现。
 *
 * <p>分类属于检验主数据。编码唯一性同时通过应用层预检查和数据库约束兜底；更新、删除先悲观锁定目标行，
 * 防止并发写入覆盖。逻辑删除会释放原业务编码，但在仍被有效检验项目引用时禁止删除。</p>
 *
 * <p>项目详情响应冗余分类编码和名称，因此分类更新除失效自身缓存外，还需在事务提交后级联失效
 * 该分类下的全部项目详情缓存，保证缓存内容与已提交主数据一致。</p>
 */
@Service
public class QualityInspectionCategoryServiceImpl implements QualityInspectionCategoryService {

    /** 主数据状态和逻辑删除占位编码约定。 */
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DELETED_CODE_PREFIX = "__DELETED_";

    private final QualityInspectionCategoryRepository categoryRepository;
    private final QualityInspectionItemRepository itemRepository;
    private final QualityCache qualityCache;

    /** 注入分类存储、下级项目引用查询和质量详情缓存。 */
    public QualityInspectionCategoryServiceImpl(QualityInspectionCategoryRepository categoryRepository,
                                                QualityInspectionItemRepository itemRepository,
                                                QualityCache qualityCache) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.qualityCache = qualityCache;
    }

    /** 创建时先做可读的唯一性校验，再由强制刷盘捕获并发插入造成的唯一键冲突。 */
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

    /**
     * 更新使用悲观锁串行化同一分类的写操作；请求未携带启用状态时保留数据库原值，
     * 避免通用字段复制把已有状态覆盖为 {@code null}。
     */
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
        // 缓存失效延迟到事务提交后执行，避免其他线程回填尚未提交或最终回滚的数据。
        evictCategoryCacheAfterCommit(id);
        // 分类信息被冗余进项目响应，分类变更必须沿引用关系级联清理项目详情缓存。
        qualityCache.evictDetailsAfterCommit(QualityRedisKeyConstants.INSPECTION_ITEM_RESOURCE,
                itemRepository.findIdsByCategoryIdAndDeletedFalse(id));
    }

    /**
     * 删除前锁定分类并检查下级引用；使用不可复用的占位编码释放原业务编码，
     * 同时停用并标记删除，使历史外键仍可保留原分类行。
     */
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
        evictCategoryCacheAfterCommit(id);
    }

    /** 分类详情采用旁路缓存，加载器只允许返回未逻辑删除的数据。 */
    @Override
    @Transactional(readOnly = true)
    public QualityInspectionCategoryRespVO getCategory(Long id) {
        return qualityCache.getOrLoadDetail(QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE,
                id, QualityInspectionCategoryRespVO.class, () -> {
            QualityInspectionCategoryRespVO response = QualityInspectionCategoryConvert.toRespVO(getCategoryEntity(id));
            return response;
        });
    }

    /** 分页先计数并将越界页码收敛到末页，避免有效筛选条件下返回误导性的空页。 */
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

    /** 读取未删除分类，不加写锁，供只读详情与关联校验使用。 */
    private QualityInspectionCategoryEntity getCategoryEntity(Long id) {
        return categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS));
    }

    /** 悲观锁定未删除分类，保证状态检查和后续修改处于同一串行化窗口。 */
    private QualityInspectionCategoryEntity getCategoryForUpdate(Long id) {
        return categoryRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(QualityErrorCodeConstants.CATEGORY_NOT_EXISTS));
    }

    /** 创建检查全部有效数据，更新检查时排除当前分类自身。 */
    private void validateCategoryCodeUnique(String categoryCode, Long excludedId) {
        boolean exists = excludedId == null
                ? categoryRepository.existsByCategoryCodeAndDeletedFalse(categoryCode)
                : categoryRepository.existsByCategoryCodeAndIdNotAndDeletedFalse(categoryCode, excludedId);
        if (exists) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_CODE_DUPLICATE);
        }
    }

    /**
     * 立即刷盘以在当前事务边界内暴露唯一键冲突，并统一转换为稳定的分类编码业务异常。
     */
    private void saveCategory(QualityInspectionCategoryEntity category) {
        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(QualityErrorCodeConstants.CATEGORY_CODE_DUPLICATE);
        }
    }

    /** 注册事务提交后的分类详情缓存失效动作。 */
    private void evictCategoryCacheAfterCommit(Long id) {
        qualityCache.evictDetailAfterCommit(QualityRedisKeyConstants.INSPECTION_CATEGORY_RESOURCE, id);
    }

    /** 无登录上下文的系统调用使用默认操作人，正常请求必须读取已认证用户。 */
    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
