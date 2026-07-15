package com.badminton.mes.module.device.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.device.constants.DeviceErrorCodeConstants;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningSaveReqVO;
import com.badminton.mes.module.device.convert.DeviceCommissioningConvert;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.device.dal.entity.DeviceCommissioningRecordEntity;
import com.badminton.mes.module.device.dal.redis.DeviceCache;
import com.badminton.mes.module.device.dal.redis.DeviceRedisKeyConstants;
import com.badminton.mes.module.device.dal.repository.DeviceAccessConfigRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCommissioningRecordRepository;
import com.badminton.mes.module.device.dal.repository.DeviceCommissioningSpecifications;
import com.badminton.mes.module.device.service.DeviceCommissioningService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设备联调记录 Service 实现。 */
@Service
public class DeviceCommissioningServiceImpl implements DeviceCommissioningService {

    private static final String COMMISSIONING_PASSED = "PASSED";
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final int DISABLED = 0;

    private final DeviceAccessConfigRepository configRepository;
    private final DeviceCommissioningRecordRepository commissioningRepository;
    private final DeviceCache deviceCache;

    public DeviceCommissioningServiceImpl(DeviceAccessConfigRepository configRepository,
                                           DeviceCommissioningRecordRepository commissioningRepository,
                                           DeviceCache deviceCache) {
        this.configRepository = configRepository;
        this.commissioningRepository = commissioningRepository;
        this.deviceCache = deviceCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCommissioningRecord(DeviceCommissioningSaveReqVO request) {
        DeviceAccessConfigEntity config = configRepository
                .findByIdAndDeletedFalseForUpdate(request.getAccessConfigId())
                .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_NOT_EXISTS));
        if (request.getTestTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException(DeviceErrorCodeConstants.COMMISSIONING_TIME_INVALID);
        }

        Long testerUserId = getCurrentOperatorId();
        DeviceCommissioningRecordEntity record = DeviceCommissioningConvert.toEntity(request);
        record.setTesterUserId(testerUserId);
        record.setCreateBy(testerUserId);
        commissioningRepository.saveAndFlush(record);

        config.setCommissioningStatus(request.getTestResult());
        if (!COMMISSIONING_PASSED.equals(request.getTestResult())) {
            config.setEnabledStatus(DISABLED);
        }
        configRepository.save(config);
        deviceCache.evictDetailAfterCommit(DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE, config.getId());
        return record.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceCommissioningRespVO getCommissioningRecord(Long id) {
        return deviceCache.getOrLoadDetail(DeviceRedisKeyConstants.COMMISSIONING_RECORD_RESOURCE,
                id, DeviceCommissioningRespVO.class, () -> {
            DeviceCommissioningRecordEntity record = commissioningRepository.findById(id)
                    .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.COMMISSIONING_RECORD_NOT_EXISTS));
            DeviceCommissioningRespVO response = DeviceCommissioningConvert.toRespVO(record);
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DeviceCommissioningRespVO> getCommissioningRecordPage(
            DeviceCommissioningPageReqVO request) {
        var specification = DeviceCommissioningSpecifications.page(request);
        long total = commissioningRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(request.getPageNo(), request.getPageSize());
        }
        int pageSize = request.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(request.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "testTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<DeviceCommissioningRecordEntity> page = commissioningRepository.findAll(specification, pageRequest);
        List<DeviceCommissioningRespVO> list = DeviceCommissioningConvert.toRespVOList(page.getContent());
        return PageResult.of(list, total, pageNo, pageSize);
    }

    private Long getCurrentOperatorId() {
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
