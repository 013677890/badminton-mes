package com.badminton.mes.module.equipment.service.impl;

import java.util.Optional;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;
import com.badminton.mes.module.equipment.dal.redis.EquipmentCache;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EquipmentManufacturerServiceImpl} 单元测试。
 *
 * <p>通过 Mockito 隔离制造商、设备台账和详情缓存持久化协作者，直接构造被测 Service。覆盖制造商
 * 编码唯一性、创建默认值、更新字段合并、设备引用删除保护、逻辑删除编码释放，以及空页短路和
 * 越界页码归一化；拒绝分支额外验证不会执行保存。
 */
@ExtendWith(MockitoExtension.class)
class EquipmentManufacturerServiceImplTest {

    /** 测试制造商主键。 */
    private static final Long MANUFACTURER_ID = 300L;

    @Mock
    private EquipmentManufacturerRepository manufacturerRepository;

    @Mock
    private EquipmentLedgerRepository ledgerRepository;

    @Mock
    private EquipmentCache equipmentCache;

    private EquipmentManufacturerServiceImpl manufacturerService;

    /** 每个用例重建 Service，确保 Mock 交互和实体修改互不影响。 */
    @BeforeEach
    void setUp() {
        manufacturerService = new EquipmentManufacturerServiceImpl(
                manufacturerRepository,
                ledgerRepository,
                equipmentCache);
    }

    @Test
    @DisplayName("创建设备制造商：默认启用并返回新主键")
    void createManufacturerAppliesDefaultStatusAndReturnsId() {
        EquipmentManufacturerSaveReqVO request = buildSaveRequest();
        request.setStatus(null);
        when(manufacturerRepository.existsByManufacturerCodeAndDeletedFalse(request.getManufacturerCode()))
                .thenReturn(false);
        when(manufacturerRepository.saveAndFlush(any(EquipmentManufacturerEntity.class)))
                .thenAnswer(invocation -> {
                    EquipmentManufacturerEntity manufacturer = invocation.getArgument(0);
                    manufacturer.setId(MANUFACTURER_ID);
                    return manufacturer;
                });

        Long manufacturerId = manufacturerService.createEquipmentManufacturer(request);

        assertThat(manufacturerId).isEqualTo(MANUFACTURER_ID);
        verify(manufacturerRepository).saveAndFlush(any(EquipmentManufacturerEntity.class));
    }

