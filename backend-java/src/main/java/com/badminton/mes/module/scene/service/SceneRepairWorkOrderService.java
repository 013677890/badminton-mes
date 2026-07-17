package com.badminton.mes.module.scene.service;

import com.badminton.mes.module.scene.controller.vo.SceneRepairCreateReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneRepairRecordCreateReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneRepairRecheckReqVO;
import com.badminton.mes.module.scene.controller.vo.SceneRepairRespVO;

/**
 * 返修工单服务，由返修工单 Controller 调用，维护返修单状态、处理记录和复检结果。
 *
 * @author 刘涵
 * @date 2026/07/16
 */
public interface SceneRepairWorkOrderService {

    /** 创建返修工单。 */
    SceneRepairRespVO create(SceneRepairCreateReqVO request);

    /** 将返修工单分派给指定处理人。 */
    void assign(Long id, Long assigneeId);

    /** 开始处理已分派的返修工单。 */
    void start(Long id);

    /** 写入一次返修处理过程记录。 */
    void addRecord(Long id, SceneRepairRecordCreateReqVO request);

    /** 提交返修后的复检结果。 */
    void recheck(Long id, SceneRepairRecheckReqVO request);

    /** 关闭已完成且复检通过的返修工单。 */
    void close(Long id);

    /** 查询返修工单详情及其处理记录。 */
    SceneRepairRespVO get(Long id);
}
