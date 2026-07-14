package com.badminton.mes.module.integration.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.CompletionOrderPageReqVO;
import com.badminton.mes.module.integration.controller.vo.CompletionOrderRespVO;
import com.badminton.mes.module.integration.controller.vo.CompletionReadLogPageReqVO;
import com.badminton.mes.module.integration.controller.vo.CompletionReadLogRespVO;
import com.badminton.mes.module.integration.dal.entity.CompletionOrderEntity;
import com.badminton.mes.module.integration.dal.entity.CompletionReadLogEntity;
import com.badminton.mes.module.integration.dal.repository.CompletionOrderRepository;
import com.badminton.mes.module.integration.dal.repository.CompletionOrderSpecifications;
import com.badminton.mes.module.integration.dal.repository.CompletionReadLogRepository;
import com.badminton.mes.module.integration.dal.repository.CompletionReadLogSpecifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 已审核生产完工单读取与读取日志查询服务。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class CompletionOrderReadService {

    private final CompletionOrderRepository completionOrderRepository;

    private final CompletionReadLogRepository readLogRepository;

    /**
     * 构造生产完工单读取服务。
     *
     * @param completionOrderRepository 完工单 Repository
     * @param readLogRepository         读取日志 Repository
     */
    public CompletionOrderReadService(
            CompletionOrderRepository completionOrderRepository,
            CompletionReadLogRepository readLogRepository) {
        this.completionOrderRepository = completionOrderRepository;
        this.readLogRepository = readLogRepository;
    }

    /**
     * 分页读取已审核生产完工单，并为本页每条记录写读取日志。
     *
     * @param reqVO 分页筛选条件
     * @return 已审核完工单分页
     */
    @Transactional(rollbackFor = Exception.class)
    public PageResult<CompletionOrderRespVO> getCompletionOrderPage(
            CompletionOrderPageReqVO reqVO) {
        validateTimeRange(reqVO.getStartTime(), reqVO.getEndTime());
        Specification<CompletionOrderEntity> specification =
                CompletionOrderSpecifications.approvedPage(reqVO);
        long total = completionOrderRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        Page<CompletionOrderEntity> page =
                completionOrderRepository.findAll(specification, pageRequest);
        List<CompletionOrderEntity> orders = page.getContent();
        recordReads(orders, normalizeCode(reqVO.getSourceSystem()));
        return PageResult.of(orders.stream().map(this::toOrderRespVO).toList(),
                total, pageNo, pageSize);
    }

    /**
     * 分页查询生产完工单读取日志。
     *
     * @param reqVO 分页筛选条件
     * @return 读取日志分页
     */
    @Transactional(readOnly = true)
    public PageResult<CompletionReadLogRespVO> getReadLogPage(
            CompletionReadLogPageReqVO reqVO) {
        validateTimeRange(reqVO.getStartTime(), reqVO.getEndTime());
        Specification<CompletionReadLogEntity> specification =
                CompletionReadLogSpecifications.page(reqVO);
        long total = readLogRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<CompletionReadLogEntity> page =
                readLogRepository.findAll(specification, pageRequest);
        return PageResult.of(page.getContent().stream().map(this::toReadLogRespVO).toList(),
                total, pageNo, pageSize);
    }

    private void recordReads(List<CompletionOrderEntity> orders, String sourceSystem) {
        if (orders.isEmpty()) {
            return;
        }

        Long readerId = SecurityContextHolder.getRequiredLoginUserId();
        List<CompletionReadLogEntity> logs = orders.stream().map(order -> {
            CompletionReadLogEntity log = new CompletionReadLogEntity();
            log.setCompletionOrderId(order.getId());
            log.setCompletionNo(order.getCompletionNo());
            log.setWorkOrderNo(order.getWorkOrderNo());
            log.setSourceSystem(sourceSystem);
            log.setReadBy(readerId);
            return log;
        }).toList();
        readLogRepository.saveAllAndFlush(logs);
    }

    private CompletionOrderRespVO toOrderRespVO(CompletionOrderEntity entity) {
        CompletionOrderRespVO response = new CompletionOrderRespVO();
        response.setId(entity.getId());
        response.setCompletionNo(entity.getCompletionNo());
        response.setProductionTaskId(entity.getProductionTaskId());
        response.setWorkOrderNo(entity.getWorkOrderNo());
        response.setProductCode(entity.getProductCode());
        response.setProductName(entity.getProductName());
        response.setBatchNo(entity.getBatchNo());
        response.setCompletionQuantity(entity.getCompletionQuantity());
        response.setGoodQuantity(entity.getGoodQuantity());
        response.setDefectQuantity(entity.getDefectQuantity());
        response.setAuditStatus(entity.getAuditStatus());
        response.setAuditTime(entity.getAuditTime());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    private CompletionReadLogRespVO toReadLogRespVO(CompletionReadLogEntity entity) {
        CompletionReadLogRespVO response = new CompletionReadLogRespVO();
        response.setId(entity.getId());
        response.setCompletionOrderId(entity.getCompletionOrderId());
        response.setCompletionNo(entity.getCompletionNo());
        response.setWorkOrderNo(entity.getWorkOrderNo());
        response.setSourceSystem(entity.getSourceSystem());
        response.setReadBy(entity.getReadBy());
        response.setReadTime(entity.getReadTime());
        return response;
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.COMPLETION_TIME_RANGE_INVALID);
        }
    }

    private int normalizePageNo(int requestedPageNo, int pageSize, long total) {
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return Math.min(requestedPageNo, totalPages);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
