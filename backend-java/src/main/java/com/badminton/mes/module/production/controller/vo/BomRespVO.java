package com.badminton.mes.module.production.controller.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/** BOM 聚合响应。 */
@Data
public class BomRespVO {
    /** BOM 主键 */
    private Long id;
    /** BOM 编码 */
    private String bomCode;
    /** 产品主键 */
    private Long productId;
    /** 产品编码 */
    private String productCode;
    /** 产品名称 */
    private String productName;
    /** BOM 业务版本 */
    private String version;
    /** BOM 状态 */
    private Integer bomStatus;
    /** 乐观锁版本 */
    private Integer lockVersion;
    /** BOM 明细；分页响应固定为空集合 */
    private List<BomDetailRespVO> details;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
