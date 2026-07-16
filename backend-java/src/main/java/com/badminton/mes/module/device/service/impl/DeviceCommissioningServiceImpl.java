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

/**
 * 设备联调记录 Service 实现。
 *
 * <p>每次联调都追加一条不可变历史记录，同时把最新结论投影到接入配置。
 * 联调结论与配置启用状态在同一事务内更新，并通过悲观锁避免并发联调相互覆盖；
 * 配置缓存只在事务提交后失效。</p>
 */
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
        // 锁定接入配置，使“新增联调事实 + 刷新当前联调状态”作为一个不可分割的状态迁移执行。
        DeviceAccessConfigEntity config = configRepository
                .findByIdAndDeletedFalseForUpdate(request.getAccessConfigId())
                .orElseThrow(() -> new ServiceException(DeviceErrorCodeConstants.ACCESS_CONFIG_NOT_EXISTS));
        // 联调记录代表已经发生的测试事实，不接受未来时间，避免状态提前生效。
        if (request.getTestTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException(DeviceErrorCodeConstants.COMMISSIONING_TIME_INVALID);
        }

        // 测试人与创建人取同一操作人快照，保证联调记录具备完整审计归属。
        Long testerUserId = getCurrentOperatorId();
        DeviceCommissioningRecordEntity record = DeviceCommissioningConvert.toEntity(request);
        record.setTesterUserId(testerUserId);
        record.setCreateBy(testerUserId);
        commissioningRepository.saveAndFlush(record);

        // 配置保存最近一次联调结论；任何非通过结论都会立即撤销启用资格，阻断后续计数接入。
        config.setCommissioningStatus(request.getTestResult());
        if (!COMMISSIONING_PASSED.equals(request.getTestResult())) {
            config.setEnabledStatus(DISABLED);
        }
        configRepository.save(config);
        // 延迟到事务提交后再失效配置详情，避免缓存观察到未提交或已回滚的联调状态。
        deviceCache.evictDetailAfterCommit(DeviceRedisKeyConstants.ACCESS_CONFIG_RESOURCE, config.getId());
        return record.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceCommissioningRespVO getCommissioningRecord(Long id) {
        // 联调详情使用缓存旁路读取，缓存内容是实体转换后的历史快照。
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
        // 先统计总量并把越界页钳制到末页，排序同时使用测试时间与主键保证结果稳定。
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
        // 受控内部调用缺少登录上下文时使用约定账号，避免测试人和审计字段为空。
        if (SecurityContextHolder.getLoginUser() == null) {
            return DEFAULT_OPERATOR_ID;
        }
        return SecurityContextHolder.getRequiredLoginUserId();
    }
}
