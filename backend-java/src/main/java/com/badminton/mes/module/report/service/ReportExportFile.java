package com.badminton.mes.module.report.service;

/**
 * 同步导出的文件内容。
 *
 * @author 刘涵
 * @date 2026/07/13
 */
public record ReportExportFile(String fileName, String contentType, byte[] content) {

    public ReportExportFile {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
