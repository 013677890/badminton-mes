package com.badminton.mes.module.production.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.badminton.mes.common.core.GlobalErrorCodeConstants;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.enums.CommonStatusEnum;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.common.security.SecurityContextHolder;
import com.badminton.mes.module.production.constants.ProductionErrorCodeConstants;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopRespVO;
import com.badminton.mes.module.production.controller.vo.WorkshopSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopUpdateReqVO;
import com.badminton.mes.module.production.convert.ProductionOrganizationConvert;
import com.badminton.mes.module.production.dal.entity.WorkshopEntity;
import com.badminton.mes.module.production.dal.repository.FactoryCalendarRepository;
import com.badminton.mes.module.production.dal.repository.ProductionLineRepository;
import com.badminton.mes.module.production.dal.repository.ProductionOrganizationSpecifications;
import com.badminton.mes.module.production.dal.repository.WorkOrderRepository;
import com.badminton.mes.module.production.dal.repository.WorkshopRepository;
import com.badminton.mes.module.production.enums.WorkOrderStatusEnum;
import com.badminton.mes.module.production.service.WorkshopService;
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
 * 车间基础资料服务实现。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Service
public class WorkshopServiceImpl implements WorkshopService {

    private static final String WORKSHOP_CODE_CONSTRAINT = "uk_active_workshop_code";

    private final WorkshopRepository workshopRepository;

    private final ProductionLineRepository productionLineRepository;

    private final WorkOrderRepository workOrderRepository;

    private final FactoryCalendarRepository factoryCalendarRepository;

    private final OrganizationUserReferenceQuery userReferenceQuery;

    /**
     * 构造车间基础资料服务。
     *
     * @param workshopRepository 车间 Repository
     * @param productionLineRepository 产线 Repository
     * @param workOrderRepository 工单 Repository
     * @param factoryCalendarRepository 工厂日历 Repository
     * @param userReferenceQuery 用户引用查询契约
     */
    public WorkshopServiceImpl(
            WorkshopRepository workshopRepository,
            ProductionLineRepository productionLineRepository,
            WorkOrderRepository workOrderRepository,
            FactoryCalendarRepository factoryCalendarRepository,
            OrganizationUserReferenceQuery userReferenceQuery) {
        this.workshopRepository = workshopRepository;
        this.productionLineRepository = productionLineRepository;
        this.workOrderRepository = workOrderRepository;
        this.factoryCalendarRepository = factoryCalendarRepository;
        this.userReferenceQuery = userReferenceQuery;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkshop(WorkshopSaveReqVO reqVO) {
        normalize(reqVO);
        validateStatus(reqVO.getStatus());
        validateCode(reqVO.getWorkshopCode(), null);
        validateManager(reqVO.getManagerId());

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        WorkshopEntity workshop = ProductionOrganizationConvert.toWorkshopEntity(reqVO);
        workshop.setCreateBy(operatorId);
        workshop.setUpdateBy(operatorId);
        save(workshop);
        return workshop.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkshop(Long id, WorkshopUpdateReqVO reqVO) {
        normalize(reqVO);
        validateStatus(reqVO.getStatus());
        WorkshopEntity workshop = requireForUpdate(id);
        validateVersion(workshop, reqVO.getVersion());
        validateCode(reqVO.getWorkshopCode(), id);
        validateManager(reqVO.getManagerId());
        boolean disabling = CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus())
                && CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus());
        if (disabling) {
            validateNoActiveReferences(id);
        }

        ProductionOrganizationConvert.copyWorkshop(reqVO, workshop);
        workshop.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(workshop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkshop(Long id, Integer version) {
        WorkshopEntity workshop = requireForUpdate(id);
        validateVersion(workshop, version);
        validateNoAnyReferences(id);
        workshop.setDeleted(true);
        workshop.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(workshop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkshopStatus(Long id, ProductionStatusReqVO reqVO) {
        validateStatus(reqVO.getStatus());
        WorkshopEntity workshop = requireForUpdate(id);
        validateVersion(workshop, reqVO.getVersion());
        if (Objects.equals(workshop.getStatus(), reqVO.getStatus())) {
            return;
        }

        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            validateManager(workshop.getManagerId());
        } else {
            validateNoActiveReferences(id);
        }
        workshop.setStatus(reqVO.getStatus());
        workshop.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(workshop);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkshopRespVO getWorkshop(Long id) {
        WorkshopEntity workshop = require(id);
        String managerName = loadManagerName(workshop.getManagerId());
        return ProductionOrganizationConvert.toWorkshopRespVO(workshop, managerName);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkshopRespVO getEnabledWorkshop(Long id) {
        WorkshopEntity workshop = require(id);
        if (!CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus())) {
            throw new ServiceException(ProductionErrorCodeConstants.WORKSHOP_NOT_EXISTS);
        }
        String managerName = loadManagerName(workshop.getManagerId());
        return ProductionOrganizationConvert.toWorkshopRespVO(workshop, managerName);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkshopRespVO> getWorkshopPage(WorkshopPageReqVO reqVO) {
        var specification = ProductionOrganizationSpecifications.workshopPage(reqVO);
        long total = workshopRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        int pageNo = normalizePageNo(reqVO.getPageNo(), reqVO.getPageSize(), total);
        Page<WorkshopEntity> page = workshopRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, reqVO.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "workshopCode")
                                .and(Sort.by(Sort.Direction.DESC, "id"))));
        Map<Long, String> managerNameMap = loadManagerNameMap(page.getContent());
        List<WorkshopRespVO> list = page.getContent().stream()
                .map(workshop -> ProductionOrganizationConvert.toWorkshopRespVO(
                        workshop, managerNameMap.get(workshop.getManagerId())))
                .toList();
        return PageResult.of(list, total, pageNo, reqVO.getPageSize());
    }

    /** 校验车间没有阻止停用的当前业务引用。 */
    private void validateNoActiveReferences(Long workshopId) {
        boolean referenced = productionLineRepository
                .existsByWorkshopIdAndStatusAndDeletedFalse(
                        workshopId, CommonStatusEnum.ENABLED.getStatus())
                || workOrderRepository.existsByWorkshopIdAndOrderStatusInAndDeletedFalse(
                        workshopId, WorkOrderStatusEnum.activeStatuses())
                || userReferenceQuery.hasEnabledWorkshopUser(workshopId);
        if (referenced) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.WORKSHOP_ACTIVE_REFERENCE_EXISTS);
        }
    }

