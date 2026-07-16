package com.badminton.mes.module.scene.service;
import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.scene.controller.vo.*;
/** 生产报工服务。 @author 刘涵 */
public interface SceneWorkReportService{
 Long submit(SceneWorkReportSubmitReqVO req,Integer sourceType);
 Long reverse(Long id,SceneWorkReportReverseReqVO req);
 Long createDeviceReport(DeviceCountRecordEntity sourceRecord, EquipmentBindingEntity binding,
                         DispatchOrderEntity dispatchOrder);
}
