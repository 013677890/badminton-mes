package com.badminton.mes.module.report.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 报表导出文件元数据和字节内容防御性复制测试。
 *
 * @author 范家权
 */
class ReportExportFileTest {

    @Test
    void preservesFileMetadata() {
        ReportExportFile file = new ReportExportFile(
                "production.csv", "text/csv;charset=UTF-8", new byte[] {1});

        assertThat(file.fileName()).isEqualTo("production.csv");
        assertThat(file.contentType()).isEqualTo("text/csv;charset=UTF-8");
    }

    @Test
    void constructorCopiesCallerOwnedContent() {
        byte[] source = {1, 2, 3};
        ReportExportFile file = new ReportExportFile("report.xlsx", "application/octet-stream", source);

        source[0] = 99;

        assertThat(file.content()).containsExactly(1, 2, 3);
    }

    @Test
    void accessorReturnsANewCopyOnEveryRead() {
        ReportExportFile file = new ReportExportFile("report.csv", "text/csv", new byte[] {4, 5, 6});
        byte[] firstRead = file.content();

        firstRead[1] = 99;

        assertThat(file.content()).containsExactly(4, 5, 6);
        assertThat(file.content()).isNotSameAs(firstRead);
    }
}
