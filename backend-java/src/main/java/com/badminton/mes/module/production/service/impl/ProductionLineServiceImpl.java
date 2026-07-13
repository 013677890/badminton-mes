package com.badminton.mes.module.production.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.ProductionLinePageReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineRespVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.convert.ProductionOrganizationConvert;
import com.badminton.mes.module.production.dal.entity.ProductionLineEntity;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.repository.DispatchOrderRepository;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.ProductionOrganizationSpecifications;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.DispatchStatusEnum;
import com.badminton.mes.module.production.service.ProductionLineService;
import com.badminton.mes.module.production.service.support.ProductionPersistenceExceptionTranslator;
import com.badminton.mes.module.system.service.OrganizationUserReferenceQuery;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 产线基础资料服务实现。
 *
 * <p>组织写路径统一按“车间 → 产线”加锁，避免用户归属校验、产线启停和
 * 车间启停之间形成检查后状态漂移。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Service
public class ProductionLineServiceImpl implements ProductionLineService {

    private static final String PRODUCTION_LINE_CODE_CONSTRAINT = "uk_active_line_code";

    private final ProductionLineRepository productionLineRepository;

    private final WorkshopRepository workshopRepository;

    private final DispatchOrderRepository dispatchOrderRepository;

    private final OrganizationUserReferenceQuery userReferenceQuery;

