package com.badminton.mes.common.core;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分页查询基础参数，各模块分页请求 VO 继承本类。
 *
 * <p>pageSize 设置上限做入参保护，防止一次拉取超大数据量拖垮服务(FLOW-012)。
 *
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
public class PageParam {

    /** 页码最小值 */
    public static final int PAGE_NO_MIN = 1;

    /** 每页条数上限，公开接口入参保护 */
    public static final int PAGE_SIZE_MAX = 100;

    /** 页码，从 1 开始 */
    @NotNull(message = "页码不能为空")
    @Min(value = PAGE_NO_MIN, message = "页码最小值为 1")
    private Integer pageNo = 1;

    /** 每页条数，最大 100 */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = PAGE_SIZE_MAX, message = "每页条数最大值为 100")
    private Integer pageSize = 10;
}
