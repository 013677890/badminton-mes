package com.badminton.mes.module.production.service;

import com.badminton.mes.module.production.controller.vo.WorkshopRespVO;

/** 车间档案只读 Service，供业务模块校验车间引用。 */
public interface WorkshopService {

    /**
     * 查询启用且未删除的车间。
     *
     * @param id 车间主键
     * @return 车间摘要
     */
    WorkshopRespVO getEnabledWorkshop(Long id);
}
