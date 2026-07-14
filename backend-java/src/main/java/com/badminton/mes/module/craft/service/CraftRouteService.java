package com.badminton.mes.module.craft.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteNewVersionReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRoutePageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStatusReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteUpdateReqVO;

/**
 * 工艺路线 Service，承载路线聚合生命周期、版本演进和变更追溯业务。
 *
 * <p>路线以聚合方式维护：主档、适用产品和工序步骤一起保存；
 * 只有草稿允许修改和删除，生效路线通过创建新版本演进。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
public interface CraftRouteService {

    /**
     * 创建草稿工艺路线聚合。
     *
     * @param reqVO 创建请求
     * @return 新路线主键
     */
    Long createRoute(CraftRouteSaveReqVO reqVO);

    /**
     * 按客户端预期版本修改草稿路线聚合。
     *
     * @param id    路线主键
     * @param reqVO 修改请求
     */
    void updateRoute(Long id, CraftRouteUpdateReqVO reqVO);

    /**
     * 按客户端预期版本逻辑删除草稿路线聚合。
     *
     * @param id              路线主键
     * @param expectedVersion 客户端读取时的版本号
     */
    void deleteRoute(Long id, Integer expectedVersion);

    /**
     * 按客户端预期版本审核草稿路线生效，并切换为适用产品的默认路线。
     *
     * @param id    路线主键
     * @param reqVO 审核请求
     */
    void approveRoute(Long id, CraftRouteStatusReqVO reqVO);

    /**
     * 按客户端预期版本停用生效路线，并清除其默认路线标记。
     *
     * @param id    路线主键
     * @param reqVO 停用请求
     */
    void disableRoute(Long id, CraftRouteStatusReqVO reqVO);

    /**
     * 基于生效路线克隆一个新业务版本的草稿路线。
     *
     * @param id    源路线主键
     * @param reqVO 新版本请求
     * @return 新版本路线主键
     */
    Long createRouteVersion(Long id, CraftRouteNewVersionReqVO reqVO);

    /**
     * 查询路线聚合详情，含适用产品和工序步骤。
     *
     * @param id 路线主键
     * @return 路线详情
     */
    CraftRouteRespVO getRoute(Long id);

    /**
     * 分页查询路线主档，不含聚合子项。
     *
     * @param reqVO 分页查询请求
     * @return 路线分页结果
     */
    PageResult<CraftRouteRespVO> getRoutePage(CraftRoutePageReqVO reqVO);

    /**
     * 查询产品默认生效路线聚合详情，供生产模块下发工单使用。
     *
     * @param productId 产品主键
     * @return 默认路线详情
     */
    CraftRouteRespVO getDefaultRoute(Long productId);

    /**
     * 分页查询路线变更日志，路线删除后仍可追溯。
     *
     * @param id    路线主键
     * @param reqVO 分页请求
     * @return 变更日志分页结果
     */
    PageResult<CraftRouteChangeLogRespVO> getRouteChangeLogPage(
            Long id, CraftRouteChangeLogPageReqVO reqVO);
}