    /**
     * 构造产线基础资料服务。
     *
     * @param productionLineRepository 产线 Repository
     * @param workshopRepository 车间 Repository
     * @param dispatchOrderRepository 派工单 Repository
     * @param userReferenceQuery 用户引用查询契约
     */
    public ProductionLineServiceImpl(
            ProductionLineRepository productionLineRepository,
            WorkshopRepository workshopRepository,
            DispatchOrderRepository dispatchOrderRepository,
            OrganizationUserReferenceQuery userReferenceQuery) {
        this.productionLineRepository = productionLineRepository;
        this.workshopRepository = workshopRepository;
        this.dispatchOrderRepository = dispatchOrderRepository;
        this.userReferenceQuery = userReferenceQuery;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProductionLine(ProductionLineSaveReqVO reqVO) {
        normalize(reqVO);
        validateStatus(reqVO.getStatus());
        validateCode(reqVO.getLineCode(), null);
        requireEnabledWorkshopForUpdate(reqVO.getWorkshopId());

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        ProductionLineEntity line =
                ProductionOrganizationConvert.toProductionLineEntity(reqVO);
        line.setCreateBy(operatorId);
        line.setUpdateBy(operatorId);
        save(line);
        return line.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductionLine(Long id, ProductionLineUpdateReqVO reqVO) {
        normalize(reqVO);
        validateStatus(reqVO.getStatus());
        LockedProductionLine locked = requireLineWithWorkshopForUpdate(id);
        ProductionLineEntity line = locked.line();
        validateVersion(line, reqVO.getVersion());
        validateWorkshopImmutable(line, reqVO.getWorkshopId());
        validateCode(reqVO.getLineCode(), id);
        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            requireEnabledWorkshop(locked.workshop());
        }
        boolean disabling = CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus())
                && CommonStatusEnum.ENABLED.getStatus().equals(line.getStatus());
        if (disabling) {
            validateNoActiveReferences(id);
        }

        ProductionOrganizationConvert.copyProductionLine(reqVO, line);
        line.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(line);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductionLine(Long id, Integer version) {
        LockedProductionLine locked = requireLineWithWorkshopForUpdate(id);
        ProductionLineEntity line = locked.line();
        validateVersion(line, version);
        validateNoAnyReferences(id);
        line.setDeleted(true);
        line.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(line);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductionLineStatus(Long id, ProductionStatusReqVO reqVO) {
        validateStatus(reqVO.getStatus());
        LockedProductionLine locked = requireLineWithWorkshopForUpdate(id);
        ProductionLineEntity line = locked.line();
        validateVersion(line, reqVO.getVersion());
        if (Objects.equals(line.getStatus(), reqVO.getStatus())) {
            return;
        }

        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            requireEnabledWorkshop(locked.workshop());
        } else {
            validateNoActiveReferences(id);
        }
        line.setStatus(reqVO.getStatus());
        line.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(line);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionLineRespVO getProductionLine(Long id) {
        ProductionLineEntity line = require(id);
        WorkshopEntity workshop = workshopRepository
                .findByIdAndDeletedFalse(line.getWorkshopId())
                .orElse(null);
        return ProductionOrganizationConvert.toProductionLineRespVO(line, workshop);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductionLineRespVO> getProductionLinePage(
            ProductionLinePageReqVO reqVO) {
        var specification =
                ProductionOrganizationSpecifications.productionLinePage(reqVO);
        long total = productionLineRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<ProductionLineEntity> page = productionLineRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, reqVO.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "lineCode")
                                .and(Sort.by(Sort.Direction.DESC, "id"))));
        Map<Long, WorkshopEntity> workshopMap = loadWorkshopMap(page.getContent());
        List<ProductionLineRespVO> list = page.getContent().stream()
                .map(line -> ProductionOrganizationConvert.toProductionLineRespVO(
                        line, workshopMap.get(line.getWorkshopId())))
                .toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    /** 校验产线没有阻止停用的当前业务引用。 */
    private void validateNoActiveReferences(Long lineId) {
        boolean referenced = dispatchOrderRepository
                .existsByLineIdAndDispatchStatusInAndDeletedFalse(
                        lineId, DispatchStatusEnum.activeStatuses())
                || userReferenceQuery.hasEnabledProductionLineUser(lineId);
        if (referenced) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.PRODUCTION_LINE_ACTIVE_REFERENCE_EXISTS);
        }
    }

    /** 校验产线没有阻止删除的历史业务引用。 */
    private void validateNoAnyReferences(Long lineId) {
        boolean referenced = dispatchOrderRepository.existsByLineIdAndDeletedFalse(lineId)
                || userReferenceQuery.hasAnyProductionLineUser(lineId);
        if (referenced) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.PRODUCTION_LINE_REFERENCE_EXISTS);
        }
    }

    /** 校验产线编码唯一。 */
    private void validateCode(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? productionLineRepository.existsByLineCodeAndDeletedFalse(code)
                : productionLineRepository.existsByLineCodeAndIdNotAndDeletedFalse(
                        code, excludeId);
        if (exists) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.PRODUCTION_LINE_CODE_DUPLICATE);
        }
    }

    /** 校验产线创建后不修改所属车间。 */
    private void validateWorkshopImmutable(
            ProductionLineEntity line, Long requestedWorkshopId) {
        if (!Objects.equals(line.getWorkshopId(), requestedWorkshopId)) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.PRODUCTION_LINE_WORKSHOP_IMMUTABLE);
        }
    }

    /** 校验启停状态。 */
    private void validateStatus(Integer status) {
        if (!CommonStatusEnum.ENABLED.getStatus().equals(status)
                && !CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "产线状态不合法");
        }
    }

    /** 查询未删除产线。 */
    private ProductionLineEntity require(Long id) {
        return productionLineRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.PRODUCTION_LINE_NOT_EXISTS));
    }

    /**
     * 按车间、产线顺序加锁并返回一致快照。
     *
     * @param id 产线主键
     * @return 加锁后的产线与车间
     */
    private LockedProductionLine requireLineWithWorkshopForUpdate(Long id) {
        Long workshopId = productionLineRepository.findWorkshopIdById(id)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.PRODUCTION_LINE_NOT_EXISTS));
        WorkshopEntity workshop = workshopRepository
                .findByIdAndDeletedFalseForUpdate(workshopId)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.PRODUCTION_LINE_WORKSHOP_NOT_AVAILABLE));
        ProductionLineEntity line = productionLineRepository
                .findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.PRODUCTION_LINE_NOT_EXISTS));
        if (!Objects.equals(workshop.getId(), line.getWorkshopId())) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.PRODUCTION_LINE_CONCURRENT_MODIFICATION);
        }
        return new LockedProductionLine(line, workshop);
    }

    /** 写锁查询并校验启用车间。 */
    private WorkshopEntity requireEnabledWorkshopForUpdate(Long workshopId) {
        WorkshopEntity workshop = workshopRepository
                .findByIdAndDeletedFalseForUpdate(workshopId)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.PRODUCTION_LINE_WORKSHOP_NOT_AVAILABLE));
        requireEnabledWorkshop(workshop);
        return workshop;
    }

    /** 校验车间处于启用状态。 */
    private void requireEnabledWorkshop(WorkshopEntity workshop) {
        if (!CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.PRODUCTION_LINE_WORKSHOP_NOT_AVAILABLE);
        }
    }

    /** 校验客户端预期版本。 */
    private void validateVersion(ProductionLineEntity line, Integer expectedVersion) {
        if (!Objects.equals(line.getVersion(), expectedVersion)) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.PRODUCTION_LINE_CONCURRENT_MODIFICATION);
        }
    }

    /** 保存产线并转换唯一键和乐观锁异常。 */
    private void save(ProductionLineEntity line) {
        try {
            productionLineRepository.saveAndFlush(line);
        } catch (DataIntegrityViolationException exception) {
            ProductionPersistenceExceptionTranslator.translateUniqueConstraint(
                    exception, PRODUCTION_LINE_CODE_CONSTRAINT,
                    ProductionErrorCodeConstants.PRODUCTION_LINE_CODE_DUPLICATE);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.PRODUCTION_LINE_CONCURRENT_MODIFICATION);
        }
    }

    /** 批量加载分页产线所属车间，避免 N+1。 */
    private Map<Long, WorkshopEntity> loadWorkshopMap(List<ProductionLineEntity> lines) {
        Set<Long> workshopIds = lines.stream()
                .map(ProductionLineEntity::getWorkshopId)
                .collect(Collectors.toSet());
        return workshopRepository.findByIdInAndDeletedFalse(workshopIds).stream()
                .collect(Collectors.toMap(WorkshopEntity::getId, Function.identity()));
    }

    /** 规范化产线请求。 */
    private void normalize(ProductionLineSaveReqVO reqVO) {
        reqVO.setLineCode(reqVO.getLineCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setLineName(reqVO.getLineName().trim());
    }

    /** 规范化超过总页数的请求页码。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int)((total + pageSize - 1) / pageSize));
    }

    /** 产线与所属车间的一致加锁结果。 */
    private record LockedProductionLine(
            ProductionLineEntity line, WorkshopEntity workshop) {
    }
}
