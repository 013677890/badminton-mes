package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;

/**
 * 设备接入配置 Service，负责配置从创建、联调到启用及删除的完整生命周期。
 *
 * <p>配置是否允许采集并非只取决于启用标志：只有联调结果为通过且配置已启用时，
 * 设备计数入口才会接收数据。</p>
 */
public interface DeviceAccessConfigService {

    /**
     * 创建一条设备接入配置。
     *
     * <p>前置条件：配置编码、设备采集点组合必须唯一，关联设备必须存在、启用且未报废。
     * 新配置会以“未联调、未启用”状态落库，数据来源、计数模式和上报模式由服务端补齐默认值。</p>
     *
     * @param request 待创建的接入参数
     * @return 新配置的主键
     * @throws com.badminton.mes.common.exception.ServiceException 配置重复或关联设备不可用时抛出
     */
    Long createAccessConfig(DeviceAccessConfigSaveReqVO request);

    /**
     * 更新指定设备接入配置。
     *
     * <p>前置条件：配置必须存在且未删除，唯一字段仍满足约束，关联设备可用；
     * 若请求启用配置，则最近联调状态必须为通过。更新在事务内完成，提交后失效该配置的详情缓存。</p>
     *
     * @param id 配置主键
     * @param request 可编辑的配置参数
     * @throws com.badminton.mes.common.exception.ServiceException 配置不存在、字段重复、设备不可用或不满足启用条件时抛出
     */
    void updateAccessConfig(Long id, DeviceAccessConfigSaveReqVO request);

    /**
     * 删除指定设备接入配置。
     *
     * <p>前置条件：配置必须存在，且尚未产生联调记录或计数记录。该操作执行逻辑删除，
     * 同时改写配置编码并强制停用，以释放原唯一编码；事务提交后失效详情缓存。</p>
     *
     * @param id 配置主键
     * @throws com.badminton.mes.common.exception.ServiceException 配置不存在、已有历史数据或删除占位编码冲突时抛出
     */
    void deleteAccessConfig(Long id);

    /**
     * 查询未删除的接入配置详情。
     *
     * <p>返回值是当前配置快照；查询可能读取或回填详情缓存，不产生数据库写入。</p>
     *
     * @param id 配置主键
     * @return 配置详情快照
     * @throws com.badminton.mes.common.exception.ServiceException 配置不存在或已删除时抛出
     */
    DeviceAccessConfigRespVO getAccessConfig(Long id);

    /**
     * 按条件分页查询未删除的接入配置。
     *
     * <p>前置条件：分页与筛选参数已经过接口层校验。若请求页超过末页，将返回末页数据；
     * 无匹配记录时返回保留原分页参数的空结果。本方法无写入副作用。</p>
     *
     * @param request 分页及筛选参数
     * @return 接入配置分页快照
     */
    PageResult<DeviceAccessConfigRespVO> getAccessConfigPage(DeviceAccessConfigPageReqVO request);
}
