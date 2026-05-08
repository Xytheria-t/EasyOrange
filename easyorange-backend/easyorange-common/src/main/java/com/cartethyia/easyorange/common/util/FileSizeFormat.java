package com.cartethyia.easyorange.common.util;

public final class FileSizeFormat {

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    private FileSizeFormat() {
        throw new IllegalStateException("Utility class");
    }

    public static String formatFileSize(long size) {
        if (size >= GB) {
            return String.format("%.2f GB", size / (double) GB);
        } else if (size >= MB) {
            return String.format("%.2f MB", size / (double) MB);
        } else if (size >= KB) {
            return String.format("%.2f KB", size / (double) KB);
        } else {
            return size + " B";
        }
    }
}
