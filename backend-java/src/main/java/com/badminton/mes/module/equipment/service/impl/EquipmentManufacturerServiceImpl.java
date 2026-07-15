package com.badminton.mes.module.equipment.service.impl;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.equipment.constants.EquipmentErrorCodeConstants;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.convert.EquipmentManufacturerConvert;
import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;
import com.badminton.mes.module.equipment.dal.redis.EquipmentCache;
import com.badminton.mes.module.equipment.dal.redis.EquipmentRedisKeyConstants;
import com.badminton.mes.module.equipment.dal.repository.EquipmentLedgerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerRepository;
import com.badminton.mes.module.equipment.dal.repository.EquipmentManufacturerSpecifications;
import com.badminton.mes.module.equipment.service.EquipmentManufacturerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备制造商 Service 实现。
 *
 * <p>作为制造商主数据的事务边界，负责编排编码唯一性校验、设备引用保护、逻辑删除和详情缓存
 * 一致性。应用层查重用于尽早返回业务错误，数据库唯一索引负责封闭并发窗口；修改和删除仅登记
 * 事务提交后的缓存失效，避免回滚数据与缓存状态不一致。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Service
public class EquipmentManufacturerServiceImpl implements EquipmentManufacturerService {

    /** 记录制造商主数据关键写操作。 */
    private static final Logger logger = LoggerFactory.getLogger(EquipmentManufacturerServiceImpl.class);

    /** TODO(角色C, 2026/07/09): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    /** 制造商持久层，负责有效数据查询及编码唯一约束落库。 */
    private final EquipmentManufacturerRepository manufacturerRepository;

    /** 设备台账持久层，用于删除前检查制造商引用。 */
    private final EquipmentLedgerRepository ledgerRepository;

    /** 制造商详情缓存协调器，写操作提交后负责失效。 */
    private final EquipmentCache equipmentCache;

    /**
     * 构造器注入：依赖不可变、便于单测中直接 new 出被测对象。
     *
     * @param manufacturerRepository 设备制造商 Repository
     * @param ledgerRepository       设备台账 Repository
     * @param equipmentCache         设备缓存组件
     */
    public EquipmentManufacturerServiceImpl(EquipmentManufacturerRepository manufacturerRepository,
                                            EquipmentLedgerRepository ledgerRepository,
                                            EquipmentCache equipmentCache) {
        this.manufacturerRepository = manufacturerRepository;
        this.ledgerRepository = ledgerRepository;
        this.equipmentCache = equipmentCache;
    }

    /**
     * 创建制造商，并用数据库唯一索引兜底并发编码冲突。
     *
     * <p>请求仅提供业务字段，创建人和缺省启用状态由服务统一补齐；立即刷新使唯一约束异常在当前
     * 事务内被翻译为稳定的业务错误，其他完整性异常保持原样上抛。
     *
     * @param reqVO 制造商创建数据
     * @return 新制造商主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEquipmentManufacturer(EquipmentManufacturerSaveReqVO reqVO) {
        validateManufacturerCode(reqVO.getManufacturerCode(), null);

        EquipmentManufacturerEntity manufacturer = EquipmentManufacturerConvert.toEntity(reqVO);
        manufacturer.setCreateBy(DEFAULT_OPERATOR_ID);
        // 设置状态默认值为启用
        if (manufacturer.getStatus() == null) {
            manufacturer.setStatus(1);
        }

        try {
            manufacturerRepository.saveAndFlush(manufacturer);
        } catch (DataIntegrityViolationException e) {
            // 并发创建穿透应用层查重时，由唯一索引 uk_manufacturer_code 兜底
            // 精确匹配唯一索引冲突，避免误判其他约束（如外键）
            if (e.getMessage() != null && e.getMessage().contains("uk_manufacturer_code")) {
                throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MANUFACTURER_CODE_DUPLICATE);
            }
            // 其他数据库约束冲突不应吞噬，向上抛出
            throw e;
        }

        logger.info("[创建设备制造商] id: {}, manufacturerCode: {}", 
                    manufacturer.getId(), manufacturer.getManufacturerCode());
        return manufacturer.getId();
    }

    /**
     * 更新有效制造商，并在事务提交后失效详情缓存。
     *
     * <p>状态未提供时保留原值；编码查重排除当前主键，允许制造商保留自身编码。
     *
     * @param id 制造商主键
     * @param reqVO 制造商更新数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEquipmentManufacturer(Long id, EquipmentManufacturerSaveReqVO reqVO) {
        EquipmentManufacturerEntity existing = validateManufacturerExists(id);
        validateManufacturerCode(reqVO.getManufacturerCode(), id);

        existing.setManufacturerCode(reqVO.getManufacturerCode());
        existing.setManufacturerName(reqVO.getManufacturerName());
        existing.setContactPerson(reqVO.getContactPerson());
        existing.setContactPhone(reqVO.getContactPhone());
        existing.setContactEmail(reqVO.getContactEmail());
        existing.setAddress(reqVO.getAddress());
        existing.setWebsite(reqVO.getWebsite());
        existing.setRemark(reqVO.getRemark());
        // 修改时若状态为 null，保持原值不变
        if (reqVO.getStatus() != null) {
            existing.setStatus(reqVO.getStatus());
        }

        manufacturerRepository.save(existing);
        evictManufacturerCacheAfterCommit(id);
        logger.info("[修改设备制造商] id: {}, manufacturerCode: {}", id, reqVO.getManufacturerCode());
    }

    /**
     * 在确认无设备引用后逻辑删除制造商。
     *
     * <p>删除前执行引用保护，防止台账留下失效制造商；删除时改写编码以释放唯一键，同时保留原记录
     * 供审计追溯，缓存失效延迟到事务成功提交。
     *
     * @param id 制造商主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEquipmentManufacturer(Long id) {
        EquipmentManufacturerEntity manufacturer = validateManufacturerExists(id);

        long equipmentCount = ledgerRepository.countByManufacturerIdAndDeletedFalse(id);
        if (equipmentCount > 0) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MANUFACTURER_HAS_EQUIPMENT);
        }

        // 删除时重命名编码，避免唯一索引冲突（允许撤销删除或数据追溯）
        String originalCode = manufacturer.getManufacturerCode();
        String deletedCode = originalCode + "_DELETED_" + System.currentTimeMillis();
        manufacturer.setManufacturerCode(deletedCode);
        manufacturer.setDeleted(true);
        manufacturerRepository.save(manufacturer);
        evictManufacturerCacheAfterCommit(id);

        logger.info("[删除设备制造商] id: {}", id);
    }

    /**
     * 读取制造商详情，缓存未命中时从数据库加载有效数据快照。
     *
     * @param id 制造商主键
     * @return 制造商详情快照
     */
    @Override
    @Transactional(readOnly = true)
    public EquipmentManufacturerRespVO getEquipmentManufacturer(Long id) {
        return equipmentCache.getOrLoadDetail(EquipmentRedisKeyConstants.MANUFACTURER_RESOURCE,
                id, EquipmentManufacturerRespVO.class, () -> {
            EquipmentManufacturerRespVO response = EquipmentManufacturerConvert.toRespVO(
                    validateManufacturerExists(id));
            return response;
        });
    }

