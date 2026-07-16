package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionResolveReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportRespVO;

/**
 * 设备计数接入及异常处理 Service。
 *
 * <p>计数入口负责校验接入资格、幂等去重、计算有效增量、记录业务异常，
 * 查询与处置接口则提供计数审计和异常闭环能力。</p>
 */
public interface DeviceCountService {

    /**
     * 接收一条设备计数上报。
     *
     * <p>前置条件：接入配置存在、已启用且联调通过，采集时间不得晚于当前时间，
     * 上报设备编码必须与配置关联设备一致，同一配置、采集时间和流水号不得重复。
     * 成功后会在同一事务中写入计数记录；若识别到设备异常、缺少工序、累计值回退或跳变，
     * 还会同步写入待处理异常，并按时间单调规则更新配置的最近通信时间。</p>
     *
     * @param request 设备原始计数及采集上下文
     * @return 入库记录主键、有效增量、匹配状态、异常类型及处理提示
     * @throws com.badminton.mes.common.exception.ServiceException 配置不可用、设备不匹配、时间非法或重复上报时抛出
     */
    DeviceCountReportRespVO reportCount(DeviceCountReportReqVO request);

    /**
     * 查询计数记录详情。
     *
     * <p>返回值保留上报发生时的设备编码、采集点等历史快照；查询可能读取或回填缓存，
     * 不修改业务数据。</p>
     *
     * @param id 计数记录主键
     * @return 计数记录详情快照
     * @throws com.badminton.mes.common.exception.ServiceException 记录不存在时抛出
     */
    DeviceCountRecordRespVO getCountRecord(Long id);

    /**
     * 按条件分页查询计数记录。
     *
     * <p>前置条件：分页与筛选参数已经过接口层校验。结果按采集时间和主键倒序排列，
     * 超出末页时回退到末页；本方法无写入副作用。</p>
     *
     * @param request 分页及筛选参数
     * @return 计数记录分页快照
     */
    PageResult<DeviceCountRecordRespVO> getCountRecordPage(DeviceCountRecordPageReqVO request);

    /**
     * 查询计数异常详情。
     *
     * <p>返回异常原因及当前处理状态快照；查询可能读取或回填缓存，不修改业务数据。</p>
     *
     * @param id 计数异常主键
     * @return 计数异常详情快照
     * @throws com.badminton.mes.common.exception.ServiceException 异常记录不存在时抛出
     */
    DeviceCountExceptionRespVO getCountException(Long id);

    /**
     * 按条件分页查询计数异常。
     *
     * <p>前置条件：分页与筛选参数已经过接口层校验。结果按创建时间和主键倒序排列，
     * 超出末页时回退到末页；本方法无写入副作用。</p>
     *
     * @param request 分页及筛选参数
     * @return 计数异常分页快照
     */
    PageResult<DeviceCountExceptionRespVO> getCountExceptionPage(DeviceCountExceptionPageReqVO request);

    /**
     * 处置一条待处理计数异常。
     *
     * <p>前置条件：异常记录存在且处理状态仍为待处理。服务会在悲观锁保护的事务中写入
     * 处理状态、结果、处理人和处理时间，提交后失效异常详情缓存，避免重复处置覆盖。</p>
     *
     * @param id 计数异常主键
     * @param request 处理状态及处理结果
     * @throws com.badminton.mes.common.exception.ServiceException 异常不存在或已被处理时抛出
     */
    void processCountException(Long id, DeviceCountExceptionResolveReqVO request);
}
