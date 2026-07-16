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
 * <p>负责接入编码/设备绑定唯一性、设备可用性和联调状态约束；已有计数数据的配置
 * 删除受保护，防止历史记录失去来源。详情缓存仅保存可重建数据。
 *
 * @author MES 开发组
 * @date 2026/07/16
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

    /** 创建接入配置，并初始化尚未联调的状态。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccessConfig(DeviceAccessConfigSaveReqVO request) {
        validateUniqueFields(request, null);
        validateEquipmentAvailable(request.getEquipmentId());

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

    /** 修改接入配置；关键绑定变化前校验设备和历史计数约束。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccessConfig(Long id, DeviceAccessConfigSaveReqVO request) {
        DeviceAccessConfigEntity config = getConfigForUpdate(id);
        validateUniqueFields(request, id);
        validateEquipmentAvailable(request.getEquipmentId());
        if (Integer.valueOf(ENABLED).equals(request.getEnabledStatus())
                && !COMMISSIONING_PASSED.equals(config.getCommissioningStatus())) {
            throw new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_ENABLE_NOT_ALLOWED);
        }

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
        evictAccessConfigCacheAfterCommit(id);
    }

    /** 在无有效计数引用时逻辑删除配置并释放唯一编码。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccessConfig(Long id) {
        DeviceAccessConfigEntity config = getConfigForUpdate(id);
        boolean hasHistory = commissioningRepository.countByAccessConfigId(id) > 0
                || countRecordRepository.countByAccessConfigId(id) > 0;
        if (hasHistory) {
            throw new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_HAS_HISTORY);
        }

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

    /** 查询接入配置详情，优先读取缓存。 */
    @Override
    @Transactional(readOnly = true)
    public DeviceAccessConfigRespVO getAccessConfig(Long id) {
        return deviceCache.getOrLoadDetail(DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE,
                id, DeviceAccessConfigRespVO.class, () -> {
            DeviceAccessConfigRespVO response = DeviceAccessConfigConvert.toRespVO(getConfig(id));
            return response;
        });
    }

    /** 分页查询设备接入配置。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<DeviceAccessConfigRespVO> getAccessConfigPage(DeviceAccessConfigPageReqVO request) {
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
        return configRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_NOT_EXISTS));
    }

    private void validateUniqueFields(DeviceAccessConfigSaveReqVO request, Long excludedId) {
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
        EquipmentLedgerRespVO equipment = equipmentLedgerService.getEquipmentLedger(equipmentId);
        if (!Integer.valueOf(ENABLED).equals(equipment.getStatus())
                || EQUIPMENT_SCRAPPED.equals(equipment.getEquipmentStatus())) {
            throw new ServiceException(DeviceErrorCodeConstants.EQUIPMENT_NOT_AVAILABLE);
        }
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }

    private void saveConfig(DeviceAccessConfigEntity config) {
        try {
            configRepository.saveAndFlush(config);
        } catch (DataIntegrityViolationException exception) {
            throw new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_CODE_DUPLICATE);
        }
    }

    private void evictAccessConfigCacheAfterCommit(Long id) {
        deviceCache.evictDetailAfterCommit(DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE, id);
    }
}
