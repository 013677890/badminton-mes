package com.badminton.mes.module.production.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.convert.WorkOrderConvert;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.redis.WorkOrderCache;
import com.badminton.mes.module.production.dal.redis.WorkOrderNoSequence;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderSpecifications;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.WorkOrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 生产工单 Service 实现。
 *
 * <p>Service 负责编排业务校验、事务、数据库写入和缓存失效时机。
 * Redis Key、TTL、序列化和异常降级均下沉到 {@link WorkOrderCache}。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderServiceImpl.class);

    /** TODO(张竹灏, 2026/07/07): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    private final WorkOrderRepository workOrderRepository;

    private final ProductRepository productRepository;

    private final WorkshopRepository workshopRepository;

    private final WorkOrderCache workOrderCache;

    private final WorkOrderNoSequence workOrderNoSequence;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param workOrderRepository 工单 Repository
     * @param productRepository   产品 Repository
     * @param workshopRepository  车间 Repository
     * @param workOrderCache      工单缓存组件
     * @param workOrderNoSequence 工单号流水生成器
     */
    public WorkOrderServiceImpl(WorkOrderRepository workOrderRepository, ProductRepository productRepository,
                                WorkshopRepository workshopRepository, WorkOrderCache workOrderCache,
                                WorkOrderNoSequence workOrderNoSequence) {
        this.workOrderRepository = workOrderRepository;
        this.productRepository = productRepository;
        this.workshopRepository = workshopRepository;
        this.workOrderCache = workOrderCache;
        this.workOrderNoSequence = workOrderNoSequence;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(WorkOrderSaveReqVO reqVO) {
        validatePlanTime(reqVO);
        ProductEntity product = validateProduct(reqVO.getProductId());
        validateWorkshop(reqVO.getWorkshopId());
        String workOrderNo = resolveWorkOrderNo(reqVO.getWorkOrderNo());

        WorkOrderEntity workOrder = WorkOrderConvert.toEntity(reqVO);
        workOrder.setWorkOrderNo(workOrderNo);
        // 冗余字段以产品档案为准回填，不信任前端提交，避免与档案不一致
        workOrder.setProductName(product.getProductName());
        workOrder.setSpec(product.getSpec());
        workOrder.setUnitId(product.getUnitId());
        workOrder.setSourceType(WorkOrderSourceTypeEnum.MANUAL.getType());
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        workOrder.setCreateBy(DEFAULT_OPERATOR_ID);
        try {
            workOrderRepository.saveAndFlush(workOrder);
        } catch (DataIntegrityViolationException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_work_order_no 兜底，转成业务错误提示
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE);
        }

        logger.info("[创建工单] id: {}, workOrderNo: {}", workOrder.getId(), workOrderNo);
        return workOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(Long id, WorkOrderSaveReqVO reqVO) {
        validatePlanTime(reqVO);
        WorkOrderEntity existing = validateWorkOrderExists(id);
        // 状态机约束：仅"已创建"状态允许修改计划信息
        if (!WorkOrderStatusEnum.CREATED.getStatus().equals(existing.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE);
        }

        ProductEntity product = validateProduct(reqVO.getProductId());
        validateWorkshop(reqVO.getWorkshopId());

        WorkOrderEntity updateEntity = WorkOrderConvert.toEntity(reqVO);
        int rows = workOrderRepository.updatePlan(id, updateEntity.getProductId(), product.getProductName(),
                product.getSpec(), product.getUnitId(), updateEntity.getBatchNo(), updateEntity.getBomId(),
                updateEntity.getRoutingId(), updateEntity.getCustomerId(), updateEntity.getWorkshopId(),
                updateEntity.getPlanQuantity(), updateEntity.getOverRatio(), updateEntity.getPriority(),
                updateEntity.getPlanStartTime(), updateEntity.getPlanEndTime(),
                WorkOrderStatusEnum.CREATED.getStatus());
        // CAS 未命中：校验与更新的间隙内工单被并发下达或删除
        if (rows == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE);
        }

        // 先库后缓存：改库成功后删缓存，删除与回源间的短暂旧值由 TTL 兜底
        workOrderCache.evict(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id) {
        WorkOrderEntity existing = validateWorkOrderExists(id);
        if (!WorkOrderStatusEnum.CREATED.getStatus().equals(existing.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_DELETE);
        }

        // 逻辑删除，JPQL 带状态条件构成 CAS，防并发下达后误删
        int rows = workOrderRepository.logicDeleteById(id, WorkOrderStatusEnum.CREATED.getStatus());
        if (rows == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_DELETE);
        }

        workOrderCache.evict(id);
        logger.info("[删除工单] id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseWorkOrder(Long id) {
        // 先 CAS 更新再对失败查因，避免"先查后改"竞态；条件含 BOM/工艺路线非空校验
        int rows = workOrderRepository.updateToReleased(id, WorkOrderStatusEnum.CREATED.getStatus(),
                WorkOrderStatusEnum.RELEASED.getStatus());
        if (rows == 1) {
            workOrderCache.evict(id);
            logger.info("[下达工单] id: {}", id);
            return;
        }

        // CAS 未命中，逐项查明原因给出精确提示(EXC-003 分门别类提示)
        WorkOrderEntity workOrder = validateWorkOrderExists(id);
        if (!WorkOrderStatusEnum.CREATED.getStatus().equals(workOrder.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RELEASE);
        }
        if (workOrder.getBomId() == null || workOrder.getRoutingId() == null) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_RELEASE_MISSING_BOM_ROUTING);
        }

        // 查因瞬间状态又被并发修改，按状态不允许下达处理
        throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RELEASE);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkOrderRespVO getWorkOrder(Long id) {
        return workOrderCache.get(id).map(WorkOrderConvert::toRespVO).orElseGet(() -> {
            WorkOrderEntity workOrder = validateWorkOrderExists(id);
            workOrderCache.put(workOrder);
            return WorkOrderConvert.toRespVO(workOrder);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkOrderRespVO> getWorkOrderPage(WorkOrderPageReqVO reqVO) {
        Specification<WorkOrderEntity> specification = WorkOrderSpecifications.page(reqVO);
        // 先 count：总数为 0 直接返回空页，省一次列表查询(SQL-005)
        long total = workOrderRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        // 请求页码超过总页数时按最后一页返回(API-009)
        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<WorkOrderEntity> page = workOrderRepository.findAll(specification, pageRequest);
        List<WorkOrderEntity> list = page.getContent();
        return PageResult.of(WorkOrderConvert.toRespVOList(list), total, pageNo, pageSize);
    }

    /**
     * 校验计划时间：完成时间不得早于开始时间。
     *
     * @param reqVO 保存请求
     */
    private void validatePlanTime(WorkOrderSaveReqVO reqVO) {
        if (reqVO.getPlanEndTime().isBefore(reqVO.getPlanStartTime())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_PLAN_TIME_INVALID);
        }
    }

    /**
     * 校验工单存在且未删除。
     *
     * @param id 工单主键
     * @return 工单数据
     */
    private WorkOrderEntity validateWorkOrderExists(Long id) {
        return workOrderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
    }

    /**
     * 校验产品存在且处于启用状态。
     *
     * @param productId 产品 id
     * @return 产品档案，用于冗余字段回填
     */
    private ProductEntity validateProduct(Long productId) {
        ProductEntity product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS);
        }

        return product;
    }

    /**
     * 校验车间存在且处于启用状态。
     *
     * @param workshopId 车间 id
     */
    private void validateWorkshop(Long workshopId) {
        WorkshopEntity workshop = workshopRepository.findByIdAndDeletedFalse(workshopId)
                .orElseThrow(() -> new ServiceException(ProductionErrorCodeConstants.WORKSHOP_NOT_EXISTS));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORKSHOP_NOT_EXISTS);
        }
    }

    /**
     * 确定工单号：外部传入时校验唯一性，未传时由系统生成。
     *
     * @param inputWorkOrderNo 请求中的工单号，可空
     * @return 最终使用的工单号
     */
    private String resolveWorkOrderNo(String inputWorkOrderNo) {
        if (!StringUtils.hasText(inputWorkOrderNo)) {
            return workOrderNoSequence.nextNo();
        }

        // 应用层先查提前给出友好提示；并发窗口由唯一索引兜底(见 insert 的异常转换)
        if (workOrderRepository.existsByWorkOrderNoAndDeletedFalse(inputWorkOrderNo)) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE);
        }

        return inputWorkOrderNo;
    }
}
