package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningSaveReqVO;

/**
 * 设备联调记录 Service，负责留存每次联调事实并同步接入配置的当前联调状态。
 *
 * <p>联调记录是不可变的历史凭证；接入配置上的联调状态则表示最近一次联调结论，
 * 并直接参与后续启用和计数采集资格判断。</p>
 */
public interface DeviceCommissioningService {

    /**
     * 新增一次设备联调记录。
     *
     * <p>前置条件：关联接入配置存在且未删除，测试时间不得晚于当前时间。
     * 服务会写入联调记录、记录当前操作人，并同步更新配置联调状态；结果非通过时还会强制停用配置。
     * 全部写入在同一事务中完成，提交后失效接入配置详情缓存。</p>
     *
     * @param request 联调时间、各项测试结果及样例报文
     * @return 新联调记录的主键
     * @throws com.badminton.mes.common.exception.ServiceException 接入配置不存在或测试时间非法时抛出
     */
    Long createCommissioningRecord(DeviceCommissioningSaveReqVO request);

    /**
     * 查询联调记录详情。
     *
     * <p>返回指定联调发生时保存的结果快照；查询可能读取或回填缓存，不修改业务数据。</p>
     *
     * @param id 联调记录主键
     * @return 联调记录详情快照
     * @throws com.badminton.mes.common.exception.ServiceException 联调记录不存在时抛出
     */
    DeviceCommissioningRespVO getCommissioningRecord(Long id);

    /**
     * 按条件分页查询联调记录。
     *
     * <p>前置条件：分页与筛选参数已经过接口层校验。结果按测试时间和主键倒序排列，
     * 超出末页时回退到末页；本方法无写入副作用。</p>
     *
     * @param request 分页及筛选参数
     * @return 联调记录分页快照
     */
    PageResult<DeviceCommissioningRespVO> getCommissioningRecordPage(DeviceCommissioningPageReqVO request);
}
