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
        // 规范化编码后再查重，并在写入前校验主管用户；车间主档是产线和工单的上级组织。
        normalize(reqVO);
        validateStatus(reqVO.getStatus());
        validateCode(reqVO.getWorkshopCode(), null);
        validateManager(reqVO.getManagerId());

        Long operatorId = SecurityContextHolder.getRequiredLoginUserId();
        WorkshopEntity workshop = ProductionOrganizationConvert.toWorkshopEntity(reqVO);
        workshop.setCreateBy(operatorId);
        workshop.setUpdateBy(operatorId);
        // 立即 flush 触发编码唯一约束，统一把数据库竞争转换成业务层可识别的错误。
        save(workshop);
        return workshop.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkshop(Long id, WorkshopUpdateReqVO reqVO) {
        // 车间行锁和版本号共同保护更新，引用检查与状态写入基于同一份最新数据库快照。
        normalize(reqVO);
        validateStatus(reqVO.getStatus());
        WorkshopEntity workshop = requireForUpdate(id);
        validateVersion(workshop, reqVO.getVersion());
        validateCode(reqVO.getWorkshopCode(), id);
        validateManager(reqVO.getManagerId());
        boolean disabling = CommonStatusEnum.DISABLED.getStatus().equals(reqVO.getStatus())
                && CommonStatusEnum.ENABLED.getStatus().equals(workshop.getStatus());
        if (disabling) {
            // 停用前检查启用产线、活动工单和用户归属，避免上级组织停用后仍有现场业务使用。
            validateNoActiveReferences(id);
        }

        ProductionOrganizationConvert.copyWorkshop(reqVO, workshop);
        workshop.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(workshop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkshop(Long id, Integer version) {
        // 车间只能逻辑删除；存在产线、工单、日历或用户引用时保留组织主档供历史追溯。
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
        // 状态切换使用锁版本控制；相同状态重复请求直接幂等返回。
        validateStatus(reqVO.getStatus());
        WorkshopEntity workshop = requireForUpdate(id);
        validateVersion(workshop, reqVO.getVersion());
        if (Objects.equals(workshop.getStatus(), reqVO.getStatus())) {
            return;
        }

        if (CommonStatusEnum.ENABLED.getStatus().equals(reqVO.getStatus())) {
            // 重新启用前确认主管用户仍然有效；主管为空时按业务允许的未指定处理。
            validateManager(workshop.getManagerId());
        } else {
            // 停用只阻断当前有效引用，不改变已产生的工单、日历和历史组织关系。
            validateNoActiveReferences(id);
        }
        workshop.setStatus(reqVO.getStatus());
        workshop.setUpdateBy(SecurityContextHolder.getRequiredLoginUserId());
        save(workshop);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkshopRespVO getWorkshop(Long id) {
        // 详情读取未删除车间，并单独查询主管姓名；找不到用户时仍返回车间基础信息。
        WorkshopEntity workshop = require(id);
        String managerName = loadManagerName(workshop.getManagerId());
        return ProductionOrganizationConvert.toWorkshopRespVO(workshop, managerName);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkshopRespVO getEnabledWorkshop(Long id) {
        // 该入口专门服务于需要可用组织的业务，因此除存在性外还强制检查启用状态。
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
        // 分页先 count 再查询当前页，主管姓名按页批量加载，避免列表渲染产生 N+1 查询。
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

    /** 校验车间没有阻止停用的当前业务引用；各 Repository 只执行 exists 查询。 */
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

    /** 校验车间没有阻止删除的历史业务引用，确保组织主档仍可支撑历史单据回显。 */
    private void validateNoAnyReferences(Long workshopId) {
        boolean referenced = productionLineRepository.existsByWorkshopIdAndDeletedFalse(workshopId)
                || workOrderRepository.existsByWorkshopIdAndDeletedFalse(workshopId)
                || factoryCalendarRepository.existsByWorkshopIdAndDeletedFalse(workshopId)
                || userReferenceQuery.hasAnyWorkshopUser(workshopId);
        if (referenced) {
            throw new ServiceException(ProductionErrorCodeConstants.WORKSHOP_REFERENCE_EXISTS);
        }
    }

    /** 校验车间主管为空或为启用用户，避免保存无效的系统用户关系。 */
    private void validateManager(Long managerId) {
        if (managerId != null && !userReferenceQuery.isEnabledUser(managerId)) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.WORKSHOP_MANAGER_NOT_AVAILABLE);
        }
    }

    /** 校验车间编码唯一；应用层预检用于友好提示，数据库唯一索引负责并发兜底。 */
    private void validateCode(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? workshopRepository.existsByWorkshopCodeAndDeletedFalse(code)
                : workshopRepository.existsByWorkshopCodeAndIdNotAndDeletedFalse(
                        code, excludeId);
        if (exists) {
            throw new ServiceException(ProductionErrorCodeConstants.WORKSHOP_CODE_DUPLICATE);
        }
    }

    /** 校验车间启停状态，只接受启用和停用两个业务值。 */
    private void validateStatus(Integer status) {
        if (!CommonStatusEnum.ENABLED.getStatus().equals(status)
                && !CommonStatusEnum.DISABLED.getStatus().equals(status)) {
            throw new ServiceException(GlobalErrorCodeConstants.PARAM_ERROR, "车间状态不合法");
        }
    }

    /** 查询未删除车间，供只读详情使用。 */
    private WorkshopEntity require(Long id) {
        return workshopRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.WORKSHOP_MASTER_NOT_EXISTS));
    }

    /** 写锁查询未删除车间，供更新、删除和状态操作建立稳定快照。 */
    private WorkshopEntity requireForUpdate(Long id) {
        return workshopRepository.findByIdAndDeletedFalseForUpdate(id)
                .orElseThrow(() -> new ServiceException(
                        ProductionErrorCodeConstants.WORKSHOP_MASTER_NOT_EXISTS));
    }

    /** 校验客户端预期版本，拒绝旧页面覆盖其他人的组织修改。 */
    private void validateVersion(WorkshopEntity workshop, Integer expectedVersion) {
        if (!Objects.equals(workshop.getVersion(), expectedVersion)) {
            throw new ServiceException(
                    ProductionErrorCodeConstants.WORKSHOP_CONCURRENT_MODIFICATION);
        }
    }

    /** 保存车间并转换唯一键和乐观锁异常；flush 让约束冲突在当前事务内尽早暴露。 */
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

    /** 规范化车间请求，统一编码大小写和名称空格后再查重入库。 */
    private void normalize(WorkshopSaveReqVO reqVO) {
        reqVO.setWorkshopCode(reqVO.getWorkshopCode().trim().toUpperCase(Locale.ROOT));
        reqVO.setWorkshopName(reqVO.getWorkshopName().trim());
    }

    /** 将越界页码收敛到最后一页，避免生成无效分页偏移。 */
    private int normalizePageNo(int requested, int pageSize, long total) {
        return Math.min(requested, (int)((total + pageSize - 1) / pageSize));
    }

    /** 查询单个车间主管姓名；主管为空或系统用户不存在时保持返回 null。 */
    private String loadManagerName(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return userReferenceQuery.loadUserNames(Set.of(managerId)).get(managerId);
    }

    /** 批量加载分页车间的主管姓名映射，避免逐条调用用户查询造成 N+1。 */
    private Map<Long, String> loadManagerNameMap(List<WorkshopEntity> workshops) {
        Set<Long> managerIds = workshops.stream()
                .map(WorkshopEntity::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return userReferenceQuery.loadUserNames(managerIds);
    }
}
