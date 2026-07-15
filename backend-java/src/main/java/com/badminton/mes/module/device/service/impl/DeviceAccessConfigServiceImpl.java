package com.badminton.mes.module.device.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.device.constants.DeviceErrorCodeConstants;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;
import com.badminton.mes.module.device.convert.DeviceAccessConfigConvert;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.device.dal.redis.DeviceCache;
import com.badminton.mes.module.device.dal.redis.DeviceRedisKeyConstants;
import com.badminton.mes.module.device.dal.repository.DeviceAccessConfigRepository;
import com.badminton.mes.module.device.dal.repository.DeviceAccessConfigSpecifications;
import com.badminton.mes.module.device.dal.repository.DeviceCommissioningRecordRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCountRecordRepository;
import com.badminton.mes.module.device.service.DeviceAccessConfigService;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.service.EquipmentLedgerService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备接入配置 Service 实现。
 *
 * <p>该实现集中维护配置默认值、唯一性、设备可用性、联调启用约束和逻辑删除规则。
 * 写操作均在事务中完成；涉及并发修改时先悲观锁定配置，并将详情缓存失效延迟到事务提交后，
 * 避免其他请求读取到尚未提交或最终回滚的中间状态。</p>
 */
@Service
public class DeviceAccessConfigServiceImpl implements DeviceAccessConfigService {

    private static final String DATA_SOURCE_HTTP_API = "HTTP_API";
    private static final String COUNT_MODE_CUMULATIVE = "CUMULATIVE";
    private static final String REPORT_MODE_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    private static final String COMMISSIONING_NOT_TESTED = "NOT_TESTED";
    private static final String COMMISSIONING_PASSED = "PASSED";
    private static final String EQUIPMENT_SCRAPPED = "SCRAPPED";
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DELETED_CODE_PREFIX = "__DELETED_";

    private final DeviceAccessConfigRepository configRepository;
    private final DeviceCommissioningRecordRepository commissioningRepository;
    private final DeviceCountRecordRepository countRecordRepository;
    private final EquipmentLedgerService equipmentLedgerService;
    private final DeviceCache deviceCache;

