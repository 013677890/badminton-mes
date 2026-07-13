package com.badminton.mes.module.andon.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;

/** 安灯异常处理配置 Service。 */
public interface AndonConfigurationService {

    Long createConfiguration(AndonConfigurationSaveReqVO request);

    void updateConfiguration(Long id, AndonConfigurationSaveReqVO request);

    void deleteConfiguration(Long id);

    AndonConfigurationRespVO getConfiguration(Long id);

    PageResult<AndonConfigurationRespVO> getConfigurationPage(
            AndonConfigurationPageReqVO request);
}
