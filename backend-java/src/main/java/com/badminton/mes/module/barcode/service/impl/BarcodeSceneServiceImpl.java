package com.badminton.mes.module.barcode.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;
import com.badminton.mes.common.exception.ServiceException;
import com.badminton.mes.module.barcode.constants.BarcodeErrorCodeConstants;
import com.badminton.mes.module.barcode.dal.entity.*;
import com.badminton.mes.module.barcode.dal.repository.*;
import com.badminton.mes.module.barcode.enums.BarcodeStatusEnum;
import com.badminton.mes.module.barcode.service.BarcodeSceneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** M2 扫码校验和使用记录写入实现。 @author 刘涵 */
@Service
public class BarcodeSceneServiceImpl implements BarcodeSceneService {
    private final BarcodeRepository barcodeRepository;private final BarcodeUseRecordRepository useRecordRepository;
    public BarcodeSceneServiceImpl(BarcodeRepository barcodeRepository,BarcodeUseRecordRepository useRecordRepository){this.barcodeRepository=barcodeRepository;this.useRecordRepository=useRecordRepository;}
    @Override @Transactional(rollbackFor=Exception.class)
    public BarcodeSceneSnapshot validateAndRecordUse(String value,Long taskId,Long productId,String batchNo,
            Long processId,Long userId,Long equipmentId,Integer useType){
        // 条码值由数据库唯一索引约束；这里只读取未删除的唯一业务记录作为扫码校验依据。
        BarcodeEntity barcode=barcodeRepository.findByBarcodeValueAndDeletedFalse(value)
                .orElseThrow(()->new ServiceException(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS));
        // 已作废或与当前生产任务、产品、批次任一维度不匹配时，统一按不可用处理，避免泄露条码详情。
        if(BarcodeStatusEnum.CANCELLED.getStatus().equals(barcode.getBarcodeStatus())
                ||barcode.getProductId()!=null&&!Objects.equals(barcode.getProductId(),productId)
                ||barcode.getBatchNo()!=null&&!Objects.equals(barcode.getBatchNo(),batchNo)
                ||barcode.getTaskId()!=null&&!Objects.equals(barcode.getTaskId(),taskId)){
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS);
        }
        if(BarcodeStatusEnum.UNUSED.getStatus().equals(barcode.getBarcodeStatus())){
            // 通过带旧状态条件的更新完成首次使用状态迁移，并发重复扫码不会反复改写状态。
            barcodeRepository.updateStatus(barcode.getId(),BarcodeStatusEnum.UNUSED.getStatus(),BarcodeStatusEnum.USED.getStatus());
        }
        // 每次合法扫码都独立记录使用事实；主条码状态与使用记录在同一事务内提交或回滚。
        BarcodeUseRecordEntity record=new BarcodeUseRecordEntity();record.setBarcodeId(barcode.getId());record.setTaskId(taskId);
        record.setProcessId(processId);record.setUserId(userId);record.setEquipmentId(equipmentId);record.setUseType(useType);
        // 业务时间由服务端统一生成，避免客户端时间不准影响后续追溯排序。
        record.setBusinessTime(LocalDateTime.now());useRecordRepository.save(record);
        // 返回后续工序需要的最小快照，调用方无需再次查询条码主表。
        return new BarcodeSceneSnapshot(barcode.getId(),barcode.getProductId(),barcode.getBatchNo(),barcode.getTaskId());
    }
}
