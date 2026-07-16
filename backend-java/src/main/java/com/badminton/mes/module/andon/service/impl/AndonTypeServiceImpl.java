package com.badminton.mes.module.andon.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.convert.AndonTypeConvert;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;
import com.badminton.mes.module.andon.dal.redis.AndonCache;
import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
import com.badminton.mes.module.andon.dal.repository.AndonConfigurationRepository;
import com.badminton.mes.module.andon.dal.repository.AndonEventRepository;
import com.badminton.mes.module.andon.dal.repository.AndonReasonRepository;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;
import com.badminton.mes.module.andon.dal.repository.AndonTypeSpecifications;
import com.badminton.mes.module.andon.service.AndonTypeService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 安灯类型维护实现。
 *
 * <p>类型决定事件采用无须处理、自处理或协助处理模式，并承载响应时限、责任角色、通知渠道及
 * 模拟灯控开关。协助模式的默认规则必须完整，数据库唯一约束与应用层校验共同保证有效类型编码唯一。
 *
 * <p>类型被原因、配置或事件引用后禁止删除；更新成功后，不仅失效类型详情缓存，还失效所有嵌入
 * 类型编码和名称的原因、配置、事件详情缓存。缓存删除延迟到事务提交后执行，避免回滚事务污染缓存。
 */
@Service
public class AndonTypeServiceImpl implements AndonTypeService {

    private static final String HANDLING_MODE_ASSISTANCE = "ASSISTANCE";
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DELETED_CODE_PREFIX = "__DELETED_";

    private final AndonTypeRepository typeRepository;
    private final AndonReasonRepository reasonRepository;
    private final AndonConfigurationRepository configurationRepository;
    private final AndonEventRepository eventRepository;
    private final AndonCache andonCache;

    /** 注入类型仓储及用于引用保护、聚合缓存失效的关联仓储。 */
    public AndonTypeServiceImpl(AndonTypeRepository typeRepository,
                                AndonReasonRepository reasonRepository,
                                AndonConfigurationRepository configurationRepository,
                                AndonEventRepository eventRepository,
                                AndonCache andonCache) {
        this.typeRepository = typeRepository;
        this.reasonRepository = reasonRepository;
        this.configurationRepository = configurationRepository;
        this.eventRepository = eventRepository;
        this.andonCache = andonCache;
    }

    /** 创建类型，校验编码和处理规则，并为可空状态字段应用业务默认值。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createType(AndonTypeSaveReqVO request) {
        validateTypeCodeUnique(request.getTypeCode(), null);
        validateHandlingRule(request);
        AndonTypeEntity type = AndonTypeConvert.toEntity(request);
        type.setLightControlEnabled(Boolean.TRUE.equals(request.getLightControlEnabled()));
        type.setEnabledStatus(request.getEnabledStatus() == null ? ENABLED : request.getEnabledStatus());
        type.setCreateBy(getCurrentOperatorId());
        type.setDeleted(false);
        saveType(type);
        return type.getId();
    }

    /** 加锁更新类型；请求未提供启用状态或灯控开关时保留持久化原值。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(Long id, AndonTypeSaveReqVO request) {
        AndonTypeEntity type = getTypeForUpdate(id);
        validateTypeCodeUnique(request.getTypeCode(), id);
        validateHandlingRule(request);
        Integer previousEnabledStatus = type.getEnabledStatus();
        Boolean previousLightControlEnabled = type.getLightControlEnabled();
        AndonTypeConvert.copyEditableFields(request, type);
        if (request.getEnabledStatus() == null) {
            type.setEnabledStatus(previousEnabledStatus);
        }
        if (request.getLightControlEnabled() == null) {
            type.setLightControlEnabled(previousLightControlEnabled);
        }
        saveType(type);
        evictTypeAggregateCacheAfterCommit(id);
    }

    /**
     * 仅删除无任何业务引用的类型；逻辑删除前改写编码，释放原编码并规避未过滤删除数据的唯一约束。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        AndonTypeEntity type = getTypeForUpdate(id);
        boolean hasReferences = typeRepository.countConfigurationsByTypeId(id) > 0
                || typeRepository.countReasonsByTypeId(id) > 0
                || typeRepository.countEventsByTypeId(id) > 0;
        if (hasReferences) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_HAS_REFERENCES);
        }
        String deletedCode = DELETED_CODE_PREFIX + Long.toString(id, 36).toUpperCase();
        if (typeRepository.existsByTypeCode(deletedCode)) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_CODE_DUPLICATE);
        }
        type.setTypeCode(deletedCode);
        type.setEnabledStatus(DISABLED);
        type.setDeleted(true);
        saveType(type);
        evictTypeCacheAfterCommit(id);
    }

    /** 通过详情缓存读取类型，未命中时加载未删除实体并显式转换。 */
    @Override
    @Transactional(readOnly = true)
    public AndonTypeRespVO getType(Long id) {
        return andonCache.getOrLoadDetail(AndonRedisKeyConstants.TYPE_RESOURCE,
                id, AndonTypeRespVO.class, () -> {
            AndonTypeRespVO response = AndonTypeConvert.toRespVO(getTypeEntity(id));
            return response;
        });
    }

