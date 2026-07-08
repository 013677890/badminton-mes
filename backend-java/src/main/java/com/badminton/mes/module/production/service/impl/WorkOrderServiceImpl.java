package com.badminton.mes.module.production.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.constants.ProductionRedisKeyConstants;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.convert.WorkOrderConvert;
import com.badminton.mes.module.production.dal.dataobject.ProductDO;
import com.badminton.mes.module.production.dal.dataobject.WorkOrderDO;
import com.badminton.mes.module.production.dal.dataobject.WorkshopDO;
import com.badminton.mes.module.production.dal.mapper.ProductMapper;
import com.badminton.mes.module.production.dal.mapper.WorkOrderMapper;
import com.badminton.mes.module.production.dal.mapper.WorkshopMapper;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.WorkOrderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import tools.jackson.databind.ObjectMapper;

/**
 * 生产工单 Service 实现。
 *
 * <p>事务说明：本样例各写方法均为单条写语句，依赖数据库自身原子性即可，
 * 未加 @Transactional；若一个方法内出现多条写语句(例如同时写工单与用料明细)，
 * 必须加 {@code @Transactional(rollbackFor = Exception.class)} 保证一致性(EXC-005)。
 *
 * <p>缓存说明：工单详情采用 Cache Aside——读时先查 Redis 未命中回源数据库并回写，
 * 写时先改库再删缓存。缓存是弱依赖，任何缓存异常都降级为直查数据库，
 * 只记日志不阻断业务(设计规约：识别弱依赖并设计降级预案)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderServiceImpl.class);

    /** 工单号前缀 */
    private static final String WORK_ORDER_NO_PREFIX = "WO";

    /** 工单号日期段格式；DateTimeFormatter 线程安全，静态复用避免重复构造 */
    private static final DateTimeFormatter SERIAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** TODO(张竹灏, 2026/07/07): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    private final WorkOrderMapper workOrderMapper;

    private final ProductMapper productMapper;

    private final WorkshopMapper workshopMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param workOrderMapper     工单 Mapper
     * @param productMapper       产品 Mapper
     * @param workshopMapper      车间 Mapper
     * @param stringRedisTemplate Redis 操作模板(String 序列化)
     * @param objectMapper        JSON 序列化器，复用 Spring 容器统一配置
     */
    public WorkOrderServiceImpl(WorkOrderMapper workOrderMapper, ProductMapper productMapper,
                                WorkshopMapper workshopMapper, StringRedisTemplate stringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.workOrderMapper = workOrderMapper;
        this.productMapper = productMapper;
        this.workshopMapper = workshopMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Long createWorkOrder(WorkOrderSaveReqVO reqVO) {
        validatePlanTime(reqVO);
        ProductDO product = validateProduct(reqVO.getProductId());
        validateWorkshop(reqVO.getWorkshopId());
        String workOrderNo = resolveWorkOrderNo(reqVO.getWorkOrderNo());

        WorkOrderDO workOrder = WorkOrderConvert.toDO(reqVO);
        workOrder.setWorkOrderNo(workOrderNo);
        // 冗余字段以产品档案为准回填，不信任前端提交，避免与档案不一致
        workOrder.setProductName(product.getProductName());
        workOrder.setSpec(product.getSpec());
        workOrder.setUnitId(product.getUnitId());
        workOrder.setSourceType(WorkOrderSourceTypeEnum.MANUAL.getType());
        workOrder.setOrderStatus(WorkOrderStatusEnum.CREATED.getStatus());
        workOrder.setCreateBy(DEFAULT_OPERATOR_ID);
        try {
            workOrderMapper.insert(workOrder);
        } catch (DuplicateKeyException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_work_order_no 兜底，转成业务错误提示
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE);
        }

        logger.info("[创建工单] id: {}, workOrderNo: {}", workOrder.getId(), workOrderNo);
        return workOrder.getId();
    }

    @Override
    public void updateWorkOrder(Long id, WorkOrderSaveReqVO reqVO) {
        validatePlanTime(reqVO);
        WorkOrderDO existing = validateWorkOrderExists(id);
        // 状态机约束：仅"已创建"状态允许修改计划信息
        if (!WorkOrderStatusEnum.CREATED.getStatus().equals(existing.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE);
        }

        ProductDO product = validateProduct(reqVO.getProductId());
        validateWorkshop(reqVO.getWorkshopId());

        WorkOrderDO updateDO = WorkOrderConvert.toDO(reqVO);
        updateDO.setId(id);
        updateDO.setProductName(product.getProductName());
        updateDO.setSpec(product.getSpec());
        updateDO.setUnitId(product.getUnitId());
        int rows = workOrderMapper.updateById(updateDO);
        // CAS 未命中：校验与更新的间隙内工单被并发下达或删除
        if (rows == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE);
        }

        // 先库后缓存：改库成功后删缓存，删除与回源间的短暂旧值由 TTL 兜底
        evictCache(id);
    }

    @Override
    public void deleteWorkOrder(Long id) {
        WorkOrderDO existing = validateWorkOrderExists(id);
        if (!WorkOrderStatusEnum.CREATED.getStatus().equals(existing.getOrderStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_DELETE);
        }

        // 逻辑删除，SQL 带状态条件构成 CAS，防并发下达后误删
        int rows = workOrderMapper.deleteById(id);
        if (rows == 0) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_DELETE);
        }

        evictCache(id);
        logger.info("[删除工单] id: {}", id);
    }

    @Override
    public void releaseWorkOrder(Long id) {
        // 先 CAS 更新再对失败查因，避免"先查后改"竞态；条件含 BOM/工艺路线非空校验
        int rows = workOrderMapper.updateToReleased(id);
        if (rows == 1) {
            evictCache(id);
            logger.info("[下达工单] id: {}", id);
            return;
        }

        // CAS 未命中，逐项查明原因给出精确提示(EXC-003 分门别类提示)
        WorkOrderDO workOrder = validateWorkOrderExists(id);
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
    public WorkOrderRespVO getWorkOrder(Long id) {
        WorkOrderDO cached = getCache(id);
        if (cached != null) {
            return WorkOrderConvert.toRespVO(cached);
        }

        WorkOrderDO workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            // 未命中不回写空值；若恶意 id 穿透成为风险，可缓存空对象或前置布隆过滤器
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS);
        }

        putCache(workOrder);
        return WorkOrderConvert.toRespVO(workOrder);
    }

    @Override
    public PageResult<WorkOrderRespVO> getWorkOrderPage(WorkOrderPageReqVO reqVO) {
        // 先 count：总数为 0 直接返回空页，省一次列表查询(SQL-005)
        long total = workOrderMapper.selectPageCount(reqVO);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        // 请求页码超过总页数时按最后一页返回(API-009)
        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);
        int offset = (pageNo - 1) * pageSize;
        List<WorkOrderDO> list = workOrderMapper.selectPageList(reqVO, offset, pageSize);
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
    private WorkOrderDO validateWorkOrderExists(Long id) {
        WorkOrderDO workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS);
        }

        return workOrder;
    }

    /**
     * 校验产品存在且处于启用状态。
     *
     * @param productId 产品 id
     * @return 产品档案，用于冗余字段回填
     */
    private ProductDO validateProduct(Long productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || !CommonStatusEnum.ENABLED.getStatus().equals(product.getStatus())) {
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
        WorkshopDO workshop = workshopMapper.selectById(workshopId);
        if (workshop == null || !CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
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
            return generateWorkOrderNo();
        }

        // 应用层先查提前给出友好提示；并发窗口由唯一索引兜底(见 insert 的异常转换)
        if (workOrderMapper.selectByWorkOrderNo(inputWorkOrderNo) != null) {
            throw new ServiceException(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE);
        }

        return inputWorkOrderNo;
    }

    /**
     * 生成工单号：WO + yyyyMMdd + 4 位流水，如 WO202607080001。
     *
     * <p>流水基于 Redis INCR 原子自增，并发下不会重复；流水超过 9999 时
     * 自然进位为 5 位，单号仍唯一。单号生成强依赖 Redis，Redis 故障时快速失败，
     * 由全局异常处理兜底返回系统错误。
     *
     * @return 新工单号
     */
    private String generateWorkOrderNo() {
        String date = LocalDate.now().format(SERIAL_DATE_FORMATTER);
        String serialKey = ProductionRedisKeyConstants.workOrderSerialKey(date);
        Long serial = stringRedisTemplate.opsForValue().increment(serialKey);
        if (serial == null) {
            // increment 仅在 pipeline/事务中返回 null，此处为防御性兜底(EXC-011 防 NPE)
            throw new ServiceException(GlobalErrorCodeConstants.SYSTEM_ERROR);
        }

        if (serial == 1L) {
            // 当日首号时设置过期时间，跨天后计数 Key 自动清理
            stringRedisTemplate.expire(serialKey, ProductionRedisKeyConstants.WORK_ORDER_SERIAL_TTL);
        }

        return String.format("%s%s%04d", WORK_ORDER_NO_PREFIX, date, serial);
    }

    /**
     * 读工单详情缓存。
     *
     * @param id 工单主键
     * @return 缓存的工单数据；未命中或缓存异常时返回 null，由调用方回源数据库
     */
    private WorkOrderDO getCache(Long id) {
        String key = ProductionRedisKeyConstants.workOrderDetailKey(id);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }

            return objectMapper.readValue(json, WorkOrderDO.class);
        } catch (RuntimeException e) {
            // 缓存是弱依赖：连接失败、反序列化失败一律降级回源，只告警不阻断业务
            logger.warn("[工单缓存读取失败] id: {}, errorMessage: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * 回写工单详情缓存，带 TTL 防止脏数据长期驻留。
     *
     * @param workOrder 工单数据
     */
    private void putCache(WorkOrderDO workOrder) {
        try {
            String json = objectMapper.writeValueAsString(workOrder);
            stringRedisTemplate.opsForValue().set(
                    ProductionRedisKeyConstants.workOrderDetailKey(workOrder.getId()),
                    json, ProductionRedisKeyConstants.WORK_ORDER_DETAIL_TTL);
        } catch (RuntimeException e) {
            logger.warn("[工单缓存写入失败] id: {}, errorMessage: {}", workOrder.getId(), e.getMessage());
        }
    }

    /**
     * 删除工单详情缓存，写操作成功后调用。
     *
     * @param id 工单主键
     */
    private void evictCache(Long id) {
        try {
            stringRedisTemplate.delete(ProductionRedisKeyConstants.workOrderDetailKey(id));
        } catch (RuntimeException e) {
            // 删除失败会短暂读到旧值，由 30 分钟 TTL 兜底过期；记 error 便于跟进补偿
            logger.error("[工单缓存删除失败] id: {}, errorMessage: {}", id, e.getMessage(), e);
        }
    }
}
