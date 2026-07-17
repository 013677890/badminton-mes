package com.badminton.mes.module.integration.service;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.integration.constants.IntegrationErrorCodeConstants;
import com.badminton.mes.module.integration.controller.vo.EquipmentBindingSaveReqVO;
import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;
import com.badminton.mes.module.integration.dal.repository.EquipmentBindingRepository;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.system.dal.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备报工绑定配置服务。
 *
 * <p>将外部设备编码绑定到 MES 产线、可选工序和默认报工人，并维护累计计数单次增量阈值。
 * 保存采用设备编码维度的新增或更新语义；跨表主键在写入前必须指向未删除且启用的主数据，
 * 自动报工开启时还强制要求默认员工，避免有效计数无法形成完整报工责任信息。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class EquipmentBindingService {

    /** 设备绑定仓储，按设备编码查询现有配置并保存更新。 */
    private final EquipmentBindingRepository bindingRepository;

    /** 产线仓储，用于确认绑定目标当前存在且启用。 */
    private final ProductionLineRepository lineRepository;

    /** 工序仓储，用于校验可选的设备固定工序范围。 */
    private final CraftProcessRepository processRepository;

    /** 用户仓储，用于校验自动报工默认员工的可用状态。 */
    private final UserRepository userRepository;

    /** 构造设备绑定服务并固定全部主档校验依赖。 */
    public EquipmentBindingService(EquipmentBindingRepository bindingRepository,
                                   ProductionLineRepository lineRepository,
                                   CraftProcessRepository processRepository,
                                   UserRepository userRepository) {
        this.bindingRepository = bindingRepository;
        this.lineRepository = lineRepository;
        this.processRepository = processRepository;
        this.userRepository = userRepository;
    }

    /** 保存或更新设备绑定配置。 */
    @Transactional(rollbackFor = Exception.class)
    public Long saveBinding(EquipmentBindingSaveReqVO reqVO) {
        // 产线是设备绑定的必选归属，必须同时满足存在、未删除和启用。
        lineRepository.findByIdAndStatusAndDeletedFalse(
                        reqVO.getLineId(), CommonStatusEnum.ENABLED.getStatus())
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.DEVICE_BINDING_LINE_INVALID));
        if (reqVO.getProcessId() != null) {
            // 工序为空表示设备可上报多个工序；非空时只允许绑定启用工序。
            processRepository.findByIdAndStatusAndDeletedFalse(
                            reqVO.getProcessId(), CommonStatusEnum.ENABLED.getStatus())
                    .orElseThrow(() -> new ServiceException(
                            IntegrationErrorCodeConstants.DEVICE_BINDING_PROCESS_INVALID));
        }
        if (Boolean.TRUE.equals(reqVO.getAutoReport())
                && reqVO.getDefaultEmployeeId() == null) {
            // 自动生成报工必须有默认责任人，否则无法形成完整的人员产量记录。
            throw new ServiceException(
                    IntegrationErrorCodeConstants.DEVICE_BINDING_EMPLOYEE_INVALID);
        }
        if (reqVO.getDefaultEmployeeId() != null) {
            // 即使关闭自动报工，只要保存了默认员工，也必须确保其当前可用。
            userRepository.findByIdAndStatusAndDeletedFalse(
                            reqVO.getDefaultEmployeeId(), CommonStatusEnum.ENABLED.getStatus())
                    .orElseThrow(() -> new ServiceException(
                            IntegrationErrorCodeConstants.DEVICE_BINDING_EMPLOYEE_INVALID));
        }

        // 设备编码统一格式后执行 upsert，避免大小写差异产生重复绑定。
        String equipmentCode = reqVO.getEquipmentCode().trim().toUpperCase();
        EquipmentBindingEntity entity = bindingRepository
                .findByEquipmentCodeAndDeletedFalse(equipmentCode)
                .orElseGet(EquipmentBindingEntity::new);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        entity.setEquipmentCode(equipmentCode);
        entity.setLineId(reqVO.getLineId());
        entity.setProcessId(reqVO.getProcessId());
        entity.setDefaultEmployeeId(reqVO.getDefaultEmployeeId());
        // Boolean.TRUE.equals 将空值安全归一化为关闭，避免三态布尔传播到持久层。
        entity.setAutoReport(Boolean.TRUE.equals(reqVO.getAutoReport()));
        entity.setMaxIncrement(reqVO.getMaxIncrement());
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus()) ? 1 : 0);
        entity.setUpdateBy(operatorId);
        if (entity.getId() == null) {
            // 创建人只在首次建档时写入，后续更新仅刷新 updateBy。
            entity.setCreateBy(operatorId);
        }
        bindingRepository.save(entity);
        return entity.getId();
    }
}
