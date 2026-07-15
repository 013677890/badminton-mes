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

/** 安灯类型 Service 实现。 */
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

    @Override
    @Transactional(readOnly = true)
    public AndonTypeRespVO getType(Long id) {
        return andonCache.getOrLoadDetail(AndonRedisKeyConstants.TYPE_RESOURCE,
                id, AndonTypeRespVO.class, () -> {
            AndonTypeRespVO response = AndonTypeConvert.toRespVO(getTypeEntity(id));
            return response;
        });
    }

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

    private void validateHandlingRule(AndonTypeSaveReqVO request) {
        if (HANDLING_MODE_ASSISTANCE.equals(request.getHandlingMode())
                && (request.getResponseMinutes() == null
                || !StringUtils.hasText(request.getResponsibleRoleCode())
                || !StringUtils.hasText(request.getNotificationChannels()))) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_RULE_INVALID);
        }
    }

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

    private AndonTypeEntity getTypeForUpdate(Long id) {
        return typeRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    private void saveType(AndonTypeEntity type) {
        try {
            typeRepository.saveAndFlush(type);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_CODE_DUPLICATE);
        }
    }

    private void evictTypeCacheAfterCommit(Long id) {
        andonCache.evictDetailAfterCommit(AndonRedisKeyConstants.TYPE_RESOURCE, id);
    }

    private void evictTypeAggregateCacheAfterCommit(Long typeId) {
        evictTypeCacheAfterCommit(typeId);
        andonCache.evictDetailsAfterCommit(AndonRedisKeyConstants.REASON_RESOURCE,
                reasonRepository.findIdsByAndonTypeIdAndDeletedFalse(typeId));
        andonCache.evictDetailsAfterCommit(AndonRedisKeyConstants.CONFIGURATION_RESOURCE,
                configurationRepository.findIdsByAndonTypeIdAndDeletedFalse(typeId));
        andonCache.evictDetailsAfterCommit(AndonRedisKeyConstants.EVENT_RESOURCE,
                eventRepository.findIdsByAndonTypeIdAndDeletedFalse(typeId));
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