    /**
     * 分页查询制造商；空结果跳过列表查询，越界页码收敛到最后一页。
     *
     * @param reqVO 分页及筛选条件
     * @return 制造商分页快照
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<EquipmentManufacturerRespVO> getEquipmentManufacturerPage(EquipmentManufacturerPageReqVO reqVO) {
        Specification<EquipmentManufacturerEntity> specification = EquipmentManufacturerSpecifications.page(reqVO);

        // 先 count：总数为 0 直接返回空页，省一次列表查询
        long total = manufacturerRepository.count(specification);
        if (total == 0) {
            return PageResult.empty(reqVO.getPageNo(), reqVO.getPageSize());
        }

        // 请求页码超过总页数时按最后一页返回
        int pageSize = reqVO.getPageSize();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        int pageNo = Math.min(reqVO.getPageNo(), totalPages);

        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<EquipmentManufacturerEntity> page = manufacturerRepository.findAll(specification, pageRequest);
        List<EquipmentManufacturerEntity> list = page.getContent();

        return PageResult.of(EquipmentManufacturerConvert.toRespVOList(list), total, pageNo, pageSize);
    }

    /**
     * 校验设备制造商存在且未删除。
     *
     * @param id 制造商主键
     * @return 制造商实体
     */
    private EquipmentManufacturerEntity validateManufacturerExists(Long id) {
        return manufacturerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MANUFACTURER_NOT_EXISTS));
    }

    /**
     * 校验有效制造商编码唯一性，更新场景排除当前记录。
     *
     * <p>该检查提供友好快速失败；并发事务仍可能同时通过，因此最终由数据库唯一索引兜底。
     *
     * @param manufacturerCode 制造商编码
     * @param excludeId        排除的制造商 id，创建时传 null
     */
    private void validateManufacturerCode(String manufacturerCode, Long excludeId) {
        boolean exists = excludeId == null
                ? manufacturerRepository.existsByManufacturerCodeAndDeletedFalse(manufacturerCode)
                : manufacturerRepository.existsByManufacturerCodeAndIdNotAndDeletedFalse(manufacturerCode, excludeId);

        if (exists) {
            throw new ServiceException(EquipmentErrorCodeConstants.EQUIPMENT_MANUFACTURER_CODE_DUPLICATE);
        }
    }

    /**
     * 登记制造商详情缓存在当前事务成功提交后失效。
     *
     * @param id 制造商主键
     */
    private void evictManufacturerCacheAfterCommit(Long id) {
        equipmentCache.evictDetailAfterCommit(EquipmentRedisKeyConstants.MANUFACTURER_RESOURCE, id);
    }
}
