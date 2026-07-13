package com.badminton.mes.module.scene.constants;

import java.util.Map;

/** M2 冻结生产参数编码及默认值。 @author 刘涵 */
public final class SceneParameterCodes {
    public static final String ALLOW_OVER_PRODUCE = "allow_over_produce";
    public static final String MUST_SCAN_REPORT = "must_scan_report";
    public static final String ALLOW_SKIP_PROCESS = "allow_skip_process";
    public static final String ENABLE_FIRST_CHECK = "enable_first_check";
    public static final String ENABLE_ANDON_LINK = "enable_andon_link";
    public static final Map<String, String> DEFAULT_VALUES = Map.of(
            ALLOW_OVER_PRODUCE, "0", MUST_SCAN_REPORT, "1", ALLOW_SKIP_PROCESS, "0",
            ENABLE_FIRST_CHECK, "0", ENABLE_ANDON_LINK, "0");
    private SceneParameterCodes() { }
}
