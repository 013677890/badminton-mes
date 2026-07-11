package com.badminton.mes.module.craft.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.craft.constants.CraftErrorCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteNewVersionReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRoutePageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStatusReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteUpdateReqVO;
import com.badminton.mes.module.craft.convert.CraftRouteConvert;
import com.badminton.mes.module.craft.dal.entity.CraftProcessEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteChangeLogEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteDetailEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteEntity;
import com.badminton.mes.module.craft.dal.entity.CraftRouteProductEntity;
import com.badminton.mes.module.craft.dal.repository.CraftProcessRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteChangeLogRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteProductRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteRepository;
import com.badminton.mes.module.craft.dal.repository.CraftRouteSpecifications;
import com.badminton.mes.module.craft.enums.CraftRouteChangeTypeEnum;
import com.badminton.mes.module.craft.enums.CraftRouteStatusEnum;
import com.badminton.mes.module.craft.service.CraftRouteAuditService;
import com.badminton.mes.module.craft.service.CraftRouteChildService;
import com.badminton.mes.module.craft.service.CraftRouteReferenceValidator;
import com.badminton.mes.module.craft.service.CraftRouteService;
import com.badminton.mes.module.craft.service.dto.CraftRouteChildren;
import com.badminton.mes.module.craft.service.dto.CraftRouteSnapshotDTO;
import com.badminton.mes.module.craft.service.support.CraftPersistenceExceptionTranslator;
import com.badminton.mes.module.craft.service.support.CraftVersionValidator;
import com.badminton.mes.module.production.dal.entity.ProductEntity;
import com.badminton.mes.module.production.dal.repository.ProductRepository;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 工艺路线 Service 实现。
 *
 * <p>负责路线聚合的生命周期编排：草稿创建与修改、审核生效与默认路线切换、
 * 停用、版本演进和变更日志；引用校验与子记录持久化分别委托
 * {@link CraftRouteReferenceValidator} 和 {@link CraftRouteChildService}。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Service
public class CraftRouteServiceImpl implements CraftRouteService {

    private static final String ACTIVE_CODE_VERSION_CONSTRAINT = "uk_active_code_version";

    private static final String CREATE_REASON = "创建工艺路线";

    private static final String DELETE_REASON = "删除工艺路线草稿";

    private final CraftRouteRepository routeRepository;

    private final CraftRouteProductRepository routeProductRepository;

    private final CraftRouteChangeLogRepository changeLogRepository;

    private final CraftProcessRepository processRepository;

    private final ProductRepository productRepository;

    private final WorkOrderRepository workOrderRepository;

    private final CraftRouteChildService childService;

    private final CraftRouteReferenceValidator referenceValidator;

    private final CraftRouteAuditService auditService;