    /** 分页查询类型；请求页码超过末页时收敛到最后一页，并按主键倒序返回。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AndonTypeRespVO> getTypePage(AndonTypePageReqVO request) {
        var specification = AndonTypeSpecifications.page(request);
        long total = typeRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<AndonTypeEntity> page = typeRepository.findAll(specification, pageRequest);
        List<AndonTypeRespVO> list = AndonTypeConvert.toRespVOList(page.getContent());
        return PageResult.of(list, total, pageNo, pageSize);
    }

    /** 协助模式必须同时声明响应分钟数、默认责任角色和至少一个通知渠道。 */
    private void validateHandlingRule(AndonTypeSaveReqVO request) {
        if (HANDLING_MODE_ASSISTANCE.equals(request.getHandlingMode())
                && (request.getResponseMinutes() == null
                || !StringUtils.hasText(request.getResponsibleRoleCode())
                || !StringUtils.hasText(request.getNotificationChannels()))) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_RULE_INVALID);
        }
    }

    /** 校验未删除类型中的业务编码唯一性；更新时排除当前记录。 */
    private void validateTypeCodeUnique(String typeCode, Long excludedId) {
        boolean exists = excludedId == null
                ? typeRepository.existsByTypeCodeAndDeletedFalse(typeCode)
                : typeRepository.existsByTypeCodeAndIdNotAndDeletedFalse(typeCode, excludedId);
        if (exists) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_CODE_DUPLICATE);
        }
    }

    private AndonTypeEntity getTypeEntity(Long id) {
        return typeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    /** 以行锁读取类型，串行化同一类型的更新和删除。 */
    private AndonTypeEntity getTypeForUpdate(Long id) {
        return typeRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    /** 立即刷新以捕获并发写入触发的唯一约束异常，并转换为稳定业务错误。 */
    private void saveType(AndonTypeEntity type) {
        try {
            typeRepository.saveAndFlush(type);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_CODE_DUPLICATE);
        }
    }

    /** 在事务成功提交后失效类型详情，回滚时保留原缓存。 */
    private void evictTypeCacheAfterCommit(Long id) {
        andonCache.evictDetailAfterCommit(AndonRedisKeyConstants.TYPE_RESOURCE, id);
    }

    /**
     * 类型展示字段变化会影响多个聚合响应，因此级联失效类型及其原因、配置、事件详情缓存。
     */
    private void evictTypeAggregateCacheAfterCommit(Long typeId) {
        evictTypeCacheAfterCommit(typeId);
        andonCache.evictDetailsAfterCommit(AndonRedisKeyConstants.REASON_RESOURCE,
                reasonRepository.findIdsByAndonTypeIdAndDeletedFalse(typeId));
        andonCache.evictDetailsAfterCommit(AndonRedisKeyConstants.CONFIGURATION_RESOURCE,
                configurationRepository.findIdsByAndonTypeIdAndDeletedFalse(typeId));
        andonCache.evictDetailsAfterCommit(AndonRedisKeyConstants.EVENT_RESOURCE,
                eventRepository.findIdsByAndonTypeIdAndDeletedFalse(typeId));
    }

    /** 无登录上下文时使用系统默认操作者，兼容初始化和后台调用。 */
    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
