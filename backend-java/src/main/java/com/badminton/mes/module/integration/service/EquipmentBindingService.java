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
 * @author 张竹灏
 * @date 2026/07/13
 */
@Service
public class EquipmentBindingService {

    private final EquipmentBindingRepository bindingRepository;
    private final ProductionLineRepository lineRepository;
    private final CraftProcessRepository processRepository;
    private final UserRepository userRepository;

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
        lineRepository.findByIdAndStatusAndDeletedFalse(
                        reqVO.getLineId(), CommonStatusEnum.ENABLED.getStatus())
                .orElseThrow(() -> new ServiceException(
                        IntegrationErrorCodeConstants.DEVICE_BINDING_LINE_INVALID));
        if (reqVO.getProcessId() != null) {
            processRepository.findByIdAndStatusAndDeletedFalse(
                            reqVO.getProcessId(), CommonStatusEnum.ENABLED.getStatus())
                    .orElseThrow(() -> new ServiceException(
                            IntegrationErrorCodeConstants.DEVICE_BINDING_PROCESS_INVALID));
        }
        if (Boolean.TRUE.equals(reqVO.getAutoReport())
                && reqVO.getDefaultEmployeeId() == null) {
            throw new ServiceException(
                    IntegrationErrorCodeConstants.DEVICE_BINDING_EMPLOYEE_INVALID);
        }
        if (reqVO.getDefaultEmployeeId() != null) {
            userRepository.findByIdAndStatusAndDeletedFalse(
                            reqVO.getDefaultEmployeeId(), CommonStatusEnum.ENABLED.getStatus())
                    .orElseThrow(() -> new ServiceException(
                            IntegrationErrorCodeConstants.DEVICE_BINDING_EMPLOYEE_INVALID));
        }

        String equipmentCode = reqVO.getEquipmentCode().trim().toUpperCase();
        EquipmentBindingEntity entity = bindingRepository
                .findByEquipmentCodeAndDeletedFalse(equipmentCode)
                .orElseGet(EquipmentBindingEntity::new);
        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        entity.setEquipmentCode(equipmentCode);
        entity.setLineId(reqVO.getLineId());
        entity.setProcessId(reqVO.getProcessId());
        entity.setDefaultEmployeeId(reqVO.getDefaultEmployeeId());
        entity.setAutoReport(Boolean.TRUE.equals(reqVO.getAutoReport()));
        entity.setMaxIncrement(reqVO.getMaxIncrement());
        entity.setStatus(CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus()) ? 1 : 0);
        entity.setUpdateBy(operatorId);
        if (entity.getId() == null) {
            entity.setCreateBy(operatorId);
        }
        bindingRepository.save(entity);
        return entity.getId();
    }
}
