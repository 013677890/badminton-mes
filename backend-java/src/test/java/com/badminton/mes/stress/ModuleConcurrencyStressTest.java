package com.badminton.mes.stress;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.badminton.mes.common.core.ErrorCode;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.convert.AndonTypeConvert;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeSaveReqVO;
import com.badminton.mes.module.barcode.convert.BarcodeTypeConvert;
import com.badminton.mes.module.craft.service.support.CraftVersionValidator;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.convert.DeviceCountRecordConvert;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentManufacturerConvert;
import com.badminton.mes.module.integration.enums.IntegrationWriteStatusEnum;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionPlanSaveReqVO;
import com.badminton.mes.module.quality.convert.QualityInspectionPlanConvert;
import com.badminton.mes.module.scene.enums.SceneTaskStatusEnum;
import com.badminton.mes.module.system.dal.redis.SystemRedisKeyConstants;
import com.badminton.mes.module.wage.service.support.WageAmountUtils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 全业务模块的进程内并发压力基线。
 *
 * <p>默认每模块执行 10,000 次操作；通过 {@code -Dmes.stress.operations=N} 调整。
 * 该套件验证无共享状态污染、无并发异常，不替代带 MySQL/Redis 的端到端容量测试。
 */
@Tag("stress")
@Timeout(90)
class ModuleConcurrencyStressTest {

    private static final int OPERATIONS = Integer.getInteger("mes.stress.operations", 10_000);
    private static final ErrorCode VERSION_CONFLICT = new ErrorCode("A0500", "version", "retry");

    @Test @DisplayName("andon 模块并发转换压力")
    void andonModule() throws Exception {
        ConcurrentStressRunner.run("andon", OPERATIONS, index -> {
            AndonTypeSaveReqVO request = new AndonTypeSaveReqVO();
            request.setTypeCode("ANDON-" + index);
            request.setTypeName("异常" + index);
            request.setExceptionCategory("EQUIPMENT");
            request.setHandlingMode("ASSISTANCE");
            if (!request.getTypeCode().equals(AndonTypeConvert.toEntity(request).getTypeCode())) {
                throw new AssertionError("andon conversion mismatch");
            }
        });
    }

    @Test @DisplayName("barcode 模块并发转换压力")
    void barcodeModule() throws Exception {
        ConcurrentStressRunner.run("barcode", OPERATIONS, index -> {
            BarcodeTypeSaveReqVO request = new BarcodeTypeSaveReqVO();
            request.setTypeCode("BAR-" + index);
            request.setTypeName("条码" + index);
            if (!request.getTypeCode().equals(BarcodeTypeConvert.toEntity(request).getTypeCode())) {
                throw new AssertionError("barcode conversion mismatch");
            }
        });
    }

    @Test @DisplayName("craft 模块乐观版本校验压力")
    void craftModule() throws Exception {
        ConcurrentStressRunner.run("craft", OPERATIONS,
                index -> CraftVersionValidator.validate(index, index, VERSION_CONFLICT));
    }

    @Test @DisplayName("device 模块计数快照转换压力")
    void deviceModule() throws Exception {
        DeviceAccessConfigEntity config = new DeviceAccessConfigEntity();
        config.setId(1L);
        config.setEquipmentId(2L);
        config.setCollectionPointCode("POINT-A");
        config.setProcessId(3L);
        ConcurrentStressRunner.run("device", OPERATIONS, index -> {
            DeviceCountReportReqVO request = new DeviceCountReportReqVO();
            request.setEquipmentCode("EQ-" + index);
            request.setCollectedAt(LocalDateTime.of(2026, 7, 14, 8, 0));
            request.setSerialNumber("SN-" + index);
            request.setCountValue((long) index);
            var entity = DeviceCountRecordConvert.toEntity(request, config, "K-" + index);
            if (entity.getRawCount() != index || !entity.getDeduplicationKey().equals("K-" + index)) {
                throw new AssertionError("device snapshot mismatch");
            }
        });
    }

    @Test @DisplayName("equipment 模块制造商转换压力")
    void equipmentModule() throws Exception {
        ConcurrentStressRunner.run("equipment", OPERATIONS, index -> {
            EquipmentManufacturerSaveReqVO request = new EquipmentManufacturerSaveReqVO();
            request.setManufacturerCode("MFR-" + index);
            request.setManufacturerName("制造商" + index);
            if (!request.getManufacturerCode().equals(
                    EquipmentManufacturerConvert.toEntity(request).getManufacturerCode())) {
                throw new AssertionError("equipment conversion mismatch");
            }
        });
    }

    @Test @DisplayName("integration 模块状态读取压力")
    void integrationModule() throws Exception {
        IntegrationWriteStatusEnum[] statuses = IntegrationWriteStatusEnum.values();
        ConcurrentStressRunner.run("integration", OPERATIONS, index -> {
            IntegrationWriteStatusEnum status = statuses[index % statuses.length];
            if (status.getStatus() == null || status.getCode() == null) {
                throw new AssertionError("integration status incomplete");
            }
        });
    }

    @Test @DisplayName("production 模块状态集合读取压力")
    void productionModule() throws Exception {
        ConcurrentStressRunner.run("production", OPERATIONS, index -> {
            if (!WorkOrderStatusEnum.activeStatuses().contains(index % 5)) {
                throw new AssertionError("production active status missing");
            }
        });
    }

    @Test @DisplayName("quality 模块检验方案转换压力")
    void qualityModule() throws Exception {
        ConcurrentStressRunner.run("quality", OPERATIONS, index -> {
            QualityInspectionPlanSaveReqVO request = new QualityInspectionPlanSaveReqVO();
            request.setPlanCode("QP-" + index);
            request.setPlanName("方案" + index);
            request.setInspectionType("PATROL");
            request.setDefaultFlag((index & 1) == 0);
            if (!request.getPlanCode().equals(QualityInspectionPlanConvert.toEntity(request).getPlanCode())) {
                throw new AssertionError("quality conversion mismatch");
            }
        });
    }

    @Test @DisplayName("scene 模块状态枚举并发读取压力")
    void sceneModule() throws Exception {
        SceneTaskStatusEnum[] statuses = SceneTaskStatusEnum.values();
        ConcurrentStressRunner.run("scene", OPERATIONS, index -> {
            if (statuses[index % statuses.length].getStatus() == null) {
                throw new AssertionError("scene status missing");
            }
        });
    }

    @Test @DisplayName("system 模块 Redis Key 生成压力")
    void systemModule() throws Exception {
        ConcurrentStressRunner.run("system", OPERATIONS, index -> {
            String key = SystemRedisKeyConstants.loginTokenKey("token-" + index);
            if (!key.equals("mes:system:login:token:token-" + index)) {
                throw new AssertionError("system key mismatch");
            }
        });
    }

    @Test @DisplayName("wage 模块金额计算压力")
    void wageModule() throws Exception {
        ConcurrentStressRunner.run("wage", OPERATIONS, index -> {
            long amount = WageAmountUtils.calculateAmount(
                    BigDecimal.TEN, BigDecimal.ONE, 25_000L, 1_000);
            if (amount != 247_500L) {
                throw new AssertionError("wage amount mismatch");
            }
        });
    }
}
