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
        BarcodeEntity barcode=barcodeRepository.findByBarcodeValueAndDeletedFalse(value)
                .orElseThrow(()->new ServiceException(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS));
        if(BarcodeStatusEnum.CANCELLED.getStatus().equals(barcode.getBarcodeStatus())
                ||barcode.getProductId()!=null&&!Objects.equals(barcode.getProductId(),productId)
                ||barcode.getBatchNo()!=null&&!Objects.equals(barcode.getBatchNo(),batchNo)
                ||barcode.getTaskId()!=null&&!Objects.equals(barcode.getTaskId(),taskId)){
            throw new ServiceException(BarcodeErrorCodeConstants.BARCODE_NOT_EXISTS);
        }
        if(BarcodeStatusEnum.UNUSED.getStatus().equals(barcode.getBarcodeStatus())){
            barcodeRepository.updateStatus(barcode.getId(),BarcodeStatusEnum.UNUSED.getStatus(),BarcodeStatusEnum.USED.getStatus());
        }
        BarcodeUseRecordEntity record=new BarcodeUseRecordEntity();record.setBarcodeId(barcode.getId());record.setTaskId(taskId);
        record.setProcessId(processId);record.setUserId(userId);record.setEquipmentId(equipmentId);record.setUseType(useType);
        record.setBusinessTime(LocalDateTime.now());useRecordRepository.save(record);
        return new BarcodeSceneSnapshot(barcode.getId(),barcode.getProductId(),barcode.getBatchNo(),barcode.getTaskId());
    }
}
