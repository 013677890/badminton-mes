package com.badminton.mes.module.andon.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.andon.constants.AndonErrorCodeConstants;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;
import com.badminton.mes.module.andon.convert.AndonConfigurationConvert;
import com.badminton.mes.module.andon.dal.entity.AndonConfigurationEntity;
import com.badminton.mes.module.andon.dal.entity.AndonTypeEntity;
import com.badminton.mes.module.andon.dal.repository.AndonConfigurationRepository;
import com.badminton.mes.module.andon.dal.repository.AndonConfigurationSpecifications;
import com.badminton.mes.module.andon.dal.repository.AndonTypeRepository;
import com.badminton.mes.module.andon.service.AndonConfigurationService;
import com.badminton.mes.module.system.controller.vo.RoleRespVO;
import com.badminton.mes.module.system.controller.vo.UserRespVO;
import com.badminton.mes.module.system.service.RoleService;
import com.badminton.mes.module.system.service.UserService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 安灯异常处理配置 Service 实现。 */
@Service
public class AndonConfigurationServiceImpl implements AndonConfigurationService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final long GLOBAL_SCOPE_LINE_ID = 0L;
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    private final AndonConfigurationRepository configurationRepository;
    private final AndonTypeRepository typeRepository;
    private final UserService userService;
    private final RoleService roleService;

    public AndonConfigurationServiceImpl(
            AndonConfigurationRepository configurationRepository,
            AndonTypeRepository typeRepository,
            UserService userService,
            RoleService roleService) {
        this.configurationRepository = configurationRepository;
        this.typeRepository = typeRepository;
        this.userService = userService;
        this.roleService = roleService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfiguration(AndonConfigurationSaveReqVO request) {
        lockType(request.getAndonTypeId());
        validateConfigurationRule(request);
        validateScopeUnique(request, null);

        AndonConfigurationEntity configuration = AndonConfigurationConvert.toEntity(request);
        configuration.setEnabledStatus(request.getEnabledStatus() == null ? ENABLED : request.getEnabledStatus());
        configuration.setCreateBy(getCurrentOperatorId());
        configuration.setDeleted(false);
        saveConfiguration(configuration);
        return configuration.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfiguration(Long id, AndonConfigurationSaveReqVO request) {
        AndonConfigurationEntity configurationSnapshot = getConfigurationEntity(id);
        if (!configurationSnapshot.getAndonTypeId().equals(request.getAndonTypeId())) {
            throw new ServiceException(AndonErrorCodeConstants.CONFIGURATION_RULE_INVALID);
        }
        lockType(configurationSnapshot.getAndonTypeId());
        AndonConfigurationEntity configuration = getConfigurationForUpdate(id);
        if (configurationRepository.countActiveEventsByAndonTypeId(configuration.getAndonTypeId()) > 0) {
            throw new ServiceException(AndonErrorCodeConstants.CONFIGURATION_HAS_ACTIVE_EVENTS);
        }
        validateConfigurationRule(request);
        validateScopeUnique(request, id);

        Integer previousEnabledStatus = configuration.getEnabledStatus();
        AndonConfigurationConvert.copyEditableFields(request, configuration);
        if (request.getEnabledStatus() == null) {
            configuration.setEnabledStatus(previousEnabledStatus);
        }
        saveConfiguration(configuration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfiguration(Long id) {
        AndonConfigurationEntity configurationSnapshot = getConfigurationEntity(id);
        lockType(configurationSnapshot.getAndonTypeId());
        AndonConfigurationEntity configuration = getConfigurationForUpdate(id);
        if (configurationRepository.countActiveEventsByAndonTypeId(configuration.getAndonTypeId()) > 0) {
            throw new ServiceException(AndonErrorCodeConstants.CONFIGURATION_HAS_ACTIVE_EVENTS);
        }
        configuration.setScopeLineId(-id);
        configuration.setEnabledStatus(DISABLED);
        configuration.setDeleted(true);
        saveConfiguration(configuration);
    }

    @Override
    @Transactional(readOnly = true)
    public AndonConfigurationRespVO getConfiguration(Long id) {
        AndonConfigurationEntity configuration = getConfigurationEntity(id);
        AndonTypeEntity andonType = getTypeEntity(configuration.getAndonTypeId());
        return AndonConfigurationConvert.toRespVO(configuration, andonType);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AndonConfigurationRespVO> getConfigurationPage(
            AndonConfigurationPageReqVO request) {
        var specification = AndonConfigurationSpecifications.page(request);
        long total = configurationRepository.count(specification);
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
        Page<AndonConfigurationEntity> page = configurationRepository.findAll(specification, pageRequest);
        Map<Long, AndonTypeEntity> typesById = typeRepository.findAllById(
                        page.getContent().stream()
                                .map(AndonConfigurationEntity::getAndonTypeId)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(AndonTypeEntity::getId, Function.identity()));
        List<AndonConfigurationRespVO> list = page.getContent().stream()
                .map(configuration -> AndonConfigurationConvert.toRespVO(
                        configuration,
                        requireType(typesById, configuration.getAndonTypeId())))
                .toList();
        return PageResult.of(list, total, pageNo, pageSize);
    }

    private void validateConfigurationRule(AndonConfigurationSaveReqVO request) {
        boolean hasHandlerUser = request.getHandlerUserId() != null;
        boolean hasHandlerRole = StringUtils.hasText(request.getHandlerRoleCode());
        if (!hasHandlerUser && !hasHandlerRole) {
            throw new ServiceException(AndonErrorCodeConstants.CONFIGURATION_RULE_INVALID);
        }
        if (hasHandlerUser) {
            validateEnabledUser(request.getHandlerUserId());
        }
        if (hasHandlerRole) {
            validateEnabledRole(request.getHandlerRoleCode());
        }

        boolean hasEscalationMinutes = request.getEscalationMinutes() != null;
        boolean hasEscalationUser = request.getEscalationUserId() != null;
        boolean hasEscalationRole = StringUtils.hasText(request.getEscalationRoleCode());
        boolean hasEscalationSubject = hasEscalationUser || hasEscalationRole;
        if (hasEscalationMinutes != hasEscalationSubject
                || (hasEscalationMinutes
                && request.getEscalationMinutes() <= request.getResponseMinutes())) {
            throw new ServiceException(AndonErrorCodeConstants.CONFIGURATION_RULE_INVALID);
        }
        if (hasEscalationUser) {
            validateEnabledUser(request.getEscalationUserId());
        }
        if (hasEscalationRole) {
            validateEnabledRole(request.getEscalationRoleCode());
        }
    }

    private void validateEnabledUser(Long userId) {
        UserRespVO user = userService.getUser(userId);
        if (!Integer.valueOf(ENABLED).equals(user.getStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.RESPONSIBLE_SUBJECT_INVALID);
        }
    }

    private void validateEnabledRole(String roleCode) {
        boolean roleExists = roleService.getEnabledRoles().stream()
                .map(RoleRespVO::getRoleCode)
                .anyMatch(roleCode::equals);
        if (!roleExists) {
            throw new ServiceException(AndonErrorCodeConstants.RESPONSIBLE_SUBJECT_INVALID);
        }
    }

    private void validateScopeUnique(AndonConfigurationSaveReqVO request, Long excludedId) {
        long scopeLineId = request.getProductionLineId() == null
                ? GLOBAL_SCOPE_LINE_ID : request.getProductionLineId();
        boolean exists = excludedId == null
                ? configurationRepository.existsByAndonTypeIdAndScopeLineIdAndDeletedFalse(
                        request.getAndonTypeId(), scopeLineId)
                : configurationRepository.existsByAndonTypeIdAndScopeLineIdAndIdNotAndDeletedFalse(
                        request.getAndonTypeId(), scopeLineId, excludedId);
        if (exists) {
            throw new ServiceException(AndonErrorCodeConstants.CONFIGURATION_SCOPE_DUPLICATE);
        }
    }

    private void validateTypeExists(Long typeId) {
        getTypeEntity(typeId);
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

    private AndonConfigurationEntity getConfigurationEntity(Long id) {
        return configurationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.CONFIGURATION_NOT_EXISTS));
    }

    private AndonConfigurationEntity getConfigurationForUpdate(Long id) {
        return configurationRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.CONFIGURATION_NOT_EXISTS));
    }

    private void saveConfiguration(AndonConfigurationEntity configuration) {
        try {
            configurationRepository.saveAndFlush(configuration);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(AndonErrorCodeConstants.CONFIGURATION_SCOPE_DUPLICATE);
        }
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
