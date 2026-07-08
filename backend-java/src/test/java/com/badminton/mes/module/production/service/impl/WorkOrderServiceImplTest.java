package com.badminton.mes.module.production.service.impl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.WorkOrderPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderRespVO;
import com.badminton.mes.module.production.controller.vo.WorkOrderSaveReqVO;
import com.badminton.mes.module.production.dal.dataobject.ProductDO;
import com.badminton.mes.module.production.dal.dataobject.WorkOrderDO;
import com.badminton.mes.module.production.dal.dataobject.WorkshopDO;
import com.badminton.mes.module.production.dal.mapper.ProductMapper;
import com.badminton.mes.module.production.dal.mapper.WorkOrderMapper;
import com.badminton.mes.module.production.dal.mapper.WorkshopMapper;
import com.badminton.mes.module.production.enums.WorkOrderSourceTypeEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WorkOrderServiceImpl} 单元测试。
 *
 * <p>数据库与 Redis 依赖全部 Mock，不依赖外部环境，可重复执行(TEST-004)；
 * 用例覆盖正确输入、边界与异常流程(TEST-009 BCDE 原则)，断言全自动(TEST-002)。
 *
 * @author 张竹灏
 * @date 2026/07/08
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    /** 测试用产品 id */
    private static final Long PRODUCT_ID = 10L;

    /** 测试用车间 id */
    private static final Long WORKSHOP_ID = 20L;

    /** 测试用工单 id */
    private static final Long WORK_ORDER_ID = 100L;

    @Mock
    private WorkOrderMapper workOrderMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WorkshopMapper workshopMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    /** JSON 序列化使用真实实例，同时验证 DO 可正常序列化/反序列化 */
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private WorkOrderServiceImpl workOrderService;

    @BeforeEach
    void setUp() {
        workOrderService = new WorkOrderServiceImpl(workOrderMapper, productMapper,
                workshopMapper, stringRedisTemplate, jsonMapper);
    }

    @Test
    @DisplayName("创建工单：自动生成单号，冗余字段按产品档案回填")
    void createWorkOrderGeneratesNoAndFillsRedundancy() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(buildEnabledProduct());
        when(workshopMapper.selectById(WORKSHOP_ID)).thenReturn(buildEnabledWorkshop());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        // 模拟数据库自增主键回填
        doAnswer(invocation -> {
            WorkOrderDO entity = invocation.getArgument(0);
            entity.setId(WORK_ORDER_ID);
            return 1;
        }).when(workOrderMapper).insert(any(WorkOrderDO.class));

        Long id = workOrderService.createWorkOrder(reqVO);

        assertThat(id).isEqualTo(WORK_ORDER_ID);
        ArgumentCaptor<WorkOrderDO> captor = ArgumentCaptor.forClass(WorkOrderDO.class);
        verify(workOrderMapper).insert(captor.capture());
        WorkOrderDO inserted = captor.getValue();
        // 单号格式：WO + 8 位日期 + 4 位流水
        assertThat(inserted.getWorkOrderNo()).matches("WO\\d{8}0001");
        assertThat(inserted.getSourceType()).isEqualTo(WorkOrderSourceTypeEnum.MANUAL.getType());
        assertThat(inserted.getOrderStatus()).isEqualTo(WorkOrderStatusEnum.CREATED.getStatus());
        // 冗余字段以档案为准，而非请求提交值
        assertThat(inserted.getProductName()).isEqualTo("比赛级羽毛球");
        assertThat(inserted.getUnitId()).isEqualTo(1L);
        // 当日首号需要设置流水 Key 过期时间
        verify(stringRedisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("创建工单：计划完成时间早于开始时间，抛计划时间异常")
    void createWorkOrderRejectsInvalidPlanTime() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setPlanEndTime(reqVO.getPlanStartTime().minusDays(1));

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_PLAN_TIME_INVALID));
        verify(workOrderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建工单：产品已停用，抛产品不可用异常")
    void createWorkOrderRejectsDisabledProduct() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        ProductDO disabled = buildEnabledProduct();
        disabled.setStatus(0);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(disabled);

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.PRODUCT_NOT_EXISTS));
    }

    @Test
    @DisplayName("创建工单：指定单号已存在，抛单号重复异常且不落库")
    void createWorkOrderRejectsDuplicateNo() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setWorkOrderNo("WO202607080001");
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(buildEnabledProduct());
        when(workshopMapper.selectById(WORKSHOP_ID)).thenReturn(buildEnabledWorkshop());
        when(workOrderMapper.selectByWorkOrderNo("WO202607080001")).thenReturn(new WorkOrderDO());

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE));
        verify(workOrderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建工单：并发穿透查重触发唯一索引，转为单号重复业务异常")
    void createWorkOrderTranslatesDuplicateKeyException() {
        WorkOrderSaveReqVO reqVO = buildSaveReqVO();
        reqVO.setWorkOrderNo("WO202607080002");
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(buildEnabledProduct());
        when(workshopMapper.selectById(WORKSHOP_ID)).thenReturn(buildEnabledWorkshop());
        when(workOrderMapper.selectByWorkOrderNo("WO202607080002")).thenReturn(null);
        when(workOrderMapper.insert(any(WorkOrderDO.class)))
                .thenThrow(new DuplicateKeyException("uk_work_order_no"));

        assertThatThrownBy(() -> workOrderService.createWorkOrder(reqVO))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NO_DUPLICATE));
    }

    @Test
    @DisplayName("修改工单：已下达状态不允许修改")
    void updateWorkOrderRejectsReleasedStatus() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus()));

        assertThatThrownBy(() -> workOrderService.updateWorkOrder(WORK_ORDER_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE));
        verify(workOrderMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("修改工单：CAS 未命中(并发下达)，抛状态不允许修改异常")
    void updateWorkOrderFailsWhenCasMiss() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus()));
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(buildEnabledProduct());
        when(workshopMapper.selectById(WORKSHOP_ID)).thenReturn(buildEnabledWorkshop());
        when(workOrderMapper.updateById(any(WorkOrderDO.class))).thenReturn(0);

        assertThatThrownBy(() -> workOrderService.updateWorkOrder(WORK_ORDER_ID, buildSaveReqVO()))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_UPDATE));
    }

    @Test
    @DisplayName("修改工单：成功后删除详情缓存")
    void updateWorkOrderEvictsCacheOnSuccess() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus()));
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(buildEnabledProduct());
        when(workshopMapper.selectById(WORKSHOP_ID)).thenReturn(buildEnabledWorkshop());
        when(workOrderMapper.updateById(any(WorkOrderDO.class))).thenReturn(1);

        workOrderService.updateWorkOrder(WORK_ORDER_ID, buildSaveReqVO());

        verify(stringRedisTemplate).delete("mes:production:work_order:" + WORK_ORDER_ID);
    }

    @Test
    @DisplayName("删除工单：成功逻辑删除并清理缓存")
    void deleteWorkOrderEvictsCacheOnSuccess() {
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus()));
        when(workOrderMapper.deleteById(WORK_ORDER_ID)).thenReturn(1);

        workOrderService.deleteWorkOrder(WORK_ORDER_ID);

        verify(stringRedisTemplate).delete("mes:production:work_order:" + WORK_ORDER_ID);
    }

    @Test
    @DisplayName("删除工单：工单不存在抛业务异常")
    void deleteWorkOrderRejectsMissingOrder() {
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> workOrderService.deleteWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
    }

    @Test
    @DisplayName("下达工单：CAS 命中直接成功并清理缓存，不再回查")
    void releaseWorkOrderSucceedsByCas() {
        when(workOrderMapper.updateToReleased(WORK_ORDER_ID)).thenReturn(1);

        workOrderService.releaseWorkOrder(WORK_ORDER_ID);

        verify(stringRedisTemplate).delete("mes:production:work_order:" + WORK_ORDER_ID);
        verify(workOrderMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("下达工单：未维护 BOM，查因后抛缺 BOM/工艺路线异常")
    void releaseWorkOrderRejectsMissingBom() {
        when(workOrderMapper.updateToReleased(WORK_ORDER_ID)).thenReturn(0);
        WorkOrderDO created = buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus());
        created.setBomId(null);
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(created);

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_RELEASE_MISSING_BOM_ROUTING));
    }

    @Test
    @DisplayName("下达工单：状态已是已下达，查因后抛状态不允许下达异常")
    void releaseWorkOrderRejectsWrongStatus() {
        when(workOrderMapper.updateToReleased(WORK_ORDER_ID)).thenReturn(0);
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(buildWorkOrder(WorkOrderStatusEnum.RELEASED.getStatus()));

        assertThatThrownBy(() -> workOrderService.releaseWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_STATUS_NOT_ALLOW_RELEASE));
    }

    @Test
    @DisplayName("查询详情：缓存命中直接返回，不查数据库")
    void getWorkOrderHitsCache() {
        WorkOrderDO cached = buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mes:production:work_order:" + WORK_ORDER_ID))
                .thenReturn(jsonMapper.writeValueAsString(cached));

        WorkOrderRespVO respVO = workOrderService.getWorkOrder(WORK_ORDER_ID);

        assertThat(respVO.getId()).isEqualTo(WORK_ORDER_ID);
        assertThat(respVO.getWorkOrderNo()).isEqualTo("WO202607080001");
        verify(workOrderMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("查询详情：缓存未命中回源数据库并回写缓存")
    void getWorkOrderFallsBackToDatabaseAndWritesCache() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus()));

        WorkOrderRespVO respVO = workOrderService.getWorkOrder(WORK_ORDER_ID);

        assertThat(respVO.getId()).isEqualTo(WORK_ORDER_ID);
        verify(valueOperations).set(eq("mes:production:work_order:" + WORK_ORDER_ID),
                anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("查询详情：工单不存在抛业务异常且不回写缓存")
    void getWorkOrderRejectsMissingOrder() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> workOrderService.getWorkOrder(WORK_ORDER_ID))
                .isInstanceOfSatisfying(ServiceException.class, e -> assertThat(e.getErrorCode())
                        .isSameAs(ProductionErrorCodeConstants.WORK_ORDER_NOT_EXISTS));
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("查询详情：Redis 故障时降级直查数据库，业务不受影响")
    void getWorkOrderDegradesWhenRedisDown() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));
        doThrow(new RedisConnectionFailureException("connection refused"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus()));

        WorkOrderRespVO respVO = workOrderService.getWorkOrder(WORK_ORDER_ID);

        assertThat(respVO.getId()).isEqualTo(WORK_ORDER_ID);
    }

    @Test
    @DisplayName("分页查询：总数为 0 直接返回空页，不再查列表")
    void getWorkOrderPageReturnsEmptyWhenNoData() {
        WorkOrderPageReqVO reqVO = new WorkOrderPageReqVO();
        when(workOrderMapper.selectPageCount(reqVO)).thenReturn(0L);

        PageResult<WorkOrderRespVO> pageResult = workOrderService.getWorkOrderPage(reqVO);

        assertThat(pageResult.getTotal()).isZero();
        assertThat(pageResult.getList()).isEmpty();
        verify(workOrderMapper, never()).selectPageList(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("分页查询：页码超过总页数时按最后一页查询(API-009)")
    void getWorkOrderPageCorrectsOverflowPageNo() {
        WorkOrderPageReqVO reqVO = new WorkOrderPageReqVO();
        reqVO.setPageNo(99);
        reqVO.setPageSize(10);
        when(workOrderMapper.selectPageCount(reqVO)).thenReturn(25L);
        when(workOrderMapper.selectPageList(reqVO, 20, 10))
                .thenReturn(List.of(buildWorkOrder(WorkOrderStatusEnum.CREATED.getStatus())));

        PageResult<WorkOrderRespVO> pageResult = workOrderService.getWorkOrderPage(reqVO);

        // 25 条 / 每页 10 条 = 3 页，第 99 页被修正为第 3 页(offset 20)
        assertThat(pageResult.getPageNo()).isEqualTo(3);
        assertThat(pageResult.getTotal()).isEqualTo(25L);
        assertThat(pageResult.getList()).hasSize(1);
        verify(workOrderMapper).selectPageList(reqVO, 20, 10);
    }

    /**
     * 构造合法的保存请求。
     */
    private WorkOrderSaveReqVO buildSaveReqVO() {
        WorkOrderSaveReqVO reqVO = new WorkOrderSaveReqVO();
        reqVO.setProductId(PRODUCT_ID);
        reqVO.setWorkshopId(WORKSHOP_ID);
        reqVO.setPlanQuantity(1000);
        reqVO.setOverRatio(new BigDecimal("5.00"));
        reqVO.setPriority(5);
        reqVO.setPlanStartTime(LocalDateTime.of(2026, 7, 10, 8, 0, 0));
        reqVO.setPlanEndTime(LocalDateTime.of(2026, 7, 15, 18, 0, 0));
        return reqVO;
    }

    /**
     * 构造启用状态的产品档案。
     */
    private ProductDO buildEnabledProduct() {
        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setProductCode("P001");
        product.setProductName("比赛级羽毛球");
        product.setSpec("77速 鹅毛");
        product.setUnitId(1L);
        product.setStatus(1);
        return product;
    }

    /**
     * 构造启用状态的车间档案。
     */
    private WorkshopDO buildEnabledWorkshop() {
        WorkshopDO workshop = new WorkshopDO();
        workshop.setId(WORKSHOP_ID);
        workshop.setWorkshopCode("WS001");
        workshop.setWorkshopName("一号成型车间");
        workshop.setStatus(1);
        return workshop;
    }

    /**
     * 构造指定状态的工单。
     *
     * @param orderStatus 工单状态值
     */
    private WorkOrderDO buildWorkOrder(Integer orderStatus) {
        WorkOrderDO workOrder = new WorkOrderDO();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setWorkOrderNo("WO202607080001");
        workOrder.setSourceType(WorkOrderSourceTypeEnum.MANUAL.getType());
        workOrder.setProductId(PRODUCT_ID);
        workOrder.setProductName("比赛级羽毛球");
        workOrder.setUnitId(1L);
        workOrder.setBomId(30L);
        workOrder.setRoutingId(40L);
        workOrder.setWorkshopId(WORKSHOP_ID);
        workOrder.setPlanQuantity(1000);
        workOrder.setPlanStartTime(LocalDateTime.of(2026, 7, 10, 8, 0, 0));
        workOrder.setPlanEndTime(LocalDateTime.of(2026, 7, 15, 18, 0, 0));
        workOrder.setOrderStatus(orderStatus);
        return workOrder;
    }
}
