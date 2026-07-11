package com.badminton.mes.module.wage.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.badminton.mes.common.security.LoginUser;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.entity.WorkOrderEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.system.dal.entity.UserEntity;
import com.badminton.mes.module.system.dal.repository.UserRepository;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordImportReqVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordImportRespVO;
import com.badminton.mes.module.wage.controller.vo.WageWorkRecordItemReqVO;
import com.badminton.mes.module.wage.dal.repository.WageWorkRecordRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link WageWorkRecordServiceImpl} 幂等导入测试。 */
@ExtendWith(MockitoExtension.class)
class WageWorkRecordServiceImplTest {

    private static final Long EMPLOYEE_ID = 10L;
    private static final Long WORK_ORDER_ID = 20L;
    private static final Long PROCESS_ID = 30L;
    private static final Long PRODUCT_ID = 40L;

    @Mock
    private WageWorkRecordRepository workRecordRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private CraftProcessRepository processRepository;
    @Mock
    private ProductRepository productRepository;

    private WageWorkRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WageWorkRecordServiceImpl(workRecordRepository, userRepository,
                workOrderRepository, processRepository, productRepository);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(99L);
        SecurityContextHolder.set("wage-test-token", loginUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("导入报工：按来源主键升序写入并准确统计重复记录")
    void importRecordsSortsIdsAndCountsDuplicates() {
        prepareReferences();
        WageWorkRecordImportReqVO request = new WageWorkRecordImportReqVO();
        request.setRecords(List.of(buildRecord(2L), buildRecord(1L)));
        when(workRecordRepository.insertIdempotently(anyLong(), anyLong(), any(LocalDate.class),
                anyLong(), anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class),
                any(LocalDateTime.class), anyLong())).thenReturn(1, 0);

        WageWorkRecordImportRespVO result = service.importRecords(request);

        ArgumentCaptor<Long> sourceIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(workRecordRepository, org.mockito.Mockito.times(2)).insertIdempotently(
                sourceIdCaptor.capture(), anyLong(), any(LocalDate.class), anyLong(), anyLong(),
                anyLong(), any(BigDecimal.class), any(BigDecimal.class), any(LocalDateTime.class), anyLong());
        assertThat(sourceIdCaptor.getAllValues()).containsExactly(1L, 2L);
        assertThat(result.getImportedCount()).isEqualTo(1);
        assertThat(result.getDuplicateCount()).isEqualTo(1);
    }

    /** 准备批量引用查询结果。 */
    private void prepareReferences() {
        UserEntity user = new UserEntity();
        user.setId(EMPLOYEE_ID);
        user.setStatus(1);
        WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setProductId(PRODUCT_ID);
        CraftProcessEntity process = new CraftProcessEntity();
        process.setId(PROCESS_ID);
        process.setStatus(1);
        process.setPieceRateEnabled(true);
        ProductEntity product = new ProductEntity();
        product.setId(PRODUCT_ID);
        product.setStatus(1);
        when(userRepository.findByIdInAndDeletedFalse(eq(java.util.Set.of(EMPLOYEE_ID))))
                .thenReturn(List.of(user));
        when(workOrderRepository.findByIdInAndDeletedFalse(eq(java.util.Set.of(WORK_ORDER_ID))))
                .thenReturn(List.of(workOrder));
        when(processRepository.findByIdInAndStatusAndDeletedFalse(
                eq(java.util.Set.of(PROCESS_ID)), anyInt())).thenReturn(List.of(process));
        when(productRepository.findByIdInAndStatusAndDeletedFalse(
                eq(java.util.Set.of(PRODUCT_ID)), anyInt())).thenReturn(List.of(product));
    }

    /** 构造合法报工导入项。 */
    private WageWorkRecordItemReqVO buildRecord(Long sourceReportId) {
        WageWorkRecordItemReqVO record = new WageWorkRecordItemReqVO();
        record.setSourceReportId(sourceReportId);
        record.setEmployeeId(EMPLOYEE_ID);
        record.setWorkDate(LocalDate.of(2026, 7, 10));
        record.setWorkOrderId(WORK_ORDER_ID);
        record.setProcessId(PROCESS_ID);
        record.setProductId(PRODUCT_ID);
        record.setQualifiedQuantity(BigDecimal.ONE);
        record.setDefectQuantity(BigDecimal.ZERO);
        record.setSourceAuditTime(LocalDateTime.of(2026, 7, 10, 12, 0));
        record.setApproved(true);
        return record;
    }
}
