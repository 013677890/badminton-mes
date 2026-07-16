package com.badminton.mes.module.scene.service;

import com.badminton.mes.module.integration.dal.entity.DeviceCountRecordEntity;
import com.badminton.mes.module.integration.dal.entity.EquipmentBindingEntity;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportReverseReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneWorkReportSubmitReqVO;

/**
 * 生产报工服务。
 *
 * <p>由现场报工 Controller 和设备计数写入服务调用；实现类负责把人工或设备来源
 * 转换为统一报工事实，并协调工单进度、工资统计和库存等下游数据。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface SceneWorkReportService {

    /** 提交一次人工/现场生产报工。 */
    Long submit(SceneWorkReportSubmitReqVO req, Integer sourceType);

    /** 冲销一条已入账报工，并记录冲销原因。 */
    Long reverse(Long id, SceneWorkReportReverseReqVO req);

    /** 将设备计数记录转换为报工，供设备接入链路调用。 */
    Long createDeviceReport(DeviceCountRecordEntity sourceRecord,
                            EquipmentBindingEntity binding,
                            DispatchOrderEntity dispatchOrder);
}
