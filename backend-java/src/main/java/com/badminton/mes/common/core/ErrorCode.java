package com.badminton.mes.common.core;

/**
 * 错误码对象，对应《Java开发手册(黄山版)》错误码规约。
 *
 * <p>错误码为 5 位字符串：错误来源(A 用户 / B 系统 / C 第三方) + 四位数字编号(ERRCODE-004)；
 * 编号优先复用手册附表 3 的既有错误码，业务细节由 {@code message} 承载，
 * 避免随意新造错误码(ERRCODE-006 / ERRCODE-008)。
 *
 * <p>{@code message} 面向排查人员描述出错原因；{@code userTip} 面向最终用户给出
 * 友好提示，两者不得混用(ERRCODE-007 / API-003)。
 *
 * @param code    错误码，5 位字符串，全部正常时为 00000(ERRCODE-003)
 * @param message 错误描述，供前端与排查人员定位问题，不含敏感信息
 * @param userTip 用户提示，简短友好，引导用户下一步操作
 * @author 张竹灏
 * @date 2026/07/07
 */
public record ErrorCode(String code, String message, String userTip) {
}
