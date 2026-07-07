package com.cartethyia.easyorange.common.util;

import java.util.Locale;

public final class FileSizeFormat {

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    private FileSizeFormat() {
    }

    public static String formatFileSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        if (size >= GB) {
            return formatWithUnit(size, GB, "GB");
        } else if (size >= MB) {
            return formatWithUnit(size, MB, "MB");
        } else if (size >= KB) {
            return formatWithUnit(size, KB, "KB");
        } else {
            return size + " B";
        }
    }

    private static String formatWithUnit(long size, long unit, String unitName) {
        return String.format(Locale.ROOT, "%.2f %s", size / (double) unit, unitName);
    }
}
