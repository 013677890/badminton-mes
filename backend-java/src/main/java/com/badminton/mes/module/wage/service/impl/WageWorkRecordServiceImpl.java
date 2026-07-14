package com.badminton.mes.module.wage.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.wage.constants.WageErrorCodeConstants;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordImportReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordImportRespVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordItemReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordPageReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordRespVO;
import com.badminton.mes.module.wage.convert.WageConvert;
import com.badminton.mes.module.wage.dal.entity.WageWorkRecordEntity;
import com.badminton.mes.module.wage.dal.repository.WageSpecifications;
import com.badminton.mes.module.wage.dal.repository.WageWorkRecordRepository;
import com.badminton.mes.module.wage.service.WageWorkRecordService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 已审核报工计件快照服务实现。 */
@Service
public class WageWorkRecordServiceImpl implements WageWorkRecordService {

    private final WageWorkRecordRepository workRecordRepository;
    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CraftProcessRepository processRepository;
    private final ProductRepository productRepository;

    /** 构造器注入。 */
    public WageWorkRecordServiceImpl(WageWorkRecordRepository workRecordRepository,
                                     UserRepository userRepository,
                                     WorkOrderRepository workOrderRepository,
                                     CraftProcessRepository processRepository,
                                     ProductRepository productRepository) {
        this.workRecordRepository = workRecordRepository;
        this.userRepository = userRepository;
        this.workOrderRepository = workOrderRepository;
        this.processRepository = processRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WageWorkRecordImportRespVO importRecords(WageWorkRecordImportReqVO reqVO) {
        validateUniqueSourceIds(reqVO.getRecords());
        ReferenceContext context = loadAndValidateReferences(reqVO.getRecords());
        reqVO.getRecords().forEach(record -> validateRecord(record, context));
        List<WageWorkRecordItemReqVO> orderedRecords = reqVO.getRecords().stream()
                .sorted(Comparator.comparing(WageWorkRecordItemReqVO::getSourceReportId))
                .toList();
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        int importedCount = 0;
        for (WageWorkRecordItemReqVO record : orderedRecords) {
            int affected = workRecordRepository.insertIdempotently(
                    record.getSourceReportId(), record.getEmployeeId(), record.getWorkDate(),
                    record.getWorkOrderId(), record.getProcessId(), record.getProductId(),
                    record.getQualifiedQuantity(), record.getDefectQuantity(),
                    record.getSourceAuditTime(), operatorId);
            if (affected != 0 && affected != 1) {
                throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR,
                        "报工幂等插入返回了无法识别的影响行数");
            }
            importedCount += affected;
        }
        return new WageWorkRecordImportRespVO(importedCount, reqVO.getRecords().size() - importedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WageWorkRecordRespVO> getRecordPage(WageWorkRecordPageReqVO reqVO) {
        if (reqVO.getWorkDateBegin() != null && reqVO.getWorkDateEnd() != null
                && reqVO.getWorkDateEnd().isBefore(reqVO.getWorkDateBegin())) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "作业结束日期不能早于开始日期");
        }
        var specification = WageSpecifications.workRecordPage(reqVO);
        long total = workRecordRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }
        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<WageWorkRecordEntity> page = workRecordRepository.findAll(specification,
                PageRequest.of(pageNo - 1, reqVO.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "workDate").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<WageWorkRecordRespVO> records = page.getContent().stream()
                .map(WageConvert::toWorkRecordRespVO).toList();
        return PageResult.of(records, total, pageNo, reqVO.getPageSize());
    }

    /** 请求内来源报工主键必须唯一，避免响应计数语义含糊。 */
    private void validateUniqueSourceIds(List<WageWorkRecordItemReqVO> records) {
        Set<Long> sourceIds = new HashSet<>();
        for (WageWorkRecordItemReqVO record : records) {
            if (!sourceIds.add(record.getSourceReportId())) {
                throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "同一请求中来源报工 id 不能重复");
            }
        }
    }

    /** 批量加载引用，避免逐条查询。 */
    private ReferenceContext loadAndValidateReferences(List<WageWorkRecordItemReqVO> records) {
        Set<Long> userIds = records.stream().map(WageWorkRecordItemReqVO::getEmployeeId).collect(Collectors.toSet());
        Set<Long> orderIds = records.stream().map(WageWorkRecordItemReqVO::getWorkOrderId).collect(Collectors.toSet());
        Set<Long> processIds = records.stream().map(WageWorkRecordItemReqVO::getProcessId).collect(Collectors.toSet());
        Set<Long> productIds = records.stream().map(WageWorkRecordItemReqVO::getProductId).collect(Collectors.toSet());

        Map<Long, UserEntity> users = userRepository.findByIdInAndDeletedFalse(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        Map<Long, WorkOrderEntity> orders = workOrderRepository.findByIdInAndDeletedFalse(orderIds).stream()
                .collect(Collectors.toMap(WorkOrderEntity::getId, Function.identity()));
        Map<Long, CraftProcessEntity> processes = processRepository.findByIdInAndStatusAndDeletedFalse(
                        processIds, CommonStatusEnum.ENABLED.getStatus()).stream()
                .collect(Collectors.toMap(CraftProcessEntity::getId, Function.identity()));
        Map<Long, ProductEntity> products = productRepository.findByIdInAndStatusAndDeletedFalse(
                        productIds, CommonStatusEnum.ENABLED.getStatus()).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        if (users.size() != userIds.size() || orders.size() != orderIds.size()
                || processes.size() != processIds.size() || products.size() != productIds.size()) {
            throw new ServiceException(WageErrorCodeConstants.WORK_RECORD_REFERENCE_INVALID);
        }
        if (users.values().stream().anyMatch(user -> !CommonStatusEnum.ENABLED.getStatus().equals(user.getStatus()))
                || processes.values().stream().anyMatch(process -> !Boolean.TRUE.equals(process.getPieceRateEnabled()))) {
            throw new ServiceException(WageErrorCodeConstants.WORK_RECORD_REFERENCE_INVALID);
        }
        return new ReferenceContext(users, orders, processes, products);
    }

    /** 校验单条快照的业务一致性。 */
    private void validateRecord(WageWorkRecordItemReqVO record, ReferenceContext context) {
        BigDecimal total = record.getQualifiedQuantity().add(record.getDefectQuantity());
        if (total.signum() <= 0) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "合格数量与不良数量不能同时为 0");
        }
        WorkOrderEntity order = context.orders().get(record.getWorkOrderId());
        if (!record.getProductId().equals(order.getProductId())) {
            throw new ServiceException(WageErrorCodeConstants.WORK_RECORD_PRODUCT_MISMATCH);
        }
    }

    /** 规范化页码。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int) ((total + pageSize - 1) / pageSize));
    }

    /** 批量引用上下文。 */
    private record ReferenceContext(Map<Long, UserEntity> users,
                                    Map<Long, WorkOrderEntity> orders,
                                    Map<Long, CraftProcessEntity> processes,
                                    Map<Long, ProductEntity> products) {
        private ReferenceContext {
            users = new HashMap<>(users);
            orders = new HashMap<>(orders);
            processes = new HashMap<>(processes);
            products = new HashMap<>(products);
        }
    }
}
