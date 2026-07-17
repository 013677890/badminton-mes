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
 * <p>面向外部系统只暴露已审核完工单，并把“本次分页实际返回了哪些单据”逐条写入读取日志。
 * 完工单读取因此使用写事务：数据查询与本页读取痕迹一并完成；读取日志查询则保持纯只读事务。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class CompletionOrderReadService {

    /** 完工单仓储，用于执行已审核条件的统计和分页查询。 */
    private final CompletionOrderRepository completionOrderRepository;

    /** 读取日志仓储，用于批量记录外部读取痕迹并支持审计分页。 */
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
        // 起止时间是组合条件，先校验可避免构造永远无结果或语义颠倒的查询。
        validateTimeRange(reqVO.getStartTime(), reqVO.getEndTime());
        Specification<CompletionOrderEntity> specification =
                CompletionOrderSpecifications.approvedPage(reqVO);
        // 先 count，空结果直接返回且不会生成任何读取日志。
        long total = completionOrderRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        // 主键升序提供稳定游览顺序，减少外部系统逐页拉取时的顺序漂移。
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        Page<CompletionOrderEntity> page =
                completionOrderRepository.findAll(specification, pageRequest);
        List<CompletionOrderEntity> orders = page.getContent();
        // 只记录当前页实际返回的单据，日志与本次读取事务共同提交。
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
        // 审计查询同样检查时间区间，但不产生新的读取日志。
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

    /** 为本页每条完工单批量构造读取日志，避免逐条执行数据库插入。 */
    private void recordReads(List<CompletionOrderEntity> orders, String sourceSystem) {
        if (orders.isEmpty()) {
            return;
        }

        // 读取人来自认证上下文，来源系统来自请求并已规范化，两者共同描述消费方身份。
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
        // 批量保存后立即刷新，使日志约束或数据库异常在返回分页数据前暴露。
        readLogRepository.saveAllAndFlush(logs);
    }

    /** 将完工单实体显式投影为对外响应，不暴露内部审计操作者等控制字段。 */
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

    /** 将读取日志实体转换为审计查询响应。 */
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

    /** 校验查询结束时间不得早于开始时间。 */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.COMPLETION_TIME_RANGE_INVALID);
        }
    }

    /** 将越界页码收敛到最后一页，保持有总数时始终返回有效页。 */
    private int normalizePageNo(int requestedPageNo, int pageSize, long total) {
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return Math.min(requestedPageNo, totalPages);
    }

    /** 规范化来源系统编码，避免大小写差异拆分读取审计。 */
    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