    /**
     * 构造器注入。
     *
     * @param routeRepository        路线主档 Repository
     * @param routeProductRepository 路线产品关系 Repository
     * @param changeLogRepository    路线变更日志 Repository
     * @param processRepository      工序 Repository
     * @param productRepository      产品 Repository
     * @param workOrderRepository    生产工单 Repository
     * @param childService           路线子记录持久化服务
     * @param referenceValidator     路线引用校验器
     * @param auditService           路线变更审计服务
     */
    public CraftRouteServiceImpl(CraftRouteRepository routeRepository,
                                 CraftRouteProductRepository routeProductRepository,
                                 CraftRouteChangeLogRepository changeLogRepository,
                                 CraftProcessRepository processRepository,
                                 ProductRepository productRepository,
                                 WorkOrderRepository workOrderRepository,
                                 CraftRouteChildService childService,
                                 CraftRouteReferenceValidator referenceValidator,
                                 CraftRouteAuditService auditService) {
        this.routeRepository = routeRepository;
        this.routeProductRepository = routeProductRepository;
        this.changeLogRepository = changeLogRepository;
        this.processRepository = processRepository;
        this.productRepository = productRepository;
        this.workOrderRepository = workOrderRepository;
        this.childService = childService;
        this.referenceValidator = referenceValidator;
        this.auditService = auditService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoute(CraftRouteSaveReqVO reqVO) {
        normalizeSaveRequest(reqVO);
        validateCodeVersion(reqVO.getRoutingCode(), reqVO.getRoutingVersion(), null);
        referenceValidator.validateForSave(reqVO.getProductIds(), reqVO.getSteps());

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftRouteEntity route = CraftRouteConvert.toEntity(reqVO);
        route.setRoutingStatus(CraftRouteStatusEnum.DRAFT.getStatus());
        route.setCreateBy(operatorId);
        route.setUpdateBy(operatorId);
        saveRoute(route);

        CraftRouteChildren children = childService.create(
                route.getId(), reqVO.getProductIds(), reqVO.getSteps(), operatorId);
        auditService.record(route.getId(), CraftRouteChangeTypeEnum.CREATE,
                null, toSnapshot(route, children),
                defaultReason(reqVO.getChangeReason(), CREATE_REASON), operatorId);
        return route.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoute(Long id, CraftRouteUpdateReqVO reqVO) {
        CraftRouteEntity route = requireRoute(id);
        CraftVersionValidator.validate(route.getVersion(), reqVO.getVersion(),
                CraftErrorCodeConstants.ROUTE_CONCURRENT_MODIFICATION);
        requireDraft(route);
        normalizeSaveRequest(reqVO);
        boolean versionIdentityChanged = route.getPreviousRouteId() != null
                && (!route.getRoutingCode().equals(reqVO.getRoutingCode())
                || !route.getSourceType().equals(reqVO.getSourceType()));
        if (versionIdentityChanged) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_VERSION_IDENTITY_IMMUTABLE);
        }
        validateCodeVersion(reqVO.getRoutingCode(), reqVO.getRoutingVersion(), id);
        referenceValidator.validateForSave(reqVO.getProductIds(), reqVO.getSteps());

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftRouteSnapshotDTO beforeSnapshot = toSnapshot(route, childService.load(id));
        CraftRouteConvert.copyToEntity(reqVO, route);
        route.setUpdateBy(operatorId);
        saveRoute(route);

        CraftRouteChildren children = childService.replace(
                id, reqVO.getProductIds(), reqVO.getSteps(), operatorId);
        auditService.record(id, CraftRouteChangeTypeEnum.UPDATE,
                beforeSnapshot, toSnapshot(route, children), reqVO.getChangeReason(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoute(Long id, Integer expectedVersion) {
        CraftRouteEntity route = requireRouteForUpdate(id);
        CraftVersionValidator.validate(route.getVersion(), expectedVersion,
                CraftErrorCodeConstants.ROUTE_CONCURRENT_MODIFICATION);
        requireDraft(route);
        if (workOrderRepository.existsByRoutingIdAndDeletedFalse(id)) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_REFERENCED_BY_WORK_ORDER);
        }

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftRouteSnapshotDTO beforeSnapshot = toSnapshot(route, childService.load(id));
        route.setDeleted(true);
        route.setUpdateBy(operatorId);
        saveRoute(route);
        childService.deleteAll(id, operatorId);
        auditService.record(id, CraftRouteChangeTypeEnum.DELETE,
                beforeSnapshot, null, DELETE_REASON, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRoute(Long id, CraftRouteStatusReqVO reqVO) {
        CraftRouteEntity route = requireRouteForUpdate(id);
        CraftVersionValidator.validate(route.getVersion(), reqVO.getVersion(),
                CraftErrorCodeConstants.ROUTE_CONCURRENT_MODIFICATION);
        requireDraft(route);

        CraftRouteChildren children = childService.load(id);
        if (children.products().isEmpty() || children.details().isEmpty()) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_CONFIGURATION_INCOMPLETE);
        }
        List<Long> productIds = children.products().stream()
                .map(CraftRouteProductEntity::getProductId)
                .toList();
        productRepository.findAllByIdInForUpdateOrderByIdAsc(productIds);
        referenceValidator.validateForApproval(children.products(), children.details());

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftRouteSnapshotDTO beforeSnapshot = toSnapshot(route, children);
        route.setRoutingStatus(CraftRouteStatusEnum.EFFECTIVE.getStatus());
        route.setAuditBy(operatorId);
        route.setAuditTime(LocalDateTime.now());
        route.setUpdateBy(operatorId);
        saveRoute(route);
        childService.activateDefaults(id, productIds, operatorId);
        auditService.record(id, CraftRouteChangeTypeEnum.APPROVE,
                beforeSnapshot, toSnapshot(route, children), reqVO.getReason().trim(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableRoute(Long id, CraftRouteStatusReqVO reqVO) {
        CraftRouteEntity route = requireRouteForUpdate(id);
        CraftVersionValidator.validate(route.getVersion(), reqVO.getVersion(),
                CraftErrorCodeConstants.ROUTE_CONCURRENT_MODIFICATION);
        requireEffective(route);

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftRouteChildren children = childService.load(id);
        CraftRouteSnapshotDTO beforeSnapshot = toSnapshot(route, children);
        route.setRoutingStatus(CraftRouteStatusEnum.DISABLED.getStatus());
        route.setUpdateBy(operatorId);
        saveRoute(route);
        childService.clearDefaults(id, operatorId);
        auditService.record(id, CraftRouteChangeTypeEnum.DISABLE,
                beforeSnapshot, toSnapshot(route, children), reqVO.getReason().trim(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRouteVersion(Long id, CraftRouteNewVersionReqVO reqVO) {
        CraftRouteEntity source = requireRouteForUpdate(id);
        CraftVersionValidator.validate(source.getVersion(), reqVO.getVersion(),
                CraftErrorCodeConstants.ROUTE_CONCURRENT_MODIFICATION);
        requireEffective(source);
        String newRoutingVersion = reqVO.getNewRoutingVersion().trim().toUpperCase(Locale.ROOT);
        validateCodeVersion(source.getRoutingCode(), newRoutingVersion, null);

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        CraftRouteEntity newRoute = new CraftRouteEntity();
        newRoute.setRoutingCode(source.getRoutingCode());
        newRoute.setRoutingName(source.getRoutingName());
        newRoute.setRoutingVersion(newRoutingVersion);
        newRoute.setPreviousRouteId(source.getId());
        newRoute.setSourceType(source.getSourceType());
        newRoute.setRoutingStatus(CraftRouteStatusEnum.DRAFT.getStatus());
        newRoute.setCreateBy(operatorId);
        newRoute.setUpdateBy(operatorId);
        saveRoute(newRoute);

        CraftRouteChildren sourceChildren = childService.load(id);
        CraftRouteChildren newChildren =
                childService.cloneTo(newRoute.getId(), sourceChildren, operatorId);
        auditService.record(newRoute.getId(), CraftRouteChangeTypeEnum.CREATE_VERSION,
                toSnapshot(source, sourceChildren), toSnapshot(newRoute, newChildren),
                reqVO.getReason().trim(), operatorId);
        return newRoute.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public CraftRouteRespVO getRoute(Long id) {
        return buildRouteDetail(requireRoute(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CraftRouteRespVO> getRoutePage(CraftRoutePageReqVO reqVO) {
        Specification<CraftRouteEntity> specification = CraftRouteSpecifications.page(reqVO);
        long total = routeRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.ASC, "routingCode").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<CraftRouteEntity> page = routeRepository.findAll(specification, pageRequest);
        return PageResult.of(CraftRouteConvert.toSimpleRespVOList(page.getContent()),
                total, pageNo, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public CraftRouteRespVO getDefaultRoute(Long productId) {
        CraftRouteProductEntity relation = routeProductRepository
                .findByProductIdAndDefaultRouteTrueAndDeletedFalse(productId)
                .orElseThrow(() -> new ServiceException(CraftErrorCodeConstants.ROUTE_DEFAULT_NOT_FOUND));
        CraftRouteEntity route = routeRepository.findByIdAndDeletedFalse(relation.getRouteId())
                .orElseThrow(() -> new ServiceException(CraftErrorCodeConstants.ROUTE_DEFAULT_NOT_FOUND));
        if (!CraftRouteStatusEnum.EFFECTIVE.getStatus().equals(route.getRoutingStatus())) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_DEFAULT_NOT_FOUND);
        }
        return buildRouteDetail(route);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CraftRouteChangeLogRespVO> getRouteChangeLogPage(
            Long id, CraftRouteChangeLogPageReqVO reqVO) {
        if (!routeRepository.existsById(id)) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_NOT_EXISTS);
        }
        long total = changeLogRepository.countByRouteIdAndDeletedFalse(id);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageSize = reqVO.getPageSize();
        int pageNo = normalizePageNo(reqVO.getPageNo(), pageSize, total);
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<CraftRouteChangeLogEntity> page =
                changeLogRepository.findByRouteIdAndDeletedFalse(id, pageRequest);
        return PageResult.of(CraftRouteConvert.toChangeLogRespVOList(page.getContent()),
                total, pageNo, pageSize);
    }

    /**
     * 查询未删除路线。
     *
     * @param id 路线主键
     * @return 路线实体
     */
    private CraftRouteEntity requireRoute(Long id) {
        return routeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(CraftErrorCodeConstants.ROUTE_NOT_EXISTS));
    }

    /**
     * 以写锁查询未删除路线，串行化状态变更。
     *
     * @param id 路线主键
     * @return 路线实体
     */
    private CraftRouteEntity requireRouteForUpdate(Long id) {
        return routeRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(CraftErrorCodeConstants.ROUTE_NOT_EXISTS));
    }

    /**
     * 校验路线为草稿状态。
     *
     * @param route 路线实体
     */
    private void requireDraft(CraftRouteEntity route) {
        if (!CraftRouteStatusEnum.DRAFT.getStatus().equals(route.getRoutingStatus())) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_NOT_DRAFT);
        }
    }

    /**
     * 校验路线为生效状态。
     *
     * @param route 路线实体
     */
    private void requireEffective(CraftRouteEntity route) {
        if (!CraftRouteStatusEnum.EFFECTIVE.getStatus().equals(route.getRoutingStatus())) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_NOT_EFFECTIVE);
        }
    }

    /**
     * 校验路线编码与业务版本组合唯一。
     *
     * @param routingCode    路线编码
     * @param routingVersion 业务版本
     * @param excludeId      排除的路线主键，创建时为 null
     */
    private void validateCodeVersion(String routingCode, String routingVersion, Long excludeId) {
        boolean exists = excludeId == null
                ? routeRepository.existsByRoutingCodeAndRoutingVersionAndDeletedFalse(
                        routingCode, routingVersion)
                : routeRepository.existsByRoutingCodeAndRoutingVersionAndIdNotAndDeletedFalse(
                        routingCode, routingVersion, excludeId);
        if (exists) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_CODE_VERSION_DUPLICATE);
        }
    }

    /**
     * 保存路线并转换可识别的并发或唯一约束异常。
     *
     * @param route 路线实体
     */
    private void saveRoute(CraftRouteEntity route) {
        try {
            routeRepository.saveAndFlush(route);
        } catch (DataIntegrityViolationException exception) {
            CraftPersistenceExceptionTranslator.translateUniqueConstraint(exception,
                    ACTIVE_CODE_VERSION_CONSTRAINT,
                    CraftErrorCodeConstants.ROUTE_CODE_VERSION_DUPLICATE);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(CraftErrorCodeConstants.ROUTE_CONCURRENT_MODIFICATION);
        }
    }

    /**
     * 组装路线聚合详情响应。
     *
     * @param route 路线实体
     * @return 路线详情
     */
    private CraftRouteRespVO buildRouteDetail(CraftRouteEntity route) {
        CraftRouteChildren children = childService.load(route.getId());
        Set<Long> productIds = children.products().stream()
                .map(CraftRouteProductEntity::getProductId)
                .collect(Collectors.toSet());
        Set<Long> processIds = children.details().stream()
                .map(CraftRouteDetailEntity::getProcessId)
                .collect(Collectors.toSet());
        Map<Long, ProductEntity> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        Map<Long, CraftProcessEntity> processMap = processRepository.findAllById(processIds).stream()
                .collect(Collectors.toMap(CraftProcessEntity::getId, Function.identity()));
        return CraftRouteConvert.toRespVO(
                route, children.products(), children.details(), productMap, processMap);
    }

    /**
     * 生成路线聚合审计快照。
     *
     * @param route    路线实体
     * @param children 路线子记录
     * @return 审计快照
     */
    private CraftRouteSnapshotDTO toSnapshot(CraftRouteEntity route, CraftRouteChildren children) {
        return CraftRouteConvert.toSnapshotDTO(route, children.products(), children.details());
    }

    /**
     * 规范化路线保存请求。
     *
     * @param reqVO 保存请求
     */
    private void normalizeSaveRequest(CraftRouteSaveReqVO reqVO) {
        reqVO.setRoutingCode(reqVO.getRoutingCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setRoutingName(reqVO.getRoutingName().trim());
        reqVO.setRoutingVersion(reqVO.getRoutingVersion().trim().toUpperCase(Locale.ROOT));
        reqVO.setChangeReason(trimToNull(reqVO.getChangeReason()));
    }

    /**
     * 将请求页码修正到有效范围。
     *
     * @param requestedPageNo 请求页码
     * @param pageSize        每页条数
     * @param total           总记录数
     * @return 实际页码
     */
    private int normalizePageNo(int requestedPageNo, int pageSize, long total) {
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return Math.min(requestedPageNo, totalPages);
    }

    /**
     * 字符串去空格，空白值转 null。
     *
     * @param value 原始字符串
     * @return 规范化字符串
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 请求未提供原因时返回默认原因。
     *
     * @param reason        请求原因
     * @param defaultReason 默认原因
     * @return 最终原因
     */
    private String defaultReason(String reason, String defaultReason) {
        return StringUtils.hasText(reason) ? reason : defaultReason;
    }
}
