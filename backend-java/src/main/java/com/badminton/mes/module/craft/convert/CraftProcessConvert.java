package com.badminton.mes.module.craft.convert;

import java.util.List;

import com.badminton.mes.module.craft.controller.vo.CraftProcessChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessDefectReasonRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessRuleRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftProcessSopRespVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessChangeLogEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessDefectReasonEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.entity.CraftProcessSopEntity;
import com.badminton.mes.module.craft.service.dto.CraftProcessSnapshotDTO;

/**
 * 工序档案对象转换器。
 *
 * <p>转换器只做字段搬运与派生字段组装，不访问数据库或当前登录用户。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public final class CraftProcessConvert {

    /**
     * 保存请求转工序实体。
     *
     * @param reqVO 保存请求
     * @return 工序实体
     */
    public static CraftProcessEntity toEntity(CraftProcessSaveReqVO reqVO) {
        CraftProcessEntity process = new CraftProcessEntity();
        copyToEntity(reqVO, process);
        return process;
    }

    /**
     * 将保存请求的业务字段复制到工序实体。
     *
     * @param reqVO   保存请求
     * @param process 目标工序实体
     */
    public static void copyToEntity(CraftProcessSaveReqVO reqVO, CraftProcessEntity process) {
        process.setProcessCode(reqVO.getProcessCode());
        process.setProcessName(reqVO.getProcessName());
        process.setProcessType(reqVO.getProcessType());
        process.setStandardTimeSeconds(reqVO.getStandardTimeSeconds());
        process.setKeyProcess(reqVO.getKeyProcess());
        process.setQualityRequired(reqVO.getQualityRequired());
        process.setScanRequired(reqVO.getScanRequired());
        process.setPieceRateEnabled(reqVO.getPieceRateEnabled());
        process.setEquipmentCategoryId(reqVO.getEquipmentCategoryId());
        process.setQualityPlanId(reqVO.getQualityPlanId());
        process.setRemark(reqVO.getRemark());
    }

    /**
     * 工序实体转响应 VO。
     *
     * @param process 工序实体
     * @return 工序响应
     */
    public static CraftProcessRespVO toRespVO(CraftProcessEntity process) {
        CraftProcessRespVO respVO = new CraftProcessRespVO();
        respVO.setId(process.getId());
        respVO.setProcessCode(process.getProcessCode());
        respVO.setProcessName(process.getProcessName());
        respVO.setProcessType(process.getProcessType());
        respVO.setStandardTimeSeconds(process.getStandardTimeSeconds());
        respVO.setKeyProcess(process.getKeyProcess());
        // 当前模型将关键工序同时视为必须报工、必须配置人员，响应字段由主档规则派生。
        respVO.setReportRequired(process.getKeyProcess());
        respVO.setPersonnelRequired(process.getKeyProcess());
        respVO.setQualityRequired(process.getQualityRequired());
        respVO.setScanRequired(process.getScanRequired());
        respVO.setPieceRateEnabled(process.getPieceRateEnabled());
        respVO.setEquipmentCategoryId(process.getEquipmentCategoryId());
        respVO.setQualityPlanId(process.getQualityPlanId());
        respVO.setRemark(process.getRemark());
        respVO.setStatus(process.getStatus());
        respVO.setVersion(process.getVersion());
        respVO.setCreateTime(process.getCreateTime());
        respVO.setUpdateTime(process.getUpdateTime());
        return respVO;
    }

    /**
     * 工序实体列表转响应列表。
     *
     * @param processes 工序实体列表
     * @return 工序响应列表
     */
    public static List<CraftProcessRespVO> toRespVOList(List<CraftProcessEntity> processes) {
        return processes.stream().map(CraftProcessConvert::toRespVO).toList();
    }

    /**
     * 工序实体转执行规则响应。
     *
     * @param process 工序实体
     * @return 工序规则响应
     */
    public static CraftProcessRuleRespVO toRuleRespVO(CraftProcessEntity process) {
        CraftProcessRuleRespVO respVO = new CraftProcessRuleRespVO();
        respVO.setId(process.getId());
        respVO.setProcessCode(process.getProcessCode());
        respVO.setProcessName(process.getProcessName());
        respVO.setProcessType(process.getProcessType());
        respVO.setStandardTimeSeconds(process.getStandardTimeSeconds());
        respVO.setKeyProcess(process.getKeyProcess());
        respVO.setReportRequired(process.getKeyProcess());
        respVO.setPersonnelRequired(process.getKeyProcess());
        respVO.setQualityRequired(process.getQualityRequired());
        respVO.setScanRequired(process.getScanRequired());
        respVO.setPieceRateEnabled(process.getPieceRateEnabled());
        respVO.setEquipmentCategoryId(process.getEquipmentCategoryId());
        respVO.setQualityPlanId(process.getQualityPlanId());
        return respVO;
    }

    /**
     * 工序实体转变更快照。
     *
     * @param process 工序实体
     * @return 工序快照
     */
    public static CraftProcessSnapshotDTO toSnapshotDTO(CraftProcessEntity process) {
        return new CraftProcessSnapshotDTO(
                process.getProcessCode(),
                process.getProcessName(),
                process.getProcessType(),
                process.getStandardTimeSeconds(),
                process.getKeyProcess(),
                process.getQualityRequired(),
                process.getScanRequired(),
                process.getPieceRateEnabled(),
                process.getEquipmentCategoryId(),
                process.getQualityPlanId(),
                process.getRemark(),
                process.getStatus(),
                process.getVersion());
    }

    /**
     * 变更日志实体转响应 VO。
     *
     * @param changeLog 变更日志实体
     * @return 日志响应
     */
    public static CraftProcessChangeLogRespVO toChangeLogRespVO(CraftProcessChangeLogEntity changeLog) {
        CraftProcessChangeLogRespVO respVO = new CraftProcessChangeLogRespVO();
        respVO.setId(changeLog.getId());
        respVO.setProcessId(changeLog.getProcessId());
        respVO.setChangeType(changeLog.getChangeType());
        respVO.setBeforeSnapshot(changeLog.getBeforeSnapshot());
        respVO.setAfterSnapshot(changeLog.getAfterSnapshot());
        respVO.setChangeReason(changeLog.getChangeReason());
        respVO.setOperatorId(changeLog.getOperatorId());
        respVO.setCreateTime(changeLog.getCreateTime());
        return respVO;
    }

    /**
     * 变更日志列表转响应列表。
     *
     * @param changeLogs 变更日志列表
     * @return 日志响应列表
     */
    public static List<CraftProcessChangeLogRespVO> toChangeLogRespVOList(
            List<CraftProcessChangeLogEntity> changeLogs) {
        return changeLogs.stream().map(CraftProcessConvert::toChangeLogRespVO).toList();
    }

    /**
     * 工序 SOP 实体转响应 VO。
     *
     * @param sop SOP 实体
     * @return SOP 响应
     */
    public static CraftProcessSopRespVO toSopRespVO(CraftProcessSopEntity sop) {
        CraftProcessSopRespVO respVO = new CraftProcessSopRespVO();
        respVO.setId(sop.getId());
        respVO.setProcessId(sop.getProcessId());
        respVO.setSopCode(sop.getSopCode());
        respVO.setSopName(sop.getSopName());
        respVO.setSopVersion(sop.getSopVersion());
        respVO.setFileUrl(sop.getFileUrl());
        respVO.setStatus(sop.getStatus());
        respVO.setVersion(sop.getVersion());
        // 已停用 SOP 不能继续被新路线使用，前端以该派生标记提示重新绑定。
        respVO.setRebindRequired(Integer.valueOf(0).equals(sop.getStatus()));
        respVO.setCreateTime(sop.getCreateTime());
        respVO.setUpdateTime(sop.getUpdateTime());
        return respVO;
    }

    /**
     * 工序 SOP 列表转响应列表。
     *
     * @param sops SOP 实体列表
     * @return SOP 响应列表
     */
    public static List<CraftProcessSopRespVO> toSopRespVOList(List<CraftProcessSopEntity> sops) {
        return sops.stream().map(CraftProcessConvert::toSopRespVO).toList();
    }

    /**
     * 工序不良原因实体转响应 VO。
     *
     * @param reason 不良原因实体
     * @return 不良原因响应
     */
    public static CraftProcessDefectReasonRespVO toDefectReasonRespVO(
            CraftProcessDefectReasonEntity reason) {
        CraftProcessDefectReasonRespVO respVO = new CraftProcessDefectReasonRespVO();
        respVO.setId(reason.getId());
        respVO.setProcessId(reason.getProcessId());
        respVO.setReasonCode(reason.getReasonCode());
        respVO.setReasonName(reason.getReasonName());
        respVO.setStatus(reason.getStatus());
        respVO.setVersion(reason.getVersion());
        respVO.setCreateTime(reason.getCreateTime());
        respVO.setUpdateTime(reason.getUpdateTime());
        return respVO;
    }

    /**
     * 工序不良原因列表转响应列表。
     *
     * @param reasons 不良原因实体列表
     * @return 不良原因响应列表
     */
    public static List<CraftProcessDefectReasonRespVO> toDefectReasonRespVOList(
            List<CraftProcessDefectReasonEntity> reasons) {
        return reasons.stream().map(CraftProcessConvert::toDefectReasonRespVO).toList();
    }

    private CraftProcessConvert() {
    }
}
