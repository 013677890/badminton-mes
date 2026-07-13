package com.badminton.mes.module.scene.service;

import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.*;
import com.badminton.mes.module.scene.constants.SceneErrorCodeConstants;
import org.springframework.stereotype.Component;

/** scene 对象级车间、产线数据权限。 @author 刘涵 */
@Component
public class SceneDataScopeService {
    public void check(Long workshopId, Long lineId) {
        LoginUser user=SecurityContextHolder.getRequiredLoginUser();
        if(user.getRoleCodes().contains(RoleCodeConstants.ADMIN)||user.getRoleCodes().contains(RoleCodeConstants.PMC)) return;
        if(user.getWorkshopId()!=null&&!user.getWorkshopId().equals(workshopId)) deny();
        boolean workshopManager=user.getRoleCodes().contains(RoleCodeConstants.WORKSHOP_MANAGER);
        if(!workshopManager&&user.getLineId()!=null&&!user.getLineId().equals(lineId)) deny();
    }
    private void deny(){throw new ServiceException(SceneErrorCodeConstants.DATA_SCOPE_DENIED);}
}
