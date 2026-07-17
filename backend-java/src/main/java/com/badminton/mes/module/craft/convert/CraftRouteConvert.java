package com.badminton.mes.module.craft.convert;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteProductRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStepRespVO;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteChangeLogEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteProductEntity;
import com.badminton.mes.module.craft.service.dto.CraftRouteSnapshotDTO;
import com.badminton.mes.module.craft.service.dto.CraftRouteStepSnapshotDTO;
import com.badminton.mes.module.production.dal.entity.ProductEntity;

/**
 * 工艺路线对象转换器。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public final class CraftRouteConvert {

    /**
     * 创建请求转路线实体。
     *
     * @param reqVO 创建请求
     * @return 路线实体
     */
    public static CraftRouteEntity toEntity(CraftRouteSaveReqVO reqVO) {
        CraftRouteEntity route = new CraftRouteEntity();
        copyToEntity(reqVO, route);
        return route;
    }

    /**
     * 复制路线主档业务字段。
     *
     * @param reqVO 保存请求
     * @param route 路线实体
     */
    public static void copyToEntity(CraftRouteSaveReqVO reqVO, CraftRouteEntity route) {
        route.setRoutingCode(reqVO.getRoutingCode());
        route.setRoutingName(reqVO.getRoutingName());
        route.setRoutingVersion(reqVO.getRoutingVersion());
        route.setSourceType(reqVO.getSourceType());
    }

    /**
     * 路线聚合转响应 VO。
     *
     * @param route        路线主档
     * @param relations    产品关系
     * @param details      路线明细
     * @param productMap   产品映射
     * @param processMap   工序映射
     * @return 路线响应
     */
    public static CraftRouteRespVO toRespVO(
            CraftRouteEntity route,
            List<CraftRouteProductEntity> relations,
            List<CraftRouteDetailEntity> details,
            Map<Long, ProductEntity> productMap,
            Map<Long, CraftProcessEntity> processMap) {
        // 先复用主表转换，再分别按已稳定排序的关系与明细补充聚合子项。
        CraftRouteRespVO respVO = toSimpleRespVO(route);
        respVO.setProducts(relations.stream()
                .map(relation -> toProductRespVO(relation, productMap.get(relation.getProductId())))
                .toList());
        respVO.setSteps(details.stream()
                .map(detail -> toStepRespVO(detail, processMap.get(detail.getProcessId())))
                .toList());
        return respVO;
    }

    /**
     * 路线主档转不含聚合子项的响应 VO。
     *
     * @param route 路线主档
     * @return 路线响应，产品和步骤为空集合
     */
    public static CraftRouteRespVO toSimpleRespVO(CraftRouteEntity route) {
        CraftRouteRespVO respVO = new CraftRouteRespVO();
        respVO.setId(route.getId());
        respVO.setRoutingCode(route.getRoutingCode());
        respVO.setRoutingName(route.getRoutingName());
        respVO.setRoutingVersion(route.getRoutingVersion());
        respVO.setPreviousRouteId(route.getPreviousRouteId());
        respVO.setSourceType(route.getSourceType());
        respVO.setRoutingStatus(route.getRoutingStatus());
        respVO.setAuditBy(route.getAuditBy());
        respVO.setAuditTime(route.getAuditTime());
        respVO.setVersion(route.getVersion());
        respVO.setProducts(Collections.emptyList());
        respVO.setSteps(Collections.emptyList());
        respVO.setCreateTime(route.getCreateTime());
        respVO.setUpdateTime(route.getUpdateTime());
        return respVO;
    }

    /**
     * 路线主档列表转响应列表。
     *
     * @param routes 路线主档列表
     * @return 路线响应列表
     */
    public static List<CraftRouteRespVO> toSimpleRespVOList(List<CraftRouteEntity> routes) {
        return routes.stream().map(CraftRouteConvert::toSimpleRespVO).toList();
    }

    /**
     * 路线聚合转审计快照。
     *
     * @param route     路线主档
     * @param relations 产品关系
     * @param details   路线明细
     * @return 审计快照
     */
    public static CraftRouteSnapshotDTO toSnapshotDTO(
            CraftRouteEntity route,
            List<CraftRouteProductEntity> relations,
            List<CraftRouteDetailEntity> details) {
        // 产品 id 排序后写入快照，避免数据库返回顺序变化造成无意义的审计差异。
        List<Long> productIds = relations.stream()
                .map(CraftRouteProductEntity::getProductId)
                .sorted()
                .toList();
        List<CraftRouteStepSnapshotDTO> steps = details.stream()
                .map(detail -> new CraftRouteStepSnapshotDTO(
                        detail.getSequenceNo(),
                        detail.getProcessId(),
                        detail.getStationId(),
                        detail.getEquipmentCategoryId(),
                        detail.getInspect(),
                        detail.getSopId(),
                        detail.getQualityPlanId()))
                .toList();
        return new CraftRouteSnapshotDTO(
                route.getRoutingCode(),
                route.getRoutingName(),
                route.getRoutingVersion(),
                route.getPreviousRouteId(),
                route.getSourceType(),
                route.getRoutingStatus(),
                route.getVersion(),
                productIds,
                steps);
    }

    /**
     * 路线变更日志实体转响应。
     *
     * @param log 日志实体
     * @return 日志响应
     */
    public static CraftRouteChangeLogRespVO toChangeLogRespVO(CraftRouteChangeLogEntity log) {
        CraftRouteChangeLogRespVO respVO = new CraftRouteChangeLogRespVO();
        respVO.setId(log.getId());
        respVO.setRouteId(log.getRouteId());
        respVO.setChangeType(log.getChangeType());
        respVO.setBeforeSnapshot(log.getBeforeSnapshot());
        respVO.setAfterSnapshot(log.getAfterSnapshot());
        respVO.setChangeReason(log.getChangeReason());
        respVO.setOperatorId(log.getOperatorId());
        respVO.setCreateTime(log.getCreateTime());
        return respVO;
    }

    /**
     * 路线变更日志列表转响应列表。
     *
     * @param logs 日志实体列表
     * @return 日志响应列表
     */
    public static List<CraftRouteChangeLogRespVO> toChangeLogRespVOList(
            List<CraftRouteChangeLogEntity> logs) {
        return logs.stream().map(CraftRouteConvert::toChangeLogRespVO).toList();
    }

    /**
     * 产品关系转响应对象。
     *
     * @param relation 产品关系
     * @param product  产品档案，历史数据缺失时可为 null
     * @return 产品关系响应
     */
    private static CraftRouteProductRespVO toProductRespVO(
            CraftRouteProductEntity relation, ProductEntity product) {
        CraftRouteProductRespVO respVO = new CraftRouteProductRespVO();
        respVO.setProductId(relation.getProductId());
        respVO.setDefaultRoute(relation.getDefaultRoute());
        if (product != null) {
            // 历史产品档案缺失时仍返回关系中的 productId，不阻断旧路线详情查询。
            respVO.setProductCode(product.getProductCode());
            respVO.setProductName(product.getProductName());
        }
        return respVO;
    }

    /**
     * 路线明细转步骤响应对象。
     *
     * @param detail  路线明细
     * @param process 工序档案，历史数据缺失时可为 null
     * @return 路线步骤响应
     */
    private static CraftRouteStepRespVO toStepRespVO(
            CraftRouteDetailEntity detail, CraftProcessEntity process) {
        CraftRouteStepRespVO respVO = new CraftRouteStepRespVO();
        respVO.setId(detail.getId());
        respVO.setSequenceNo(detail.getSequenceNo());
        respVO.setProcessId(detail.getProcessId());
        respVO.setStationId(detail.getStationId());
        respVO.setEquipmentCategoryId(detail.getEquipmentCategoryId());
        respVO.setInspectNode(detail.getInspect());
        respVO.setSopId(detail.getSopId());
        respVO.setQualityPlanId(detail.getQualityPlanId());
        if (process != null) {
            // 工序展示字段从当前档案补充；持久化步骤控制字段仍以路线明细为准。
            respVO.setProcessCode(process.getProcessCode());
            respVO.setProcessName(process.getProcessName());
            respVO.setKeyProcess(process.getKeyProcess());
            respVO.setScanRequired(process.getScanRequired());
            respVO.setPieceRateEnabled(process.getPieceRateEnabled());
        }
        return respVO;
    }

    /** 工具类禁止实例化。 */
    private CraftRouteConvert() {
    }
}
