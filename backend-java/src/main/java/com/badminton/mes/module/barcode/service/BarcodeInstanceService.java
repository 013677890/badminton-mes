package com.badminton.mes.module.barcode.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.barcode.controller.vo.BarcodeBatchGenerateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeCancelReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeGenerateRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeImportReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeImportRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeInstancePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeInstanceRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeParseReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeParseRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodePrintReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodePrintRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeUseRecordRespVO;

/**
 * 条码实例 Service，定义批次码生成、解析、作废与查询用例。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
public interface BarcodeInstanceService {

    /**
     * 按应用规则生成单个批次码：Redis 流水取号 + MySQL 唯一索引兜底重试。
     *
     * @param reqVO 生成请求
     * @return 生成结果
     */
    BarcodeGenerateRespVO generateBarcode(BarcodeGenerateReqVO reqVO);

    /**
     * 批量生成批次码，单次上限 500，逐个取号落库并在同一事务内提交。
     *
     * @param reqVO 批量生成请求
     * @return 生成结果列表
     */
    List<BarcodeGenerateRespVO> batchGenerateBarcodes(BarcodeBatchGenerateReqVO reqVO);

    /**
     * 解析条码值，返回条码事实与产品/物料/类型业务上下文。
     *
     * @param reqVO 解析请求
     * @return 解析结果
     */
    BarcodeParseRespVO parseBarcode(BarcodeParseReqVO reqVO);

    /**
     * 作废未使用条码；已使用条码不能作废(已冻结决策)。
     *
     * @param id    条码主键
     * @param reqVO 作废请求，原因仅日志留痕
     */
    void cancelBarcode(Long id, BarcodeCancelReqVO reqVO);

    /**
     * 记录打印动作并返回预览数据：逐次插入打印记录，含打印人、时间、序号、
     * 原因、模板版本与预览快照，不驱动真实打印机(已冻结决策)。
     * 重复打印(序号 > 1)必须填写原因。
     *
     * @param id    条码主键
     * @param reqVO 打印请求
     * @return 打印记录事实与预览数据
     */
    BarcodePrintRespVO printBarcode(Long id, BarcodePrintReqVO reqVO);

    /**
     * 导入外部批次码：单次上限 500，逐条校验重复性与工单范围，部分成功，
     * 响应返回成功数、失败数与逐条失败原因(M1 待确认事项②口径)。
     *
     * @param reqVO 导入请求
     * @return 导入结果
     */
    BarcodeImportRespVO importBarcodes(BarcodeImportReqVO reqVO);

    /**
     * 查询条码的扫码使用记录，按业务发生时间倒序；
     * 写入随 M2 现场执行扫码切片落地，之前返回空列表。
     *
     * @param id 条码主键
     * @return 使用记录列表，无数据时为空集合(API-002)
     */
    List<BarcodeUseRecordRespVO> getBarcodeUseRecords(Long id);

    /**
     * 查询条码详情。
     *
     * @param id 条码主键
     * @return 条码详情
     */
    BarcodeInstanceRespVO getBarcodeInstance(Long id);

    /**
     * 分页查询条码实例。
     *
     * @param reqVO 分页筛选条件
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    PageResult<BarcodeInstanceRespVO> getBarcodeInstancePage(BarcodeInstancePageReqVO reqVO);
}