    @Test
    @DisplayName("创建设备制造商：编码重复时拒绝创建")
    void createManufacturerRejectsDuplicateCode() {
        EquipmentManufacturerSaveReqVO request = buildSaveRequest();
        when(manufacturerRepository.existsByManufacturerCodeAndDeletedFalse(request.getManufacturerCode()))
                .thenReturn(true);

        assertThatThrownBy(() -> manufacturerService.createEquipmentManufacturer(request))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_MANUFACTURER_CODE_DUPLICATE));
        verify(manufacturerRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("修改设备制造商：更新可变字段并保留空状态")
    void updateManufacturerCopiesMutableFieldsAndKeepsStatusWhenRequestStatusIsNull() {
        EquipmentManufacturerEntity existingManufacturer = buildManufacturerEntity();
        when(manufacturerRepository.findByIdAndDeletedFalse(MANUFACTURER_ID))
                .thenReturn(Optional.of(existingManufacturer));
        when(manufacturerRepository.existsByManufacturerCodeAndIdNotAndDeletedFalse(
                "SUPPLIER-UPDATED", MANUFACTURER_ID)).thenReturn(false);
        EquipmentManufacturerSaveReqVO request = buildSaveRequest();
        request.setManufacturerCode("SUPPLIER-UPDATED");
        request.setManufacturerName("更新后的供应商");
        request.setStatus(null);

        manufacturerService.updateEquipmentManufacturer(MANUFACTURER_ID, request);

        assertThat(existingManufacturer.getManufacturerCode()).isEqualTo("SUPPLIER-UPDATED");
        assertThat(existingManufacturer.getManufacturerName()).isEqualTo("更新后的供应商");
        assertThat(existingManufacturer.getStatus()).isEqualTo(1);
        verify(manufacturerRepository).save(existingManufacturer);
    }

    @Test
    @DisplayName("删除设备制造商：存在设备引用时拒绝删除")
    void deleteManufacturerRejectsLedgerReference() {
        when(manufacturerRepository.findByIdAndDeletedFalse(MANUFACTURER_ID))
                .thenReturn(Optional.of(buildManufacturerEntity()));
        when(ledgerRepository.countByManufacturerIdAndDeletedFalse(MANUFACTURER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> manufacturerService.deleteEquipmentManufacturer(MANUFACTURER_ID))
                .isInstanceOfSatisfying(ServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isSameAs(
                                EquipmentErrorCodeConstants.EQUIPMENT_MANUFACTURER_HAS_EQUIPMENT));
        verify(manufacturerRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除设备制造商：无引用时重命名编码并逻辑删除")
    void deleteManufacturerRenamesCodeAndMarksDeleted() {
        EquipmentManufacturerEntity existingManufacturer = buildManufacturerEntity();
        when(manufacturerRepository.findByIdAndDeletedFalse(MANUFACTURER_ID))
                .thenReturn(Optional.of(existingManufacturer));
        when(ledgerRepository.countByManufacturerIdAndDeletedFalse(MANUFACTURER_ID)).thenReturn(0L);

        manufacturerService.deleteEquipmentManufacturer(MANUFACTURER_ID);

        assertThat(existingManufacturer.getManufacturerCode()).startsWith("SUPPLIER_DELETED_");
        assertThat(existingManufacturer.getDeleted()).isTrue();
        verify(manufacturerRepository).save(existingManufacturer);
    }

    @Test
    @DisplayName("分页查询设备制造商：无数据时返回空分页")
    void getManufacturerPageReturnsEmptyPageWhenNoData() {
        EquipmentManufacturerPageReqVO request = new EquipmentManufacturerPageReqVO();
        request.setPageNo(1);
        request.setPageSize(20);
        when(manufacturerRepository.count(any(Specification.class))).thenReturn(0L);

        PageResult<?> pageResult = manufacturerService.getEquipmentManufacturerPage(request);

        assertThat(pageResult.getTotal()).isZero();
        assertThat(pageResult.getList()).isEmpty();
        verify(manufacturerRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("分页查询设备制造商：请求页码超过总页数时修正为最后一页")
    void getManufacturerPageNormalizesPageNoWhenRequestedPageExceedsTotalPages() {
        EquipmentManufacturerPageReqVO request = new EquipmentManufacturerPageReqVO();
        request.setPageNo(9);
        request.setPageSize(2);
        when(manufacturerRepository.count(any(Specification.class))).thenReturn(3L);
        when(manufacturerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildManufacturerEntity())));

        PageResult<?> pageResult = manufacturerService.getEquipmentManufacturerPage(request);

        assertThat(pageResult.getPageNo()).isEqualTo(2);
        assertThat(pageResult.getPageSize()).isEqualTo(2);
        assertThat(pageResult.getTotal()).isEqualTo(3L);
        verify(manufacturerRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    /** 构造字段完整、默认启用的制造商保存请求。 */
    private EquipmentManufacturerSaveReqVO buildSaveRequest() {
        EquipmentManufacturerSaveReqVO request = new EquipmentManufacturerSaveReqVO();
        request.setManufacturerCode("SUPPLIER");
        request.setManufacturerName("设备供应商");
        request.setContactPerson("张三");
        request.setContactPhone("13800138000");
        request.setContactEmail("supplier@example.com");
        request.setAddress("测试地址");
        request.setWebsite("https://supplier.example.com");
        request.setRemark("测试制造商");
        request.setStatus(1);
        return request;
    }

    /** 构造当前有效的制造商实体，供更新和删除场景复用。 */
    private EquipmentManufacturerEntity buildManufacturerEntity() {
        EquipmentManufacturerEntity manufacturer = new EquipmentManufacturerEntity();
        manufacturer.setId(MANUFACTURER_ID);
        manufacturer.setManufacturerCode("SUPPLIER");
        manufacturer.setManufacturerName("设备供应商");
        manufacturer.setStatus(1);
        manufacturer.setDeleted(false);
        return manufacturer;
    }
}
