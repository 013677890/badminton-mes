package com.badminton.mes.module.scene.service;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import org.springframework.stereotype.Component;

/**
 * scene 对象级车间、产线数据权限组件。
 *
 * <p>现场 Service 在读取或修改带车间/产线归属的数据前调用 {@link #check(Long, Long)}；
 * 管理员和 PMC 可跨范围访问，车间主管限制在所属车间，普通用户进一步限制到所属产线。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
@Component
public class SceneDataScopeService {

    /** 校验当前登录用户是否有权访问指定车间和产线对象。 */
    public void check(Long workshopId, Long lineId) {
        LoginUser user=SecurityContextHolder.getRequiredLoginUser();
        if(user.getRoleCodes().contains(RoleCodeConstants.ADMIN)||user.getRoleCodes().contains(RoleCodeConstants.PMC)) return;
        if(user.getWorkshopId()!=null&&!user.getWorkshopId().equals(workshopId)) deny();
        boolean workshopManager=user.getRoleCodes().contains(RoleCodeConstants.WORKSHOP_MANAGER);
        if(!workshopManager&&user.getLineId()!=null&&!user.getLineId().equals(lineId)) deny();
    }
    private void deny(){throw new ServiceException(SceneErrorCodeConstants.DATA_SCOPE_DENIED);}
}
