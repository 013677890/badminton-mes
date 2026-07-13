package com.badminton.mes.module.production.service.impl;

import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.WorkshopRespVO;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.service.WorkshopService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 车间档案只读 Service 实现。 */
@Service
public class WorkshopServiceImpl implements WorkshopService {

    private final WorkshopRepository workshopRepository;

    public WorkshopServiceImpl(WorkshopRepository workshopRepository) {
        this.workshopRepository = workshopRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkshopRespVO getEnabledWorkshop(Long id) {
        WorkshopEntity workshop = workshopRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.WORKSHOP_NOT_EXISTS));
        if (!CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORKSHOP_NOT_EXISTS);
        }

        WorkshopRespVO response = new WorkshopRespVO();
        response.setId(workshop.getId());
        response.setWorkshopCode(workshop.getWorkshopCode());
        response.setWorkshopName(workshop.getWorkshopName());
        response.setStatus(workshop.getStatus());
        return response;
    }
}
