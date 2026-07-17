package com.badminton.mes.module.craft.service;

import java.util.List;

import com.badminton.mes.module.craft.controller.vo.CraftRouteStepSaveReqVO;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteProductEntity;
import com.badminton.mes.module.craft.dal.repository.CraftRouteDetailRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;
import com.badminton.mes.module.craft.service.dto.CraftRouteChildren;

import org.springframework.stereotype.Service;

/**
 * 工艺路线产品关系和步骤明细持久化服务。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Service
public class CraftRouteChildService {

    private final CraftRouteProductRepository productRepository;

    private final CraftRouteDetailRepository detailRepository;

    /**
     * 构造器注入。
     *
     * @param productRepository 路线产品关系 Repository
     * @param detailRepository  路线明细 Repository
     */
    public CraftRouteChildService(CraftRouteProductRepository productRepository,
                                  CraftRouteDetailRepository detailRepository) {
        this.productRepository = productRepository;
        this.detailRepository = detailRepository;
    }

    /**
     * 查询路线全部未删除子记录。
     *
     * @param routeId 路线主键
     * @return 路线子记录
     */
    public CraftRouteChildren load(Long routeId) {
        // 产品按主键、步骤按工序顺序稳定读取，保证接口响应和审计快照具备确定性。
        return new CraftRouteChildren(
                productRepository.findByRouteIdAndDeletedFalseOrderByProductIdAsc(routeId),
                detailRepository.findByRouteIdAndDeletedFalseOrderBySequenceNoAsc(routeId));
    }

    /**
     * 以逻辑删除旧记录、插入新记录的方式替换草稿路线子记录。
     *
     * @param routeId    路线主键
     * @param productIds 产品主键列表
     * @param steps      路线步骤
     * @param operatorId 操作人主键
     * @return 新路线子记录
     */
    public CraftRouteChildren replace(
            Long routeId,
            List<Long> productIds,
            List<CraftRouteStepSaveReqVO> steps,
            Long operatorId) {
        // 草稿编辑采用整组替换，避免逐条差异更新遗漏已从请求中移除的子记录。
        deleteAll(routeId, operatorId);
        return create(routeId, productIds, steps, operatorId);
    }

    /**
     * 首次插入路线子记录，不执行无意义的旧记录删除。
     *
     * @param routeId    路线主键
     * @param productIds 产品主键列表
     * @param steps      路线步骤
     * @param operatorId 操作人主键
     * @return 新路线子记录
     */
    public CraftRouteChildren create(
            Long routeId,
            List<Long> productIds,
            List<CraftRouteStepSaveReqVO> steps,
            Long operatorId) {

        // 先在内存构造两类实体，再分别批量写入，避免每条关系单独往返数据库。
        List<CraftRouteProductEntity> products = productIds.stream()
                .map(productId -> buildProduct(routeId, productId, operatorId))
                .toList();
        List<CraftRouteDetailEntity> details = steps.stream()
                .map(step -> buildDetail(routeId, step, operatorId))
                .toList();
        return new CraftRouteChildren(
                productRepository.saveAll(products),
                detailRepository.saveAll(details));
    }

    /**
     * 克隆源路线子记录到新版本路线。
     *
     * @param newRouteId 新路线主键
     * @param source     源路线子记录
     * @param operatorId 操作人主键
     * @return 新路线子记录
     */
    public CraftRouteChildren cloneTo(
            Long newRouteId, CraftRouteChildren source, Long operatorId) {
        // 克隆只复制业务字段，不复用旧主键、审计人和默认路线标记。
        List<CraftRouteProductEntity> products = source.products().stream()
                .map(relation -> buildProduct(newRouteId, relation.getProductId(), operatorId))
                .toList();
        List<CraftRouteDetailEntity> details = source.details().stream()
                .map(detail -> cloneDetail(newRouteId, detail, operatorId))
                .toList();
        return new CraftRouteChildren(
                productRepository.saveAll(products),
                detailRepository.saveAll(details));
    }

    /**
     * 将路线切换为其产品的默认路线。
     *
     * @param routeId    路线主键
     * @param productIds 产品主键列表
     * @param operatorId 操作人主键
     */
    public void activateDefaults(Long routeId, List<Long> productIds, Long operatorId) {
        // 必须先清除同产品的其他默认关系，再标记当前路线，维持每个产品唯一默认路线。
        productRepository.clearOtherDefaults(productIds, routeId, operatorId);
        productRepository.markRouteAsDefault(routeId, operatorId);
    }

    /**
     * 清除停用路线的默认标记。
     *
     * @param routeId    路线主键
     * @param operatorId 操作人主键
     */
    public void clearDefaults(Long routeId, Long operatorId) {
        productRepository.clearRouteDefaults(routeId, operatorId);
    }

    /**
     * 逻辑删除路线全部子记录。
     *
     * @param routeId    路线主键
     * @param operatorId 操作人主键
     */
    public void deleteAll(Long routeId, Long operatorId) {
        // 子记录采用逻辑删除保留版本审计；先明细后产品关系与聚合结构方向一致。
        detailRepository.logicDeleteByRouteId(routeId, operatorId);
        productRepository.logicDeleteByRouteId(routeId, operatorId);
    }

    /**
     * 构造路线产品关系。
     *
     * @param routeId    路线主键
     * @param productId  产品主键
     * @param operatorId 操作人主键
     * @return 待保存产品关系
     */
    private CraftRouteProductEntity buildProduct(Long routeId, Long productId, Long operatorId) {
        CraftRouteProductEntity relation = new CraftRouteProductEntity();
        relation.setRouteId(routeId);
        relation.setProductId(productId);
        // 草稿创建和版本克隆均不能自动成为默认路线，默认标记只在审核生效时切换。
        relation.setDefaultRoute(false);
        relation.setCreateBy(operatorId);
        relation.setUpdateBy(operatorId);
        return relation;
    }

    /**
     * 构造路线步骤明细。
     *
     * @param routeId    路线主键
     * @param step       步骤请求
     * @param operatorId 操作人主键
     * @return 待保存路线明细
     */
    private CraftRouteDetailEntity buildDetail(
            Long routeId, CraftRouteStepSaveReqVO step, Long operatorId) {
        CraftRouteDetailEntity detail = new CraftRouteDetailEntity();
        detail.setRouteId(routeId);
        detail.setSequenceNo(step.getSequenceNo());
        detail.setProcessId(step.getProcessId());
        detail.setStationId(step.getStationId());
        detail.setEquipmentCategoryId(step.getEquipmentCategoryId());
        detail.setInspect(step.getInspectNode());
        detail.setSopId(step.getSopId());
        detail.setQualityPlanId(step.getQualityPlanId());
        detail.setCreateBy(operatorId);
        detail.setUpdateBy(operatorId);
        return detail;
    }

    /**
     * 克隆一条路线步骤明细。
     *
     * @param newRouteId 新路线主键
     * @param source     源路线明细
     * @param operatorId 操作人主键
     * @return 待保存克隆明细
     */
    private CraftRouteDetailEntity cloneDetail(
            Long newRouteId, CraftRouteDetailEntity source, Long operatorId) {
        CraftRouteDetailEntity detail = new CraftRouteDetailEntity();
        detail.setRouteId(newRouteId);
        detail.setSequenceNo(source.getSequenceNo());
        detail.setProcessId(source.getProcessId());
        detail.setStationId(source.getStationId());
        detail.setEquipmentCategoryId(source.getEquipmentCategoryId());
        detail.setInspect(source.getInspect());
        detail.setSopId(source.getSopId());
        detail.setQualityPlanId(source.getQualityPlanId());
        detail.setCreateBy(operatorId);
        detail.setUpdateBy(operatorId);
        return detail;
    }
}
