package com.badminton.mes.common.core;

import java.util.Collections;
import java.util.List;

import lombok.Data;

/**
 * 分页查询结果。
 *
 * @param <T> 行记录类型
 * @author 张竹灏
 * @date 2026/07/07
 */
@Data
public class PageResult<T> {

    /** 当前页数据，无数据时为空集合而非 null(API-002) */
    private List<T> list;

    /** 满足条件的总记录数 */
    private Long total;

    /** 实际生效的页码(请求页码超过总页数时会被修正为最后一页，见 API-009) */
    private Integer pageNo;

    /** 每页条数 */
    private Integer pageSize;

    /**
     * 构造分页结果。
     *
     * @param list     当前页数据
     * @param total    总记录数
     * @param pageNo   实际生效页码
     * @param pageSize 每页条数
     * @param <T>      行记录类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> list, Long total, Integer pageNo, Integer pageSize) {
        // 统一在这里组装分页字段，避免各查询服务遗漏 pageNo/pageSize 或返回 null 列表。
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        return result;
    }

    /**
     * 构造空分页结果，用于 count 为 0 时直接返回、不再执行分页查询(SQL-005)。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param <T>      行记录类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(Integer pageNo, Integer pageSize) {
        // 使用不可变空集合表达“无记录”，保持 JSON 为 []，同时避免无意义的数据库分页查询。
        return of(Collections.emptyList(), 0L, pageNo, pageSize);
    }
}
