package com.badminton.mes.module.andon.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;
import com.badminton.mes.module.andon.convert.AndonReasonConvert;
import com.badminton.mes.module.andon.dal.entity.AndonReasonEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;
import com.badminton.mes.module.andon.dal.repository.AndonReasonRepository;
import com.badminton.mes.module.andon.dal.repository.AndonReasonSpecifications;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;
import com.badminton.mes.module.andon.service.AndonReasonService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 安灯异常原因 Service 实现。 */
@Service
public class AndonReasonServiceImpl implements AndonReasonService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DELETED_CODE_PREFIX = "__DELETED_";

    private final AndonReasonRepository reasonRepository;
    private final AndonTypeRepository typeRepository;

    public AndonReasonServiceImpl(
            AndonReasonRepository reasonRepository,
            AndonTypeRepository typeRepository) {
        this.reasonRepository = reasonRepository;
        this.typeRepository = typeRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReason(AndonReasonSaveReqVO request) {
        lockType(request.getAndonTypeId());
        validateReasonCodeUnique(request.getReasonCode(), null);

        AndonReasonEntity reason = AndonReasonConvert.toEntity(request);
        reason.setEnabledStatus(request.getEnabledStatus() == null ? ENABLED : request.getEnabledStatus());
        reason.setCreateBy(getCurrentOperatorId());
        reason.setDeleted(false);
        saveReason(reason);
        return reason.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReason(Long id, AndonReasonSaveReqVO request) {
        AndonReasonEntity reasonSnapshot = getReasonEntity(id);
        lockTypes(reasonSnapshot.getAndonTypeId(), request.getAndonTypeId());
        AndonReasonEntity reason = getReasonForUpdate(id);
        validateReasonCodeUnique(request.getReasonCode(), id);
        boolean changesReferencedReasonType = !reason.getAndonTypeId().equals(request.getAndonTypeId())
                && reasonRepository.countEventReferences(id) > 0;
        if (changesReferencedReasonType) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_HAS_EVENTS);
        }

        Integer previousEnabledStatus = reason.getEnabledStatus();
        AndonReasonConvert.copyEditableFields(request, reason);
        if (request.getEnabledStatus() == null) {
            reason.setEnabledStatus(previousEnabledStatus);
        }
        saveReason(reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReason(Long id) {
        AndonReasonEntity reasonSnapshot = getReasonEntity(id);
        lockType(reasonSnapshot.getAndonTypeId());
        AndonReasonEntity reason = getReasonForUpdate(id);
        if (reasonRepository.countEventReferences(id) > 0) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_HAS_EVENTS);
        }

        String deletedCode = DELETED_CODE_PREFIX + Long.toString(id, 36).toUpperCase();
        if (reasonRepository.existsByReasonCode(deletedCode)) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_CODE_DUPLICATE);
        }
        reason.setReasonCode(deletedCode);
        reason.setEnabledStatus(DISABLED);
        reason.setDeleted(true);
        saveReason(reason);
    }

    @Override
    @Transactional(readOnly = true)
    public AndonReasonRespVO getReason(Long id) {
        AndonReasonEntity reason = getReasonEntity(id);
        AndonTypeEntity andonType = getTypeEntity(reason.getAndonTypeId());
        return AndonReasonConvert.toRespVO(reason, andonType);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AndonReasonRespVO> getReasonPage(AndonReasonPageReqVO request) {
        var specification = AndonReasonSpecifications.page(request);
        long total = reasonRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }

        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "id"));
        Page<AndonReasonEntity> page = reasonRepository.findAll(specification, pageRequest);
        Map<Long, AndonTypeEntity> typesById = typeRepository.findAllById(
                        page.getContent().stream()
                                .map(AndonReasonEntity::getAndonTypeId)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(AndonTypeEntity::getId, Function.identity()));
        List<AndonReasonRespVO> list = page.getContent().stream()
                .map(reason -> AndonReasonConvert.toRespVO(
                        reason,
                        requireType(typesById, reason.getAndonTypeId())))
                .toList();
        return PageResult.of(list, total, pageNo, pageSize);
    }

    private void validateReasonCodeUnique(String reasonCode, Long excludedId) {
        boolean exists = excludedId == null
                ? reasonRepository.existsByReasonCodeAndDeletedFalse(reasonCode)
                : reasonRepository.existsByReasonCodeAndIdNotAndDeletedFalse(reasonCode, excludedId);
        if (exists) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_CODE_DUPLICATE);
        }
    }

    private void validateTypeExists(Long typeId) {
        getTypeEntity(typeId);
    }

    private void lockTypes(Long firstTypeId, Long secondTypeId) {
        if (firstTypeId.equals(secondTypeId)) {
            lockType(firstTypeId);
            return;
        }
        Long lowerTypeId = Math.min(firstTypeId, secondTypeId);
        Long higherTypeId = Math.max(firstTypeId, secondTypeId);
        lockType(lowerTypeId);
        lockType(higherTypeId);
    }

    private void lockType(Long typeId) {
        typeRepository.findByIdAndDeletedFalseForUpdate(typeId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    private AndonTypeEntity getTypeEntity(Long typeId) {
        return typeRepository.findByIdAndDeletedFalse(typeId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    private AndonTypeEntity requireType(Map<Long, AndonTypeEntity> typesById, Long typeId) {
        AndonTypeEntity andonType = typesById.get(typeId);
        if (andonType == null || Boolean.TRUE.equals(andonType.getDeleted())) {
            throw new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS);
        }
        return andonType;
    }

    private AndonReasonEntity getReasonEntity(Long id) {
        return reasonRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.REASON_NOT_EXISTS));
    }

    private AndonReasonEntity getReasonForUpdate(Long id) {
        return reasonRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.REASON_NOT_EXISTS));
    }

    private void saveReason(AndonReasonEntity reason) {
        try {
            reasonRepository.saveAndFlush(reason);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(AndonErrorCodeConstants.REASON_CODE_DUPLICATE);
        }
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
