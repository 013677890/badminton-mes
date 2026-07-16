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
import com.badminton.mes.module.andon.dal.redis.AndonCache;
import com.badminton.mes.module.andon.dal.redis.AndonRedisKeyConstants;
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

/**
 * 安灯协助处理配置维护实现。
 *
 * <p>配置按“安灯类型 + 作用范围”唯一。{@code productionLineId == null} 表示全局规则，持久化时以
 * {@code scopeLineId = 0} 统一参与唯一约束；非空值表示具体产线规则，事件创建时优先于全局规则。
 *
 * <p>规则至少包含处理用户或处理角色。升级分钟数必须与升级用户/角色成组配置，并且严格晚于响应
 * 分钟数。更新和删除会锁定类型并拒绝存在活动事件的变更，防止处理中事件的责任、时限语义漂移；
 * 详情缓存只在事务提交后失效。
 */
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
    private final AndonCache andonCache;

    /** 注入配置、类型、责任主体查询服务及详情缓存。 */
    public AndonConfigurationServiceImpl(
            AndonConfigurationRepository configurationRepository,
            AndonTypeRepository typeRepository,
            UserService userService,
            RoleService roleService,
            AndonCache andonCache) {
        this.configurationRepository = configurationRepository;
        this.typeRepository = typeRepository;
        this.userService = userService;
        this.roleService = roleService;
        this.andonCache = andonCache;
    }

    /** 锁定类型，验证责任与时限规则、范围唯一性后创建配置。 */
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

    /**
     * 更新配置时禁止更换安灯类型，并拒绝修改仍有活动事件的类型规则。
     */
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
        evictConfigurationCacheAfterCommit(id);
    }

    /**
     * 无活动事件时逻辑删除配置；将作用域哨兵改为负主键，以释放原“类型 + 范围”唯一键。
     */
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
        evictConfigurationCacheAfterCommit(id);
    }

    /** 读取配置详情并装配类型展示信息，聚合作为一个详情缓存单元。 */
    @Override
    @Transactional(readOnly = true)
    public AndonConfigurationRespVO getConfiguration(Long id) {
        return andonCache.getOrLoadDetail(AndonRedisKeyConstants.CONFIGURATION_RESOURCE,
                id, AndonConfigurationRespVO.class, () -> {
            AndonConfigurationEntity configuration = getConfigurationEntity(id);
            AndonTypeEntity andonType = getTypeEntity(configuration.getAndonTypeId());
            AndonConfigurationRespVO response = AndonConfigurationConvert.toRespVO(configuration, andonType);
            return response;
        });
    }

    /** 分页查询配置，并批量预取类型以避免逐条关联查询。 */
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

    /**
     * 校验处理对象、升级对象和响应/升级时限的组合关系，并确认引用的用户与角色处于启用状态。
     */
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

    /** 要求配置引用的责任用户处于启用状态。 */
    private void validateEnabledUser(Long userId) {
        UserRespVO user = userService.getUser(userId);
        if (!Integer.valueOf(ENABLED).equals(user.getStatus())) {
            throw new ServiceException(AndonErrorCodeConstants.RESPONSIBLE_SUBJECT_INVALID);
        }
    }

    /** 要求配置引用的角色存在于当前启用角色集合。 */
    private void validateEnabledRole(String roleCode) {
        boolean roleExists = roleService.getEnabledRoles().stream()
                .map(RoleRespVO::getRoleCode)
                .anyMatch(roleCode::equals);
        if (!roleExists) {
            throw new ServiceException(AndonErrorCodeConstants.RESPONSIBLE_SUBJECT_INVALID);
        }
    }

    /**
     * 将空产线规范为全局范围哨兵值，校验同一类型在同一全局或产线范围内只有一条未删除配置。
     */
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

    /** 通过统一实体读取语义验证安灯类型存在。 */
    private void validateTypeExists(Long typeId) {
        getTypeEntity(typeId);
    }

    /** 锁定类型，串行化该类型下配置与其他依赖类型规则的写操作。 */
    private void lockType(Long typeId) {
        typeRepository.findByIdAndDeletedFalseForUpdate(typeId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    private AndonTypeEntity getTypeEntity(Long typeId) {
        return typeRepository.findByIdAndDeletedFalse(typeId)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.TYPE_NOT_EXISTS));
    }

    /** 从分页批量预取结果中取得有效类型，避免返回缺少类型展示信息的配置。 */
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

    /** 以行锁读取配置，串行化同一规则的更新和删除。 */
    private AndonConfigurationEntity getConfigurationForUpdate(Long id) {
        return configurationRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(AndonErrorCodeConstants.CONFIGURATION_NOT_EXISTS));
    }

    /** 刷新写入并把数据库并发唯一约束冲突转换为范围重复业务异常。 */
    private void saveConfiguration(AndonConfigurationEntity configuration) {
        try {
            configurationRepository.saveAndFlush(configuration);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(AndonErrorCodeConstants.CONFIGURATION_SCOPE_DUPLICATE);
        }
    }

    /** 在事务成功提交后失效配置详情缓存。 */
    private void evictConfigurationCacheAfterCommit(Long id) {
        andonCache.evictDetailAfterCommit(AndonRedisKeyConstants.CONFIGURATION_RESOURCE, id);
    }

    /** 无登录上下文时使用系统默认操作者，兼容初始化和后台调用。 */
    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