    /** 校验车间没有阻止删除的历史业务引用。 */
    private void validateNoAnyReferences(Long workshopId) {
        boolean referenced = productionLineRepository.existsByWorkshopIdAndDeletedFalse(workshopId)
                || workOrderRepository.existsByWorkshopIdAndDeletedFalse(workshopId)
                || factoryCalendarRepository.existsByWorkshopIdAndDeletedFalse(workshopId)
                || userReferenceQuery.hasAnyWorkshopUser(workshopId);
        if (referenced) {
            throw new ServiceException(ProductionErrorCodeConstants.WORKSHOP_REFERENCE_EXISTS);
        }
    }

    /** 校验车间主管为空或为启用用户。 */
    private void validateManager(Long managerId) {
        if (managerId != null && !userReferenceQuery.isEnabledUser(managerId)) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.WORKSHOP_MANAGER_NOT_AVAILABLE);
        }
    }

    /** 校验车间编码唯一。 */
    private void validateCode(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? workshopRepository.existsByWorkshopCodeAndDeletedFalse(code)
                : workshopRepository.existsByWorkshopCodeAndIdNotAndDeletedFalse(
                        code, excludeId);
        if (exists) {
            throw new ServiceException(ProductionErrorCodeConstants.WORKSHOP_CODE_DUPLICATE);
        }
    }

    /** 校验启停状态。 */
    private void validateStatus(Integer status) {
        if (!CommonStatusEnum.ENABLED.getStatus().equals(status)
                && !CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "车间状态不合法");
        }
    }

    /** 查询未删除车间。 */
    private WorkshopEntity require(Long id) {
        return workshopRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.WORKSHOP_MASTER_NOT_EXISTS));
    }

    /** 写锁查询未删除车间。 */
    private WorkshopEntity requireForUpdate(Long id) {
        return workshopRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.WORKSHOP_MASTER_NOT_EXISTS));
    }

    /** 校验客户端预期版本。 */
    private void validateVersion(WorkshopEntity workshop, Integer expectedVersion) {
        if (!Objects.equals(workshop.getVersion(), expectedVersion)) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.WORKSHOP_CONCURRENT_MODIFICATION);
        }
    }

    /** 保存车间并转换唯一键和乐观锁异常。 */
    private void save(WorkshopEntity workshop) {
        try {
            workshopRepository.saveAndFlush(workshop);
        } catch (DataIntegrityViolationException exception) {
            ProductionPersistenceExceptionTranslator.translateUniqueConstraint(
                    exception, WORKSHOP_CODE_CONSTRAINT,
                    ProductionErrorCodeConstants.WORKSHOP_CODE_DUPLICATE);
        } catch (OptimisticLockingFailureException exception) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.WORKSHOP_CONCURRENT_MODIFICATION);
        }
    }

    /** 规范化车间请求。 */
    private void normalize(WorkshopSaveReqVO reqVO) {
        reqVO.setWorkshopCode(reqVO.getWorkshopCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setWorkshopName(reqVO.getWorkshopName().trim());
    }

    /** 规范化超过总页数的请求页码。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int)((total + pageSize - 1) / pageSize));
    }

    /** 查询单个车间主管姓名，主管为空时返回 null。 */
    private String loadManagerName(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return userReferenceQuery.loadUserNames(Set.of(managerId)).get(managerId);
    }

    /** 批量加载分页车间的主管姓名映射，避免 N+1。 */
    private Map<Long, String> loadManagerNameMap(List<WorkshopEntity> workshops) {
        Set<Long> managerIds = workshops.stream()
                .map(WorkshopEntity::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return userReferenceQuery.loadUserNames(managerIds);
    }
}
