package com.badminton.mes.module.integration.service;

import com.badminton.mes.module.integration.service.dto.ApprovedCompletionDTO;

/**
 * 审核通过完工单发布契约。
 *
 * @author 张竹灏
 * @date 2026/07/14
 */
public interface CompletionOrderPublishService {

    /**
     * 幂等发布审核通过的完工单，供 ERP/WMS 读取接口查询。
     *
     * @param completion 审核通过的完工数据
     * @return A 组完工读取表主键
     */
    Long publishApproved(ApprovedCompletionDTO completion);
}
