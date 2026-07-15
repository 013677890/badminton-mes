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
 * <p>Service 负责编排业务校验、事务、数据库写入。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@Service
public class EquipmentManufacturerServiceImpl implements EquipmentManufacturerService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentManufacturerServiceImpl.class);

    /** TODO(角色C, 2026/07/09): 认证模块建设后，改为从登录上下文获取当前用户 id */
    private static final Long DEFAULT_OPERATOR_ID = 1L;

    private final EquipmentManufacturerRepository manufacturerRepository;

    private final EquipmentLedgerRepository ledgerRepository;

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
     * 校验制造商编码唯一性。
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

    private void evictManufacturerCacheAfterCommit(Long id) {
        equipmentCache.evictDetailAfterCommit(EquipmentRedisKeyConstants.MANUFACTURER_RESOURCE, id);
    }
}
