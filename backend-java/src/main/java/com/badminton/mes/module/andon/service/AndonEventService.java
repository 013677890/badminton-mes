package com.badminton.mes.module.andon.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonEventActionReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventCreateReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonProcessLogRespVO;

/**
 * 现场安灯异常 Service 契约。
 *
 * <p>由 {@code AndonEventController} 接收操作员的异常上报和状态操作，
 * 由 {@code AndonTimeoutScheduler} 周期性调用超时处理；实现类负责状态机、
 * 处理日志、通知记录以及详情缓存失效，不把这些业务规则放到 Controller。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface AndonEventService {

    /** 创建安灯异常并根据异常类型初始化处理方式、责任人和通知。 */
    Long createEvent(AndonEventCreateReqVO request);

    /** 确认待确认异常，记录实际原因并将状态推进到 {@code CONFIRMED}。 */
    void confirmEvent(Long id, AndonEventActionReqVO request);

    /** 开始处理已确认异常，将状态推进到 {@code PROCESSING}。 */
    void startProcessing(Long id, AndonEventActionReqVO request);

    /** 将异常转派给指定用户或角色，并保留转派操作日志。 */
    void transferEvent(Long id, AndonEventActionReqVO request);

    /** 提交处理结果并将异常推进到等待关闭状态。 */
    void completeEvent(Long id, AndonEventActionReqVO request);

    /** 由管理人员关闭已完成异常，结束本次安灯处理流程。 */
    void closeEvent(Long id, AndonEventActionReqVO request);

    /** 手工升级异常，通常用于响应超时或现场判断需要升级的场景。 */
    void escalateEvent(Long id, AndonEventActionReqVO request);

    /**
     * 扫描并处理响应/升级截止时间已到的异常。
     *
     * @return 本次扫描实际处理的异常数量
     */
    int processTimeoutEvents();

    /** 查询异常详情，返回给详情页及跨模块引用校验使用。 */
    AndonEventRespVO getEvent(Long id);

    /** 按条件分页查询异常，供 Controller 列表页使用。 */
    PageResult<AndonEventRespVO> getEventPage(AndonEventPageReqVO request);

    /** 查询异常处理轨迹，按操作时间倒序返回。 */
    List<AndonProcessLogRespVO> getProcessLogs(Long id);
}
