package com.badminton.mes.module.production.convert;

import java.util.List;

import com.badminton.mes.module.production.controller.vo.DispatchAdjustLogRespVO;
import com.badminton.mes.module.production.controller.vo.DispatchRespVO;
import com.badminton.mes.module.production.dal.entity.DispatchAdjustLogEntity;
import com.badminton.mes.module.production.dal.entity.DispatchOrderEntity;

/**
 * 派工单实体与 VO 转换器。名称类冗余字段(工单号/产线名/班次名)由 Service 批量回填。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
public final class DispatchOrderConvert {

    /**
     * 派工单实体转响应 VO(仅本表字段)。
     *
     * @param entity 派工单实体
     * @return 响应 VO
     */
    public static DispatchRespVO toRespVO(DispatchOrderEntity entity) {
        // 这里只转换派工单自身字段；工单号、产品、产线和班次名称由 Service 批量查询后回填。
        DispatchRespVO respVO = new DispatchRespVO();
        respVO.setId(entity.getId());
        respVO.setDispatchNo(entity.getDispatchNo());
        respVO.setWorkOrderId(entity.getWorkOrderId());
        respVO.setLineId(entity.getLineId());
        respVO.setShiftId(entity.getShiftId());
        respVO.setPlanDate(entity.getPlanDate());
        respVO.setPlanQuantity(entity.getPlanQuantity());
        respVO.setPlanStartTime(entity.getPlanStartTime());
        respVO.setPlanEndTime(entity.getPlanEndTime());
        respVO.setSuggest(entity.getSuggest());
        respVO.setDispatchStatus(entity.getDispatchStatus());
        respVO.setAuditBy(entity.getAuditBy());
        respVO.setAuditTime(entity.getAuditTime());
        respVO.setAdjustReason(entity.getAdjustReason());
        respVO.setCreateBy(entity.getCreateBy());
        respVO.setCreateTime(entity.getCreateTime());
        return respVO;
    }

    /**
     * 调整日志实体列表转响应 VO 列表。
     *
     * @param entities 调整日志实体列表
     * @return 响应 VO 列表
     */
    public static List<DispatchAdjustLogRespVO> toAdjustLogRespVOList(List<DispatchAdjustLogEntity> entities) {
        // 日志按 Repository 的时间顺序原样映射，不在转换层重新排序或改变审计快照。
        return entities.stream().map(entity -> {
            DispatchAdjustLogRespVO respVO = new DispatchAdjustLogRespVO();
            respVO.setId(entity.getId());
            respVO.setDispatchOrderId(entity.getDispatchOrderId());
            respVO.setAdjustType(entity.getAdjustType());
            respVO.setBeforeSnapshot(entity.getBeforeSnapshot());
            respVO.setAfterSnapshot(entity.getAfterSnapshot());
            respVO.setAdjustReason(entity.getAdjustReason());
            respVO.setOperatorId(entity.getOperatorId());
            respVO.setCreateTime(entity.getCreateTime());
            return respVO;
        }).toList();
    }

    private DispatchOrderConvert() {
        // 转换器只提供静态方法，不允许实例化。
    }
}
