package com.cartethyia.easyorange.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileSizeFormat 测试")
class FileSizeFormatTest {

    @Test
    @DisplayName("小于等于 0 返回 0 B")
    void nonPositive_returnsZeroB() {
        assertThat(FileSizeFormat.formatFileSize(0)).isEqualTo("0 B");
        assertThat(FileSizeFormat.formatFileSize(-5)).isEqualTo("0 B");
    }

    @Test
    @DisplayName("小于 1KB 返回字节")
    void bytes_format() {
        assertThat(FileSizeFormat.formatFileSize(512)).isEqualTo("512 B");
    }

    @Test
    @DisplayName("KB 级别格式化为两位小数")
    void kb_format() {
        assertThat(FileSizeFormat.formatFileSize(2048)).isEqualTo("2.00 KB");
    }

    @Test
    @DisplayName("MB 级别格式化为两位小数")
    void mb_format() {
        assertThat(FileSizeFormat.formatFileSize(5L * 1024 * 1024)).isEqualTo("5.00 MB");
    }

    @Test
    @DisplayName("GB 级别格式化为两位小数")
    void gb_format() {
        assertThat(FileSizeFormat.formatFileSize(2L * 1024 * 1024 * 1024)).isEqualTo("2.00 GB");
    }
}