    public DeviceAccessConfigServiceImpl(DeviceAccessConfigRepository configRepository,
                                         DeviceCommissioningRecordRepository commissioningRepository,
                                         DeviceCountRecordRepository countRecordRepository,
                                         EquipmentLedgerService equipmentLedgerService,
                                         DeviceCache deviceCache) {
        this.configRepository = configRepository;
        this.commissioningRepository = commissioningRepository;
        this.countRecordRepository = countRecordRepository;
        this.equipmentLedgerService = equipmentLedgerService;
        this.deviceCache = deviceCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccessConfig(DeviceAccessConfigSaveReqVO request) {
        // 应用层预检用于返回明确业务错误；最终唯一性仍由数据库约束兜底，防止并发穿透。
        validateUniqueFields(request, null);
        validateEquipmentAvailable(request.getEquipmentId());

        // 接入协议由当前 HTTP 接口固定；空白业务选项在服务端统一补默认值，避免依赖调用方约定。
        DeviceAccessConfigEntity config = DeviceAccessConfigConvert.toEntity(request);
        config.setDataSource(DATA_SOURCE_HTTP_API);
        config.setCountMode(request.getCountMode() == null ? COUNT_MODE_CUMULATIVE : request.getCountMode());
        config.setReportMode(request.getReportMode() == null
                ? REPORT_MODE_PENDING_CONFIRMATION : request.getReportMode());
        config.setCommissioningStatus(COMMISSIONING_NOT_TESTED);
        config.setEnabledStatus(DISABLED);
        config.setDeleted(false);
        config.setCreateBy(getCurrentOperatorId());
        saveConfig(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccessConfig(Long id, DeviceAccessConfigSaveReqVO request) {
        // 先悲观锁定当前配置，使唯一性校验、启用判断和最终写入处于同一串行化修改窗口。
        DeviceAccessConfigEntity config = getConfigForUpdate(id);
        validateUniqueFields(request, id);
        validateEquipmentAvailable(request.getEquipmentId());
        // “启用”是联调通过后的显式动作；未联调或联调失败的配置不能绕过状态机直接采集。
        if (Integer.valueOf(ENABLED).equals(request.getEnabledStatus())
                && !COMMISSIONING_PASSED.equals(config.getCommissioningStatus())) {
            throw new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_ENABLE_NOT_ALLOWED);
        }

        // 转换器只覆盖允许编辑的字段，数据来源、联调状态等服务端维护字段不会被请求污染。
        DeviceAccessConfigConvert.copyEditableFields(request, config);
        if (config.getCountMode() == null) {
            config.setCountMode(COUNT_MODE_CUMULATIVE);
        }
        if (config.getReportMode() == null) {
            config.setReportMode(REPORT_MODE_PENDING_CONFIRMATION);
        }
        if (config.getEnabledStatus() == null) {
            config.setEnabledStatus(DISABLED);
        }
        saveConfig(config);
        // 仅在事务成功提交后删除缓存；回滚事务不会误删仍然有效的旧快照。
        evictAccessConfigCacheAfterCommit(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccessConfig(Long id) {
        // 锁内判断历史引用，避免删除检查与配置状态修改之间发生并发覆盖。
        DeviceAccessConfigEntity config = getConfigForUpdate(id);
        boolean hasHistory = commissioningRepository.countByAccessConfigId(id) > 0
                || countRecordRepository.countByAccessConfigId(id) > 0;
        if (hasHistory) {
            throw new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_HAS_HISTORY);
        }

        // 逻辑删除时改写唯一编码，既保留审计实体，又允许原业务编码被后续新配置复用。
        String deletedCode = DELETED_CODE_PREFIX + Long.toString(id, 36).toUpperCase();
        if (configRepository.existsByConfigCode(deletedCode)) {
            throw new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_CODE_DUPLICATE);
        }
        config.setConfigCode(deletedCode);
        config.setEnabledStatus(DISABLED);
        config.setDeleted(true);
        saveConfig(config);
        evictAccessConfigCacheAfterCommit(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceAccessConfigRespVO getAccessConfig(Long id) {
        // 详情采用缓存旁路模式：命中直接返回快照，未命中才查询未删除实体并回填缓存。
        return deviceCache.getOrLoadDetail(DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE,
                id, DeviceAccessConfigRespVO.class, () -> {
            DeviceAccessConfigRespVO response = DeviceAccessConfigConvert.toRespVO(getConfig(id));
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeviceAccessConfigRespVO> getAccessConfigPage(DeviceAccessConfigPageReqVO request) {
        // 先计数再钳制页码，保证请求超过末页时仍返回最后一页，而不是制造语义含混的空页。
        var specification = DeviceAccessConfigSpecifications.page(request);
        long total = configRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<DeviceAccessConfigEntity> page = configRepository.findAll(specification, pageRequest);
        List<DeviceAccessConfigRespVO> list = DeviceAccessConfigConvert.toRespVOList(page.getContent());
        return PageResult.of(list, total, pageNo, pageSize);
    }

    private DeviceAccessConfigEntity getConfig(Long id) {
        return configRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_NOT_EXISTS));
    }

    private DeviceAccessConfigEntity getConfigForUpdate(Long id) {
        // Repository 的 ForUpdate 查询持有数据库行级悲观锁，锁释放边界由外层事务决定。
        return configRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_NOT_EXISTS));
    }

    private void validateUniqueFields(DeviceAccessConfigSaveReqVO request, Long excludedId) {
        // 更新场景排除自身主键；编码唯一与“设备 + 采集点”唯一分别提供不同的领域错误。
        boolean codeExists = excludedId == null
                ? configRepository.existsByConfigCodeAndDeletedFalse(request.getConfigCode())
                : configRepository.existsByConfigCodeAndIdNotAndDeletedFalse(request.getConfigCode(), excludedId);
        if (codeExists) {
            throw new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_CODE_DUPLICATE);
        }
        boolean pointExists = excludedId == null
                ? configRepository.existsByEquipmentIdAndCollectionPointCodeAndDeletedFalse(
                        request.getEquipmentId(), request.getCollectionPointCode())
                : configRepository.existsByEquipmentIdAndCollectionPointCodeAndIdNotAndDeletedFalse(
                        request.getEquipmentId(), request.getCollectionPointCode(), excludedId);
        if (pointExists) {
            throw new ServiceException(DeviceErrorCodeConstants.COLLECTION_POINT_DUPLICATE);
        }
    }

    private void validateEquipmentAvailable(Long equipmentId) {
        // 接入配置只能绑定业务启用且未报废的设备，防止为不可生产设备建立采集入口。
        EquipmentLedgerRespVO equipment = equipmentLedgerService.getEquipmentLedger(equipmentId);
        if (!Integer.valueOf(ENABLED).equals(equipment.getStatus())
                || EQUIPMENT_SCRAPPED.equals(equipment.getEquipmentStatus())) {
            throw new ServiceException(DeviceErrorCodeConstants.EQUIPMENT_NOT_AVAILABLE);
        }
    }

    private Long getCurrentOperatorId() {
        // 无登录上下文通常出现在系统初始化或受控内部调用，使用约定账号保证审计字段完整。
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }

    private void saveConfig(DeviceAccessConfigEntity config) {
        try {
            // 立即 flush 让数据库唯一约束在本方法内触发，并统一映射为稳定的领域错误码。
            configRepository.saveAndFlush(config);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_CODE_DUPLICATE);
        }
    }

    private void evictAccessConfigCacheAfterCommit(Long id) {
        // 失效动作注册到事务提交阶段，避免数据库回滚后缓存却被提前清除或短暂重建为脏数据。
        deviceCache.evictDetailAfterCommit(DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE, id);
    }
}
