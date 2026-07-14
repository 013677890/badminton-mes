package com.badminton.mes.module.production.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.production.controller.vo.BomActionReqVO;
import com.badminton.mes.module.production.controller.vo.BomNewVersionReqVO;
import com.badminton.mes.module.production.controller.vo.BomPageReqVO;
import com.badminton.mes.module.production.controller.vo.BomRespVO;
import com.badminton.mes.module.production.controller.vo.BomSaveReqVO;
import com.badminton.mes.module.production.controller.vo.BomUpdateReqVO;

/** BOM 版本与明细服务。 */
public interface BomService {

    /**
     * 创建 BOM 草稿。
     *
     * @param reqVO BOM 创建请求
     * @return 新 BOM 主键
     */
    Long createBom(BomSaveReqVO reqVO);

    /**
     * 修改 BOM 草稿并整体替换明细。
     *
     * @param id BOM 主键
     * @param reqVO BOM 修改请求
     */
    void updateBom(Long id, BomUpdateReqVO reqVO);

    /**
     * 删除未被引用的 BOM 草稿。
     *
     * @param id BOM 主键
     * @param lockVersion 客户端预期锁版本
     */
    void deleteBom(Long id, Integer lockVersion);

    /**
     * 生效 BOM，并停用同产品的旧生效版本。
     *
     * @param id BOM 主键
     * @param reqVO 生效请求
     */
    void activateBom(Long id, BomActionReqVO reqVO);

    /**
     * 停用生效 BOM。
     *
     * @param id BOM 主键
     * @param reqVO 停用请求
     */
    void disableBom(Long id, BomActionReqVO reqVO);

    /**
     * 从历史 BOM 克隆新草稿版本。
     *
     * @param id 来源 BOM 主键
     * @param reqVO 新版本请求
     * @return 新 BOM 主键
     */
    Long createNewVersion(Long id, BomNewVersionReqVO reqVO);

    /**
     * 查询 BOM 聚合详情。
     *
     * @param id BOM 主键
     * @return BOM 聚合详情
     */
    BomRespVO getBom(Long id);

    /**
     * 分页查询 BOM。
     *
     * @param reqVO 分页筛选条件
     * @return BOM 分页结果
     */
    PageResult<BomRespVO> getBomPage(BomPageReqVO reqVO);
}
