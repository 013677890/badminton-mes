package com.badminton.mes.module.integration.service;

import java.util.List;

import com.badminton.mes.module.integration.service.dto.ErpCraftDTO;
import com.badminton.mes.module.integration.service.dto.ErpTaskDTO;

/**
 * ERP 生产任务和工艺数据源。
 *
 * @author 张竹灏
 * @date 2026/07/13
 */
public interface ErpDataSource {

    /** 请求未指定来源时使用的稳定 ERP 系统标识，同时参与同步幂等键。 */
    String DEFAULT_SOURCE_SYSTEM = "ERP";

    /** 读取 ERP 生产任务。 */
    List<ErpTaskDTO> fetchTasks();

    /** 读取 ERP 工艺路线。 */
    List<ErpCraftDTO> fetchCrafts();
}
