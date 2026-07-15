package com.badminton.mes.module.andon.service;

import java.util.List;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.andon.controller.vo.AndonEventActionReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventCreateReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonEventRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonProcessLogRespVO;

/**
 * 现场安灯事件业务契约。
 *
 * <p>事件主流程遵循“待确认 - 已确认 - 处理中 - 待关闭 - 已关闭”状态机；无须处理模式可在创建时
 * 直接闭环。状态变更操作会校验当前状态、实际原因、指派对象及操作者权限，并同步记录处理日志和
 * 模拟通知。违反状态机、引用关系、指派规则或权限边界时抛出统一业务异常，不允许部分更新。
 *
 * <p>读取详情包含完整处理日志与通知记录，分页列表仅返回事件摘要；调用方如需审计轨迹，应使用
 * {@link #getEvent(Long)} 或 {@link #getProcessLogs(Long)}，不能依赖分页结果中的明细集合。
 */
public interface AndonEventService {

    /**
     * 发起安灯事件，并依据类型的三种处理模式初始化状态、责任人、响应时限和模拟灯控状态。
     *
     * @param request 事件来源及业务关联信息
     * @return 新事件主键
     * @throws com.badminton.mes.common.exception.ServiceException 类型或原因不可用、业务引用冲突、
     *         协助规则无法匹配、指派对象无效或事件编号冲突时抛出
     */
    Long createEvent(AndonEventCreateReqVO request);

    /**
     * 确认待确认事件并归属实际原因，同时结束响应计时。
     *
     * @param id 事件主键
     * @param request 实际原因及操作说明
     * @throws com.badminton.mes.common.exception.ServiceException 事件不存在、状态不是待确认、
     *         原因不属于当前类型或操作者无处理权限时抛出
     */
    void confirmEvent(Long id, AndonEventActionReqVO request);

    /**
     * 将已确认事件推进到处理中状态。
     *
     * @param id 事件主键
     * @param request 操作说明
     * @throws com.badminton.mes.common.exception.ServiceException 事件状态不允许或操作者无处理权限时抛出
     */
    void startProcessing(Long id, AndonEventActionReqVO request);

    /**
     * 在已确认或处理中阶段转派责任用户、责任角色，事件状态本身保持不变。
     *
     * @param id 事件主键
     * @param request 新责任主体及必填转派说明
     * @throws com.badminton.mes.common.exception.ServiceException 状态不允许、说明缺失、责任主体不可用
     *         或操作者无处理权限时抛出
     */
    void transferEvent(Long id, AndonEventActionReqVO request);

    /**
     * 提交处理结果，将处理中事件推进到待关闭状态并停止后续响应、升级计时。
     *
     * @param id 事件主键
     * @param request 实际原因、处理结果及影响数据
     * @throws com.badminton.mes.common.exception.ServiceException 状态不允许、实际原因或处理结果不完整、
     *         原因归属错误或操作者无处理权限时抛出
     */
    void completeEvent(Long id, AndonEventActionReqVO request);

    /**
     * 由管理角色验收并关闭待关闭事件；已开启的模拟设备灯会随之关闭。
     *
     * @param id 事件主键
     * @param request 关闭说明
     * @throws com.badminton.mes.common.exception.ServiceException 事件不是待关闭状态或操作者不具备管理权限时抛出
     */
    void closeEvent(Long id, AndonEventActionReqVO request);

    /**
     * 手工升级已确认或处理中事件，优先采用请求中的责任主体，缺省时回退到适用配置的升级对象。
     *
     * @param id 事件主键
     * @param request 可选升级对象及操作说明
     * @throws com.badminton.mes.common.exception.ServiceException 事件已升级、状态不允许、无有效升级对象
     *         或操作者无处理权限时抛出
     */
    void escalateEvent(Long id, AndonEventActionReqVO request);

    /**
     * 扫描并处理到期事件。
     *
     * <p>实现必须以单事件独立事务执行响应超时或自动升级，隔离单条数据、锁及通知错误，保证同一轮
     * 其他候选事件仍可继续处理。
     *
     * @return 本轮实际发生超时标记或升级的事件数量
     */
    int processTimeoutEvents();

    /**
     * 查询事件详情，同时装配按时间顺序排列的处理日志与通知记录。
     *
     * @param id 事件主键
     * @return 完整事件详情
     * @throws com.badminton.mes.common.exception.ServiceException 事件或关联安灯类型不存在时抛出
     */
    AndonEventRespVO getEvent(Long id);

    /**
     * 按筛选条件分页查询事件摘要，超出末页的页码会收敛到最后一页。
     *
     * @param request 筛选及分页条件
     * @return 不包含日志、通知明细的事件分页结果
     * @throws com.badminton.mes.common.exception.ServiceException 结果引用的安灯类型不存在时抛出
     */
    PageResult<AndonEventRespVO> getEventPage(AndonEventPageReqVO request);

    /**
     * 查询指定事件的完整处理轨迹，按日志主键升序返回。
     *
     * @param id 事件主键
     * @return 处理日志列表
     * @throws com.badminton.mes.common.exception.ServiceException 事件不存在时抛出
     */
    List<AndonProcessLogRespVO> getProcessLogs(Long id);
}
